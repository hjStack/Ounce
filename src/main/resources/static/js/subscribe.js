/* /subscribe — 가입 전 구독 소개 페이지.
 *
 * 하는 일 세 가지뿐이다:
 *   1. 끼니 수 슬라이더 → 보관 방식 배지와 주간 구성 미리보기를 갱신
 *   2. 미리보기 슬롯을 실제 상품으로 채우고 셔플 (매주 바꿀 수 있다는 걸 보여주는 용도)
 *   3. 사전 예약 이메일 접수
 *
 * 끼니 수에서 파생되는 값은 전부 OuncePlan(subscription-plan.js)에서 가져온다.
 * 상품 조회·이스케이프·토스트는 Ounce(product-card.js)를 그대로 쓴다.
 *
 * 서버에 구독 API 가 아직 없다. 사전 예약 접수만 엔드포인트가 필요하고,
 * 그 자리는 submitReservation() 안에 형태를 맞춰 남겨두었다.
 */
(function () {
    'use strict';

    var plan = window.OuncePlan;
    var ui = window.Ounce;

    /* 미리보기에 쓸 상품 풀. fetchCatalog 가 캐시하므로 페이지당 1회만 나간다. */
    var catalog = [];

    /* 현재 미리보기에 올라간 상품들. 셔플할 때 "직전과 완전히 같은 구성"이
     * 나오는 걸 피하려고 들고 있는다. */
    var slots = [];

    var els = {};

    function $(id) { return document.getElementById(id); }

    /* ── 슬롯 카드 ─────────────────────────────────────────────
     * product-card.js 의 cardHTML 을 쓰지 않는 이유: 그 카드는 가격과
     * 장바구니 버튼이 붙어 있다. 이 페이지는 가격을 노출하지 않고
     * 개별 구매를 유도하지도 않으므로 요일 + 요리 이름만 남긴 축약형을 쓴다. */
    var DAY_LABELS = ['월', '화', '수', '목', '금', '토', '일'];

    function slotHTML(product, index) {
        var parts = ui.splitName(product.name);
        var escape = function (v) {
            return String(v == null ? '' : v)
                .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
        };

        return '' +
        '<article class="overflow-hidden rounded-xl border border-background-200 bg-white">' +
            '<div class="relative aspect-square overflow-hidden bg-background-100">' +
                '<img src="' + escape(product.imageUrl || ui.PLACEHOLDER) + '"' +
                    ' alt="' + escape(parts.title) + '" loading="lazy"' +
                    ' class="h-full w-full object-cover"' +
                    ' onerror="this.onerror=null;this.src=Ounce.PLACEHOLDER"/>' +
                '<span class="absolute left-2 top-2 rounded-md bg-white/95 px-2 py-1 text-[11px]' +
                    ' font-bold text-foreground-700 shadow-sm backdrop-blur-sm">' +
                    DAY_LABELS[index % 7] + '</span>' +
            '</div>' +
            '<div class="p-3">' +
                '<h3 class="clamp-2 text-[13px] font-semibold leading-snug text-foreground-950">' +
                    escape(parts.title) + '</h3>' +
            '</div>' +
        '</article>';
    }

    /* ── 미리보기 ──────────────────────────────────────────────
     * 카탈로그에서 count 개를 겹치지 않게 뽑는다. 상품 수가 count 보다 적으면
     * 있는 만큼만 그린다(중복으로 칸을 메우면 "매주 다른 구성"과 정반대 인상). */
    function pickSlots(count) {
        var pool = catalog.slice();
        var picked = [];
        while (picked.length < count && pool.length > 0) {
            var i = Math.floor(Math.random() * pool.length);
            picked.push(pool.splice(i, 1)[0]);
        }
        return picked;
    }

    function renderPreview(count) {
        if (!els.previewGrid) return;

        if (catalog.length === 0) {
            ui.renderSkeletons(els.previewGrid, count, 'category');
            return;
        }

        slots = pickSlots(count);
        els.previewGrid.textContent = slots.map(slotHTML).join('');
        els.previewGrid.removeAttribute('aria-busy');

        if (els.previewStatus) {
            els.previewStatus.classList.add('hidden');
        }
    }

    /* ── 끼니 수 반영 ──────────────────────────────────────────
     * 슬라이더를 끌 때마다 불린다. 보관 방식이 냉장↔냉동으로 뒤집히는 게
     * 이 화면에서 가장 중요한 정보라, 배지·설명·미리보기를 한 번에 맞춘다. */
    function applyMeals(meals) {
        var selected = plan.of(meals);
        var storage = selected.storage;

        els.mealCount.textContent = selected.meals;
        els.previewCount.textContent = selected.meals;

        els.storageIcon.className = storage.icon + ' text-primary-600 text-lg';
        els.storageLabel.textContent = storage.label;
        els.storageNote.textContent = storage.note;
        els.storageDetail.textContent = plan.storageDetail(selected.meals);

        /* 눈금 중 선택된 것만 진하게. 슬라이더 thumb 만으로는 지금 값이
         * 어느 눈금인지 모바일에서 잘 안 읽힌다. */
        els.ticks.forEach(function (tick) {
            var isOn = Number(tick.dataset.meals) === selected.meals;
            tick.classList.toggle('text-foreground-400', !isOn);
            tick.classList.toggle('text-primary-600', isOn);
            tick.classList.toggle('font-bold', isOn);
        });

        renderPriceIfKnown(selected);
        renderPreview(selected.meals);
    }

    /* 가격은 지금 노출하지 않는다. OuncePlan 에 price 가 채워지는 날
     * (= 단가 확정 후) 이 함수가 자동으로 블록을 열어준다. */
    function renderPriceIfKnown(selected) {
        if (!els.price) return;
        if (selected.price == null) {
            els.price.classList.add('hidden');
            return;
        }
        els.priceTotal.textContent = ui.won(selected.price);
        els.pricePerMeal.textContent = ui.won(selected.perMeal != null
            ? selected.perMeal
            : Math.round(selected.price / selected.meals));
        els.price.classList.remove('hidden');
    }

    /* ── 사전 예약 ─────────────────────────────────────────────
     * 서버 엔드포인트가 아직 없다. 지금은 접수 시도 형태만 만들어두고
     * 실패해도 사용자에게는 접수된 것처럼 보이지 않게 처리한다
     * (잘못된 성공 메시지가 사전 예약 수치를 오염시키는 게 더 나쁘다). */
    function submitReservation(email) {
        // TODO: 구독 사전 예약 API 연결 — POST /api/subscriptions/reservations { email }
        return fetch('/api/subscriptions/reservations', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ email: email })
        }).then(function (res) {
            if (!res.ok) throw new Error('사전 예약 접수 실패: ' + res.status);
            return true;
        });
    }

    function showReserveMessage(text, isError) {
        if (!els.reserveMsg) return;
        els.reserveMsg.textContent = text;
        els.reserveMsg.classList.remove('hidden');
        els.reserveMsg.classList.toggle('text-white/80', !isError);
        els.reserveMsg.classList.toggle('text-red-200', !!isError);
    }

    function mountReserveForm() {
        var form = $('reserve-form');
        if (!form) return;

        var input = $('reserve-email');
        var button = form.querySelector('button[type="submit"]');

        form.addEventListener('submit', function (e) {
            e.preventDefault();

            var email = (input.value || '').trim();
            /* novalidate 로 브라우저 기본 툴팁을 끈 대신 여기서 확인한다.
               서버 검증을 대체하는 게 아니라 오타를 바로 알려주는 정도. */
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                showReserveMessage('이메일 주소를 다시 확인해 주세요.', true);
                input.focus();
                return;
            }

            button.disabled = true;
            showReserveMessage('접수 중…', false);

            submitReservation(email)
                .then(function () {
                    form.classList.add('hidden');
                    showReserveMessage('사전 예약이 접수됐습니다. 오픈하면 ' + email + ' 로 알려드릴게요.', false);
                })
                .catch(function () {
                    showReserveMessage(
                        '지금은 접수가 되지 않습니다. 잠시 후 다시 시도해 주세요.', true);
                    button.disabled = false;
                });
        });
    }

    /* ── 부팅 ──────────────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', function () {
        var range = $('meal-range');
        if (!range) return;   // 다른 페이지에서 잘못 로드된 경우

        els = {
            range: range,
            mealCount: $('meal-count'),
            previewCount: $('preview-count'),
            storageIcon: $('storage-icon'),
            storageLabel: $('storage-label'),
            storageNote: $('storage-note'),
            storageDetail: $('storage-detail'),
            price: $('plan-price'),
            priceTotal: $('plan-price-total'),
            pricePerMeal: $('plan-price-per-meal'),
            previewGrid: $('preview-grid'),
            previewStatus: $('preview-status'),
            reserveMsg: $('reserve-msg'),
            ticks: Array.prototype.slice.call(document.querySelectorAll('.meal-tick'))
        };

        range.min = plan.MIN_MEALS;
        range.max = plan.MAX_MEALS;

        /* 상품이 도착하기 전에도 배지·눈금은 바로 맞춰둔다. 그리드는 스켈레톤. */
        applyMeals(range.value);

        range.addEventListener('input', function () { applyMeals(range.value); });

        var shuffle = $('shuffle-btn');
        if (shuffle) {
            shuffle.addEventListener('click', function () { renderPreview(plan.clamp(range.value)); });
        }

        ui.fetchCatalog()
            .then(function (products) {
                catalog = products || [];
                if (catalog.length === 0) {
                    els.previewGrid.innerHTML = '';
                    els.previewStatus.textContent = '준비된 밀키트를 불러오지 못했습니다.';
                    els.previewStatus.classList.remove('hidden');
                    return;
                }
                renderPreview(plan.clamp(range.value));
            })
            .catch(function () {
                els.previewGrid.innerHTML = '';
                els.previewStatus.textContent = '밀키트 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
                els.previewStatus.classList.remove('hidden');
            });

        mountReserveForm();
    });
})();
