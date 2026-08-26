# Coupon, Delivery, Point, Q&A API Plan

## Scope

현재 프로젝트의 기존 API 스타일에 맞춰 쿠폰, 배송, 포인트, Q&A API를 설계한다.

- API prefix는 기존과 동일하게 `/api`를 사용한다.
- 인증은 기존 JWT 쿠키 기반 인증을 사용한다.
- `A&A`는 문맥상 고객 문의 기능인 `Q&A`로 정의한다.
- 일반 사용자는 본인 리소스만 조회/사용할 수 있다.
- 관리자 API는 `ROLE_ADMIN` 권한 분리가 완료된 뒤 활성화한다.

## Common Rules

### Authentication

| API | 인증 |
| --- | --- |
| 사용자 쿠폰/배송/포인트/Q&A | Required |
| 관리자 발급/상태 변경/답변 | Required + Admin |

### Response Policy

신규 API는 문자열 응답 대신 DTO 응답을 우선한다.

```json
{
  "code": "COUPON_NOT_AVAILABLE",
  "message": "사용할 수 없는 쿠폰입니다."
}
```

### Pagination

이력성 데이터는 기본 페이지네이션을 적용한다.

| Query | Default | Description |
| --- | --- | --- |
| `page` | `0` | 0-based page |
| `size` | `20` | page size |

## Coupon API

### Business Rules

- 쿠폰은 회원에게 귀속된다.
- 쿠폰 상태는 `AVAILABLE`, `USED`, `EXPIRED`를 사용한다.
- 하나의 주문에는 쿠폰을 최대 1개만 사용할 수 있다.
- 쿠폰 적용은 주문 생성 트랜잭션 안에서 처리한다.
- 쿠폰이 주문 금액보다 큰 경우 최종 결제 금액은 0원 미만이 될 수 없다.

### Required Entity Extension

현재 `Coupon` 엔티티는 상태와 회원/주문 연결만 있으므로 실제 할인 정책을 위해 아래 필드가 필요하다.

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `name` | `String` | Y | 쿠폰명 |
| `discountType` | `FIXED`, `PERCENT` | Y | 정액/정률 |
| `discountAmount` | `int` | Y | 할인 금액 또는 할인율 |
| `maxDiscountAmount` | `Integer` | N | 정률 쿠폰 최대 할인액 |
| `minOrderAmount` | `int` | Y | 최소 주문 금액 |
| `issuedAt` | `LocalDateTime` | Y | 발급 시각 |
| `expiresAt` | `LocalDateTime` | Y | 만료 시각 |
| `usedAt` | `LocalDateTime` | N | 사용 시각 |

### Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/coupons/me` | 내 쿠폰 목록 조회 |
| `GET` | `/api/coupons/me/available` | 특정 주문 금액 기준 사용 가능 쿠폰 조회 |
| `POST` | `/api/coupons/{couponId}/validate` | 쿠폰 사용 가능 여부 검증 |
| `POST` | `/api/admin/coupons/issue` | 관리자 쿠폰 발급 |
| `PATCH` | `/api/admin/coupons/{couponId}/expire` | 관리자 쿠폰 만료 처리 |

### `GET /api/coupons/me`

Response

```json
[
  {
    "couponId": 1,
    "name": "첫 주문 3000원 할인",
    "status": "AVAILABLE",
    "discountType": "FIXED",
    "discountAmount": 3000,
    "minOrderAmount": 20000,
    "expiresAt": "2026-09-30T23:59:59"
  }
]
```

### `GET /api/coupons/me/available?orderAmount=36000`

Response

```json
[
  {
    "couponId": 1,
    "name": "첫 주문 3000원 할인",
    "expectedDiscountAmount": 3000,
    "finalAmount": 33000
  }
]
```

### `POST /api/coupons/{couponId}/validate`

Request

```json
{
  "orderAmount": 36000
}
```

Response

```json
{
  "couponId": 1,
  "available": true,
  "discountAmount": 3000,
  "finalAmount": 33000
}
```

### Order Integration

쿠폰은 별도 적용 API에서 확정하지 않고 주문 생성 시점에 최종 사용 처리한다.

`OrderCreateRequest`에 `couponId`를 추가한다.

```json
{
  "selectedCartProductIds": [1, 2, 3],
  "deliveryType": "DAWN",
  "couponId": 1
}
```

주문 생성 처리 순서:

1. 회원 조회
2. 장바구니 상품 조회
3. 주문 금액 계산
4. 쿠폰 소유권/상태/만료/최소 주문 금액 검증
5. 할인 금액 계산
6. 최종 결제 금액 기준 포인트 차감
7. 쿠폰 `USED` 처리 및 주문 연결
8. 주문/주문상품/배송/포인트 이력 저장
9. 장바구니 상품 삭제

## Delivery API

### Business Rules

- 배송은 주문 결제 완료 후 생성된다.
- 기본 상태는 `PREPARING`이다.
- 일반 사용자는 본인 주문의 배송만 조회할 수 있다.
- 배송 상태 변경은 관리자 또는 운영자만 가능하다.
- 주문 취소는 배송 상태가 `SHIPPED` 이상이면 불가하다.

