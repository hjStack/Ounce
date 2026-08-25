/* Ounce 구독 플랜 — 끼니 수에서 파생되는 모든 값의 단일 출처.
 *
 * subscribe.js(가입 전 플랜 선택)와 subscription.js(주간 메뉴 변경)가 같이 쓴다.
 * 두 화면이 "5끼는 냉장" 같은 규칙을 각자 들고 있으면 한쪽만 고쳐서 어긋나므로
 * 끼니 수를 넣으면 나머지가 다 나오는 함수 하나로 묶었다.
 *
 * 가격이 여기 없는 이유: 현재 /subscribe 는 금액을 노출하지 않고 사전 예약만 받는다.
 * 단가가 확정되면 PLANS 각 항목에 price/perMeal 을 채우고 subscribe.html 의
 * #plan-price 블록을 열면 된다. 화면 쪽은 이미 그 형태를 받게 짜여 있다.
 *
 * 보관 방식은 소비기한에서 나온 결과다. 주 1회 묶음 배송이라 마지막 끼니를
 * 수령 후 N일째에 먹게 되는데, 6끼 이상은 냉장 소비기한을 넘겨서 냉동으로 간다.
 * 배송을 주 2회로 쪼개면 택배비가 두 배가 되므로 배송 횟수는 주 1회로 고정이다.
 *
 * 주의: 사업계획에서 확정된 건 주 5끼(냉장)·주 7끼(냉동) 두 개뿐이다.
 * 4·6끼는 같은 규칙을 적용한 잠정값이라 confirmed: false 로 표시해두었다.
 * 화면에서 이 플래그를 어떻게 쓸지는 각 페이지가 결정한다(지금은 안 쓴다).
 */
window.OuncePlan = (function () {
    'use strict';

    /* 최소가 4끼인 이유는 원가다. 포장·배송 고정비는 몇 끼를 보내든 거의 같아서
       끼니 수가 적을수록 한 끼가 지는 몫이 급격히 커진다. 주 3끼는 그 몫이 너무 커져
       위탁 단가가 조금만 올라가도 적자로 뒤집히므로 구독 플랜에서 제외했다.
       3끼 이하가 필요한 사람은 단건 구매(/products)로 보낸다. */
    var MIN_MEALS = 4;
    var MAX_MEALS = 7;

    var CHILLED = {
        key: 'chilled',
        label: '냉장',
        icon: 'ri-temp-cold-line',
        note: '수령 후 5일 내 조리'
    };
    var FROZEN = {
        key: 'frozen',
        label: '냉동',
        icon: 'ri-snowy-line',
        note: '조리 전 재료를 급속 동결'
    };

    /* 끼니 수 → 플랜. 단가가 정해지면 여기에 한 줄씩 채우면 끝난다.
       confirmed 는 사업계획에서 단가까지 확정된 플랜인지 표시한다(5끼·7끼).
       4·6끼는 보관 방식만 같은 규칙으로 채운 잠정값이다. */
    var PLANS = {
        4: { meals: 4, storage: CHILLED, confirmed: false },
        5: { meals: 5, storage: CHILLED, confirmed: true },
        6: { meals: 6, storage: FROZEN, confirmed: false },
        7: { meals: 7, storage: FROZEN, confirmed: true }
    };

    /* 슬라이더 값이 범위를 벗어나도 화면이 깨지지 않게 한 번 조인다. */
    function clamp(meals) {
        var n = parseInt(meals, 10);
        if (isNaN(n)) return 5;
        return Math.min(MAX_MEALS, Math.max(MIN_MEALS, n));
    }

    function of(meals) {
        return PLANS[clamp(meals)];
    }

    /* 냉동 구성이 차별점과 충돌하지 않는다는 설명. 배지 옆 한 줄로 쓴다.
     * 완조리를 얼린 게 아니라 조리 전 상태를 얼린 것이라는 점이 핵심이라
     * 냉동 플랜을 고른 순간 바로 보이게 해야 한다. */
    function storageDetail(meals) {
        var plan = of(meals);
        return plan.storage === FROZEN
            ? '완성된 음식을 얼린 게 아니라, 손질과 계량이 끝난 재료를 그대로 급속 동결했습니다. 냄비에 붓고 끓이는 시점에 조리가 완성되는 건 냉장과 같습니다.'
            : '손질과 계량이 끝난 상태로 냉장 도착합니다. 받은 주 안에 다 먹는 구성이라 소비기한을 신경 쓸 일이 없습니다.';
    }

    return {
        MIN_MEALS: MIN_MEALS,
        MAX_MEALS: MAX_MEALS,
        CHILLED: CHILLED,
        FROZEN: FROZEN,
        clamp: clamp,
        of: of,
        storageDetail: storageDetail
    };
})();
