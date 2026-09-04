// import http from 'k6/http';
// import { check } from 'k6';
//
// const BASE = 'https://ouncefresh.com';
//
// // ===== 여기 3개를 네 서비스에 맞게 채워야 함 =====
// const LOGIN_BODY = JSON.stringify({ email: 'test@test.com', password: 'password' }); // TODO: 실제 로그인 필드/계정
// const ORDER_BODY = JSON.stringify({ /* TODO: OrderCreateRequest 필드 */ });
// // 재고 키: 테스트 전 EC2에서 redis-cli SET <키> 100 으로 세팅
// // ================================================
//
// export const options = {
//   scenarios: {
//     flash_sale: {
//       executor: 'per-vu-iterations',
//       vus: 1000,
//       iterations: 1,
//     },
//   },
// };
//
// // 로그인 1회 → Authorization 쿠키 확보 (전체 VU가 공유)
// export function setup() {
//   const res = http.post(`${BASE}/api/members/login`, LOGIN_BODY, {
//     headers: { 'Content-Type': 'application/json' },
//   });
//   const cookie = res.cookies['__Host-oz-a'];
//   if (!cookie) {
//     throw new Error('로그인 실패: Authorization 쿠키 없음. status=' + res.status);
//   }
//   return { token: cookie[0].value };
// }
//
// export default function (data) {
//   const res = http.post(`${BASE}/api/orders`, ORDER_BODY, {
//     headers: {
//       'Content-Type': 'application/json',
//       'Cookie': `Authorization=${data.token}`,
//     },
//   });
//   check(res, {
//     'created (201)': (r) => r.status === 201,
//     'sold out (409/400)': (r) => r.status === 409 || r.status === 400,
//   });
// }