### Required Entity Extension

현재 `Delivery` 엔티티에는 운송장과 수령자 상세 정보가 부족하다.

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `receiverPhone` | `String` | Y | 수령자 연락처 |
| `zipCode` | `String` | Y | 우편번호 |
| `addressDetail` | `String` | N | 상세 주소 |
| `trackingNumber` | `String` | N | 운송장 번호 |
| `carrier` | `String` | N | 택배사 |
| `shippedAt` | `LocalDateTime` | N | 출고 시각 |
| `deliveredAt` | `LocalDateTime` | N | 배송 완료 시각 |

### Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/deliveries/me` | 내 배송 목록 조회 |
| `GET` | `/api/deliveries/{deliveryId}` | 배송 상세 조회 |
| `GET` | `/api/orders/{orderId}/delivery` | 주문 기준 배송 조회 |
| `PATCH` | `/api/admin/deliveries/{deliveryId}/status` | 관리자 배송 상태 변경 |
| `PATCH` | `/api/admin/deliveries/{deliveryId}/tracking` | 관리자 운송장 등록 |

### `GET /api/orders/{orderId}/delivery`

Response

```json
{
  "deliveryId": 10,
  "orderId": 100,
  "receiverName": "홍길동",
  "address": "서울시 강남구 ...",
  "deliveryType": "DAWN",
  "status": "PREPARING",
  "statusDescription": "배송 준비중",
  "carrier": null,
  "trackingNumber": null
}
```

### `PATCH /api/admin/deliveries/{deliveryId}/status`

Request

```json
{
  "status": "SHIPPED"
}
```

Response

```json
{
  "deliveryId": 10,
  "status": "SHIPPED",
  "statusDescription": "배송 중"
}
```

## Point API

### Business Rules

- `Member.point`를 현재 잔액으로 사용한다.
- `PointHistory`는 포인트 원장으로 사용한다.
- 포인트 증가 이력은 양수, 차감 이력은 음수로 저장한다.
- 주문 결제 시 `USE` 이력을 남긴다.
- 주문 취소/환불 시 `REFUND` 이력을 남긴다.
- 포인트 잔액 변경과 이력 저장은 같은 트랜잭션에서 처리한다.

### Required Entity Extension

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `order` | `Order` | N | 주문 결제/환불 연결 |
| `description` | `String` | N | 이력 설명 |
| `balanceAfter` | `int` | Y | 이력 발생 후 잔액 |
| `expiresAt` | `LocalDateTime` | N | 적립 포인트 만료일 |

### Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/points/me` | 내 포인트 잔액 조회 |
| `GET` | `/api/points/me/histories` | 내 포인트 이력 조회 |
| `POST` | `/api/admin/points/grant` | 관리자 포인트 지급 |
| `POST` | `/api/admin/points/deduct` | 관리자 포인트 차감 |

### `GET /api/points/me`

Response

```json
{
  "memberId": 1,
  "point": 12000
}
```

### `GET /api/points/me/histories?page=0&size=20`

Response

```json
{
  "content": [
    {
      "pointHistoryId": 1,
      "amount": -33000,
      "type": "USE",
      "typeDescription": "상품 결제 사용",
      "description": "주문 결제",
      "balanceAfter": 12000,
      "createdAt": "2026-08-26T10:15:30"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

### `POST /api/admin/points/grant`

Request

```json
{
  "memberId": 1,
  "amount": 1000,
  "type": "REWARD",
  "description": "회원가입 축하 포인트"
}
```

Response

```json
{
  "memberId": 1,
  "amount": 1000,
  "balanceAfter": 13000
}
```

## Q&A API

### Business Rules

- Q&A는 1:1 문의로 정의한다.
- 일반 사용자는 본인이 작성한 문의만 조회/수정/삭제할 수 있다.
- 답변 완료 상태에서는 사용자가 내용을 수정할 수 없다.
- 답변은 관리자만 작성할 수 있다.

### Required Entity Extension

현재 `QnA` 엔티티에는 답변 내용과 답변 시간이 없다.

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `answer` | `String` | N | 관리자 답변 |
| `answeredAt` | `LocalDateTime` | N | 답변 시각 |
| `category` | `String` or enum | N | 문의 유형 |

### Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/qna` | 문의 등록 |
| `GET` | `/api/qna/me` | 내 문의 목록 조회 |
| `GET` | `/api/qna/{qnaId}` | 문의 상세 조회 |
| `PATCH` | `/api/qna/{qnaId}` | 답변 전 문의 수정 |
| `DELETE` | `/api/qna/{qnaId}` | 답변 전 문의 삭제 |
| `GET` | `/api/admin/qna` | 관리자 문의 목록 조회 |
| `POST` | `/api/admin/qna/{qnaId}/answer` | 관리자 답변 등록 |

### `POST /api/qna`

Request

```json
{
  "title": "배송지를 변경하고 싶어요",
  "content": "이번 주 주문 배송지를 회사로 변경할 수 있나요?",
  "category": "DELIVERY"
}
```

Response

```json
{
  "qnaId": 1,
  "status": "WAITING"
}
```

### `GET /api/qna/{qnaId}`

Response

```json
{
  "qnaId": 1,
  "title": "배송지를 변경하고 싶어요",
  "content": "이번 주 주문 배송지를 회사로 변경할 수 있나요?",
  "category": "DELIVERY",
  "status": "ANSWERED",
  "answer": "배송 준비 전 주문은 마이페이지에서 배송지를 변경할 수 있습니다.",
  "createdAt": "2026-08-26T10:00:00",
  "answeredAt": "2026-08-26T11:00:00"
}
```

### `POST /api/admin/qna/{qnaId}/answer`

Request

```json
{
  "answer": "배송 준비 전 주문은 마이페이지에서 배송지를 변경할 수 있습니다."
}
```

Response

```json
{
  "qnaId": 1,
  "status": "ANSWERED",
  "answeredAt": "2026-08-26T11:00:00"
}
```

## Error Codes

| Code | HTTP | Message |
| --- | --- | --- |
| `COUPON_NOT_FOUND` | `404` | 쿠폰을 찾을 수 없습니다. |
| `COUPON_OWNER_MISMATCH` | `403` | 본인 쿠폰만 사용할 수 있습니다. |
| `COUPON_NOT_AVAILABLE` | `400` | 사용할 수 없는 쿠폰입니다. |
| `COUPON_EXPIRED` | `400` | 만료된 쿠폰입니다. |
| `COUPON_MIN_ORDER_AMOUNT` | `400` | 최소 주문 금액을 충족하지 않았습니다. |
| `DELIVERY_NOT_FOUND` | `404` | 배송 정보를 찾을 수 없습니다. |
| `DELIVERY_OWNER_MISMATCH` | `403` | 본인 배송 정보만 조회할 수 있습니다. |
| `DELIVERY_STATUS_INVALID` | `400` | 변경할 수 없는 배송 상태입니다. |
| `POINT_NOT_ENOUGH` | `400` | 포인트가 부족합니다. |
| `POINT_AMOUNT_INVALID` | `400` | 포인트 금액이 올바르지 않습니다. |
| `QNA_NOT_FOUND` | `404` | 문의를 찾을 수 없습니다. |
| `QNA_OWNER_MISMATCH` | `403` | 본인 문의만 조회할 수 있습니다. |
| `QNA_ALREADY_ANSWERED` | `400` | 답변 완료된 문의는 수정할 수 없습니다. |

## Implementation Priority

1. 포인트 조회/이력 API
2. 주문 생성 시 포인트 이력 저장
3. 배송 조회 API
4. 주문 생성 시 배송 생성
5. Q&A 사용자 등록/조회 API
6. 관리자 Q&A 답변 API
7. 쿠폰 엔티티 확장
8. 주문 생성 시 쿠폰 적용
9. 관리자 쿠폰 발급 API
10. 관리자 배송/포인트 운영 API

## Repository Plan

| Domain | Repository Method |
| --- | --- |
| Coupon | `findAllByMemberMemberIdOrderByCouponIdDesc(Long memberId)` |
| Coupon | `findByCouponIdAndMemberMemberId(Long couponId, Long memberId)` |
| Delivery | `findAllByOrderMemberMemberIdOrderByDeliveryIdDesc(Long memberId)` |
| Delivery | `findByOrderOrderIdAndOrderMemberMemberId(Long orderId, Long memberId)` |
| PointHistory | `findAllByMemberMemberIdOrderByPointHistoryIdDesc(Long memberId, Pageable pageable)` |
| QnA | `findAllByMemberMemberIdOrderByQnaIdDesc(Long memberId, Pageable pageable)` |
| QnA | `findByQnaIdAndMemberMemberId(Long qnaId, Long memberId)` |

## Controller Plan

| Controller | Base Path |
| --- | --- |
| `CouponController` | `/api/coupons` |
| `DeliveryController` | `/api/deliveries` |
| `PointController` | `/api/points` |
| `QnaController` | `/api/qna` |
| `CouponAdminController` | `/api/admin/coupons` |
| `DeliveryAdminController` | `/api/admin/deliveries` |
| `PointAdminController` | `/api/admin/points` |
| `QnaAdminController` | `/api/admin/qna` |

## SecurityConfig Update

일반 API는 현재 `anyRequest().authenticated()`에 걸리므로 별도 허용 설정이 필요 없다.

관리자 API는 권한 분리 후 아래 정책을 추가한다.

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

## Open Decisions

- 쿠폰을 정액만 지원할지, 정률까지 지원할지 결정해야 한다.
- Q&A를 1:1 문의로만 둘지, 상품 상세 문의까지 확장할지 결정해야 한다.
- 배송지 정보를 주문 생성 요청에서 받을지, 회원 기본 주소에서 가져올지 결정해야 한다.
- 포인트 충전 기능을 실제 결제 연동 전에도 제공할지 결정해야 한다.
