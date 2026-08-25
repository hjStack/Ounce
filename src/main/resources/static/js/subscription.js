/* /subscription — 내 구독. 이번 주에 올 밀키트를 바꾸는 화면.
 *
 * 이 페이지가 Ounce 의 차별점이다. 기존 정기배송은 배송일보다 한참 앞서 주문이
 * 마감돼서 그 주의 상황에 맞춰 바꾸기 어려웠으니, 반대로 "아직 바꿀 수 있다"를
 * 화면 맨 위(카운트다운)와 카드마다(교체 버튼)에서 계속 말해준다.
 *
 * ⚠ 서버에 구독 API 가 아직 하나도 없다(src/main 전체에 구독 코드 없음).
 * 그래서 이번 주 구성은 localStorage 에 두고 화면만 실제처럼 돌아가게 했다.
 * 다만 나중에 배선할 때 화면 코드를 다시 쓰지 않도록, 상태 접근은 아래 Store
 * 한 군데로 모았다. 각 함수에 대응하는 엔드포인트를 TODO 로 적어두었으니
 * 백엔드가 생기면 Store 내부만 fetch 로 갈아내면 된다.
 *
 * 상품 조회·placeholder·토스트·카테고리 분류는 Ounce(product-card.js) 재사용.
 * 끼니 수 → 보관 방식은 OuncePlan(subscription-plan.js) 재사용.
 */
(function () {
    'use strict';

    var ui = window.Ounce;
    var plan = window.OuncePlan;

    var DAY_LABELS = ['월', '화', '수', '목', '금', '토', '일'];
    var STORAGE_KEY = 'ounce.subscription.mock.v1';

    /* ── Store ─────────────────────────────────────────────────
     * 서버가 생기면 여기만 바뀐다. 화면 코드는 Store 만 본다. */
    var Store = {
        // TODO: GET /api/subscriptions/me — { meals, slots: [productId] }
        load: function () {
            try {
                var raw = localStorage.getItem(STORAGE_KEY);
                return raw ? JSON.parse(raw) : null;
            } catch (e) {
                return null;   // 사파리 프라이빗 모드 등에서 localStorage 가 던진다
            }
        },

        // TODO: PUT /api/subscriptions/me/weeks/current — { slots: [productId] }
        saveWeek: function (data) {
            try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
            } catch (e) { /* 저장 못 해도 화면은 계속 동작해야 한다 */ }
            return Promise.resolve();
        },

        // TODO: PATCH /api/subscriptions/me — { meals }  (다음 주부터 적용)
        savePlan: function (data) {
            return this.saveWeek(data);
        }
    };

    /* ── 상태 ──────────────────────────────────────────────────
     * slots: 이번 주 구성(작업 중). saved: 마지막으로 저장된 구성.
     * 둘을 비교해서 "변경 취소"와 저장 바를 띄운다. */
    var state = {
        meals: 5,        // 이번 주 슬롯 수 (이번 주는 확정이므로 화면에서 바꾸지 않는다)
        planMeals: 5,    // 다음 주부터 적용할 끼니 수
        slots: [],       // productId 배열
        saved: [],
        locked: false    // 변경 마감이 지났는가
    };

    var catalog = [];
    var byId = {};
    var pickerSlotIndex = null;
    var pickerCategory = 'all';
    var els = {};

    function $(id) { return document.getElementById(id); }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    /* ── 마감 카운트다운 ───────────────────────────────────────
     * 마감 시점은 서버가 절대 시각(ISO)으로 내려준다. 확정값이 아니라
     * 위탁 제조사와 협의 후 정해질 값이라 컨트롤러 상수 두 개로 모아뒀다. */
    function startCountdown(deadlineIso) {
        var deadline = new Date(deadlineIso);
        if (isNaN(deadline.getTime())) return;

        function tick() {
            var remain = deadline.getTime() - Date.now();

            if (remain <= 0) {
                lockChanges();
                return;
            }

            var totalMinutes = Math.floor(remain / 60000);
            var days = Math.floor(totalMinutes / (60 * 24));
            var hours = Math.floor(totalMinutes / 60) % 24;
            var minutes = totalMinutes % 60;

            /* 남은 시간이 하루 이상이면 초 단위는 의미가 없다.
               하루 안쪽으로 들어오면 분까지 보여주고 갱신 주기를 줄인다. */
            els.countdown.textContent = days > 0
                ? days + '일 ' + hours + '시간'
                : hours + '시간 ' + minutes + '분';

            setTimeout(tick, days > 0 ? 60000 : 20000);
        }
        tick();
    }

    /** 마감이 지난 상태. 교체·저장을 막고 왜 막혔는지 말해준다. */
    function lockChanges() {
        state.locked = true;

        els.deadlineBar.className =
            'flex flex-wrap items-center gap-x-3 gap-y-1.5 rounded-xl border border-background-200' +
            ' bg-background-100 px-4 py-3.5 mb-8';
        els.deadlineBar.innerHTML =
            '<i class="ri-lock-line text-foreground-400 text-lg"></i>' +
            '<p class="text-sm text-foreground-600">' +
            '<span class="font-semibold">이번 주 변경이 마감됐습니다.</span> ' +
            '곧 제조에 들어가요. 다음 주 구성은 마감 이후 열립니다.</p>';

        els.slotsHint.textContent = '이번 주 구성은 확정됐습니다. 다음 주 구성은 배송 후에 고를 수 있어요.';
        renderSlots();
        renderSaveBar();
    }

    /* ── 요약 ──────────────────────────────────────────────── */
    function renderSummary() {
        var current = plan.of(state.meals);
        els.summaryMeals.textContent = current.meals;
        els.summaryStorage.textContent = current.storage.label;
        els.summaryStorageIcon.className = current.storage.icon + ' text-primary-600 text-lg';
        els.summaryDelivery.textContent = els.root.dataset.deliveryLabel || '–';
    }

    /* ── 슬롯 ──────────────────────────────────────────────── */
    function slotHTML(productId, index) {
        var product = byId[productId];
        var day = DAY_LABELS[index % 7];

        /* 저장된 구성과 달라진 슬롯은 표시해준다. 여러 칸을 바꾸다 보면
           뭘 건드렸는지 잊어버린다. */
        var changed = state.saved[index] !== productId;

        if (!product) {
            // 상품이 사라졌거나(품절 삭제) 아직 못 고른 빈 슬롯
            return '' +
            '<article class="flex flex-col overflow-hidden rounded-xl border border-dashed' +
                ' border-background-300 bg-background-50">' +
                '<div class="flex aspect-square items-center justify-center text-foreground-300">' +
                    '<i class="ri-add-line text-3xl"></i></div>' +
                '<div class="p-3.5">' +
                    '<p class="text-[11px] font-bold text-foreground-400 mb-1">' + day + '</p>' +
                    '<p class="text-[13px] text-foreground-400 mb-3">비어 있음</p>' +
                    slotButtonHTML(index, '고르기') +
                '</div>' +
            '</article>';
        }

        var parts = ui.splitName(product.name);

        return '' +
        '<article class="flex flex-col overflow-hidden rounded-xl border bg-white transition-colors ' +
            (changed ? 'border-primary-300 ring-1 ring-primary-200' : 'border-background-200') + '">' +

            '<div class="relative aspect-square overflow-hidden bg-background-100">' +
                '<img src="' + escapeHtml(product.imageUrl || ui.PLACEHOLDER) + '"' +
                    ' alt="' + escapeHtml(parts.title) + '" loading="lazy"' +
                    ' class="h-full w-full object-cover"' +
                    ' onerror="this.onerror=null;this.src=Ounce.PLACEHOLDER"/>' +
                '<span class="absolute left-2 top-2 rounded-md bg-white/95 px-2 py-1 text-[11px]' +
                    ' font-bold text-foreground-700 shadow-sm backdrop-blur-sm">' + day + '</span>' +
                (changed
                    ? '<span class="absolute right-2 top-2 rounded-md bg-primary-500 px-2 py-1' +
                        ' text-[11px] font-bold text-white shadow-sm">변경</span>'
                    : '') +
            '</div>' +

            '<div class="flex flex-1 flex-col p-3.5">' +
                '<h3 class="clamp-2 text-[14px] font-semibold leading-snug text-foreground-950 mb-1">' +
                    escapeHtml(parts.title) + '</h3>' +
                '<p class="clamp-1 text-xs text-foreground-500 mb-3">' +
                    escapeHtml(product.description) + '</p>' +
                '<div class="mt-auto">' + slotButtonHTML(index, '교체') + '</div>' +
            '</div>' +
        '</article>';
    }

    function slotButtonHTML(index, label) {
        if (state.locked) {
            return '<span class="flex w-full items-center justify-center gap-1.5 rounded-lg' +
                ' bg-background-100 px-3 py-2 text-xs font-semibold text-foreground-400">' +
                '<i class="ri-lock-line text-sm"></i>확정</span>';
        }
        return '<button type="button" class="slot-swap-btn flex w-full items-center justify-center' +
            ' gap-1.5 rounded-lg border border-background-200 bg-white px-3 py-2 text-xs' +
            ' font-semibold text-foreground-700 transition-colors hover:border-primary-300' +
            ' hover:bg-primary-50 hover:text-primary-700" data-slot="' + index + '">' +
            '<i class="ri-repeat-2-line text-sm"></i>' + label + '</button>';
    }

    function renderSlots() {
        els.slotGrid.innerHTML = state.slots.map(slotHTML).join('');
        els.slotGrid.removeAttribute('aria-busy');
    }

    /* ── 저장 바 ───────────────────────────────────────────── */
    function changedCount() {
        var n = 0;
        for (var i = 0; i < state.slots.length; i++) {
            if (state.slots[i] !== state.saved[i]) n++;
        }
        return n;
    }

    function renderSaveBar() {
        var count = state.locked ? 0 : changedCount();
        var dirty = count > 0;

        els.saveBar.classList.toggle('translate-y-full', !dirty);
        els.saveBarCount.textContent = count + '곳';
        els.resetBtn.classList.toggle('hidden', !dirty);
        els.resetBtn.classList.toggle('flex', dirty);
    }

    function saveWeek() {
        els.saveBtn.disabled = true;
        Store.saveWeek({ meals: state.meals, planMeals: state.planMeals, slots: state.slots })
            .then(function () {
                state.saved = state.slots.slice();
                renderSlots();
                renderSaveBar();
                ui.toast('이번 주 구성을 저장했습니다.');
            })
            .catch(function () {
                ui.toast('저장하지 못했습니다. 잠시 후 다시 시도해 주세요.', 'error');
            })
            .finally(function () { els.saveBtn.disabled = false; });
    }

    function resetWeek() {
        state.slots = state.saved.slice();
        renderSlots();
        renderSaveBar();
    }

    /* ── 선택 시트 ─────────────────────────────────────────── */
    function pickerCardHTML(product) {
        var parts = ui.splitName(product.name);

        /* 이번 주 다른 칸에 이미 들어간 상품이라는 걸 알려준다. 막지는 않는다
           (같은 메뉴를 두 번 먹겠다는 선택도 정상이다). */
        var usedElsewhere = state.slots.some(function (id, i) {
            return id === product.productId && i !== pickerSlotIndex;
        });
        var isCurrent = state.slots[pickerSlotIndex] === product.productId;

        return '' +
        '<button type="button" class="picker-item group relative overflow-hidden rounded-xl border' +
            ' text-left transition-all hover:-translate-y-0.5 hover:shadow-md ' +
            (isCurrent ? 'border-primary-500 ring-1 ring-primary-300' : 'border-background-200') +
            '" data-product-id="' + escapeHtml(product.productId) + '">' +

            '<div class="relative aspect-square overflow-hidden bg-background-100">' +
                '<img src="' + escapeHtml(product.imageUrl || ui.PLACEHOLDER) + '"' +
                    ' alt="" loading="lazy" class="h-full w-full object-cover"' +
                    ' onerror="this.onerror=null;this.src=Ounce.PLACEHOLDER"/>' +
                (isCurrent
                    ? '<span class="absolute inset-0 flex items-center justify-center' +
                        ' bg-primary-900/45"><span class="rounded-full bg-white px-3 py-1 text-[11px]' +
                        ' font-bold text-primary-700">현재 선택</span></span>'
                    : usedElsewhere
                        ? '<span class="absolute left-2 top-2 rounded-md bg-foreground-950/70 px-2 py-1' +
                            ' text-[10px] font-semibold text-white">이번 주에 있음</span>'
                        : '') +
            '</div>' +
            '<div class="p-2.5">' +
                '<p class="clamp-2 text-[13px] font-semibold leading-snug text-foreground-950">' +
                    escapeHtml(parts.title) + '</p>' +
            '</div>' +
        '</button>';
    }

    function renderPickerGrid() {
        var keyword = (els.pickerSearch.value || '').trim().toLowerCase();

        var list = catalog.filter(function (product) {
            if (!ui.inCategory(product, pickerCategory)) return false;
            if (!keyword) return true;
            return String(product.name || '').toLowerCase().indexOf(keyword) !== -1;
        });

        if (list.length === 0) {
            els.pickerGrid.innerHTML = '';
            els.pickerStatus.textContent = keyword
                ? '"' + keyword + '"에 해당하는 밀키트가 없습니다.'
                : '이 분류에 준비된 밀키트가 없습니다.';
            els.pickerStatus.classList.remove('hidden');
            return;
        }

        els.pickerStatus.classList.add('hidden');
        els.pickerGrid.innerHTML = list.map(pickerCardHTML).join('');
    }

    function renderPickerChips() {
        var chips = [{ key: 'all', label: '전체', icon: 'ri-grid-line' }].concat(ui.CATEGORIES);

        els.pickerChips.innerHTML = chips.map(function (c) {
            var on = c.key === pickerCategory;
            return '<button type="button" class="picker-chip flex items-center gap-1.5 rounded-full' +
                ' px-3.5 py-1.5 text-xs font-semibold transition-colors ' +
                (on ? 'bg-primary-500 text-white'
                    : 'bg-background-100 text-foreground-600 hover:bg-background-200') +
                '" data-category="' + c.key + '">' +
                '<i class="' + c.icon + '"></i>' + escapeHtml(c.label) + '</button>';
        }).join('');
    }

    function openPicker(slotIndex) {
        if (state.locked) return;

        pickerSlotIndex = slotIndex;
        pickerCategory = 'all';
        els.pickerSearch.value = '';
        els.pickerSubtitle.textContent =
            DAY_LABELS[slotIndex % 7] + '요일 슬롯 · 마감 전까지 몇 번이든 바꿀 수 있어요';

        renderPickerChips();
        renderPickerGrid();

        els.picker.classList.remove('hidden');
        // 다음 프레임에 클래스를 붙여야 transform 트랜지션이 실제로 재생된다
        requestAnimationFrame(function () { els.picker.classList.add('is-open'); });
        document.body.style.overflow = 'hidden';
        setTimeout(function () { els.pickerSearch.focus(); }, 260);
    }

    function closePicker() {
        els.picker.classList.remove('is-open');
        document.body.style.overflow = '';
        setTimeout(function () { els.picker.classList.add('hidden'); }, 260);
        pickerSlotIndex = null;
    }

    function chooseProduct(productId) {
        if (pickerSlotIndex == null) return;
        state.slots[pickerSlotIndex] = productId;
        renderSlots();
        renderSaveBar();
        closePicker();
    }

    /* ── 끼니 수 변경 (다음 주부터) ─────────────────────────── */
    function applyPlanSlider(meals) {
        var selected = plan.of(meals);

        els.planMeals.textContent = selected.meals;
        els.planStorageIcon.className = selected.storage.icon + ' text-foreground-500 text-base';
        els.planStorageLabel.textContent = selected.storage.label;
        els.planStorageNote.textContent = selected.storage.note;

        els.planTicks.forEach(function (tick) {
            var on = Number(tick.dataset.meals) === selected.meals;
            tick.classList.toggle('text-foreground-400', !on);
            tick.classList.toggle('text-primary-600', on);
            tick.classList.toggle('font-bold', on);
        });

        // 지금 플랜과 같으면 저장할 게 없다
        els.planSaveBtn.disabled = selected.meals === state.planMeals;
    }

    function savePlan() {
        var next = plan.clamp(els.planRange.value);
        els.planSaveBtn.disabled = true;

        Store.savePlan({ meals: state.meals, planMeals: next, slots: state.slots })
            .then(function () {
                state.planMeals = next;
                ui.toast('다음 주부터 주 ' + next + '끼로 받습니다.');
                applyPlanSlider(next);
            })
            .catch(function () {
                ui.toast('변경하지 못했습니다. 잠시 후 다시 시도해 주세요.', 'error');
                els.planSaveBtn.disabled = false;
            });
    }

    /* ── 일시정지 · 해지 ───────────────────────────────────────
     * 서버 엔드포인트가 없어서 아직 실제로 처리되지 않는다.
     * 되지도 않는 걸 됐다고 말하지 않는다 — 준비 중임을 그대로 알린다. */
    function mountLifecycleButtons() {
        var pause = $('pause-btn');
        var cancel = $('cancel-btn');

        // TODO: POST /api/subscriptions/me/skip — { week: 'current' }
        if (pause) {
            pause.addEventListener('click', function () {
                ui.toast('건너뛰기는 정식 오픈 시 제공됩니다.');
            });
        }

        // TODO: DELETE /api/subscriptions/me
        if (cancel) {
            cancel.addEventListener('click', function () {
                if (!confirm('구독을 해지하면 다음 주부터 배송이 멈춥니다.\n이번 주 구성은 그대로 도착합니다.\n\n해지하시겠어요?')) return;
                ui.toast('해지는 정식 오픈 시 제공됩니다.');
            });
        }
    }

    /* ── 초기 구성 ─────────────────────────────────────────────
     * 저장된 게 있으면 그걸 쓰고, 없으면 카탈로그에서 겹치지 않게 뽑아 채운다.
     * 저장된 상품이 카탈로그에서 사라졌으면 그 칸은 빈 슬롯으로 남긴다
     * (조용히 다른 메뉴로 바꿔치기하면 사용자가 고른 걸 뒤집는 셈이다). */
    function seedSlots(count) {
        var pool = catalog.slice();
        var picked = [];
        while (picked.length < count && pool.length > 0) {
            picked.push(pool.splice(Math.floor(Math.random() * pool.length), 1)[0].productId);
        }
        while (picked.length < count) picked.push(null);
        return picked;
    }

    function hydrate() {
        var stored = Store.load();

        if (stored && Array.isArray(stored.slots) && stored.slots.length > 0) {
            state.meals = plan.clamp(stored.meals != null ? stored.meals : stored.slots.length);
            state.planMeals = plan.clamp(stored.planMeals != null ? stored.planMeals : state.meals);
            state.slots = stored.slots.slice(0, state.meals);
            // 저장 당시보다 끼니 수가 늘어난 경우 남은 칸을 채운다
            while (state.slots.length < state.meals) state.slots.push(null);
        } else {
            state.slots = seedSlots(state.meals);
        }

        state.saved = state.slots.slice();
    }

    /* ── 부팅 ──────────────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', function () {
        var root = $('subscription-root');
        if (!root) return;

        els = {
            root: root,
            summaryMeals: $('summary-meals'),
            summaryStorage: $('summary-storage'),
            summaryStorageIcon: $('summary-storage-icon'),
            summaryDelivery: $('summary-delivery'),
            deadlineBar: $('deadline-bar'),
            countdown: $('deadline-countdown'),
            deadlineLabel: $('deadline-label'),
            slotsHint: $('slots-hint'),
            slotGrid: $('slot-grid'),
            slotStatus: $('slot-status'),
            saveBar: $('save-bar'),
            saveBarCount: $('save-bar-count'),
            saveBtn: $('save-btn'),
            resetBtn: $('reset-btn'),
            picker: $('picker'),
            pickerPanel: $('picker-panel'),
            pickerBackdrop: $('picker-backdrop'),
            pickerClose: $('picker-close'),
            pickerSubtitle: $('picker-subtitle'),
            pickerSearch: $('picker-search'),
            pickerChips: $('picker-chips'),
            pickerGrid: $('picker-grid'),
            pickerStatus: $('picker-status'),
            planRange: $('plan-range'),
            planMeals: $('plan-meals'),
            planStorageIcon: $('plan-storage-icon'),
            planStorageLabel: $('plan-storage-label'),
            planStorageNote: $('plan-storage-note'),
            planSaveBtn: $('plan-save-btn'),
            planTicks: Array.prototype.slice.call(document.querySelectorAll('.plan-tick'))
        };

        els.deadlineLabel.textContent = root.dataset.deadlineLabel
            ? root.dataset.deadlineLabel + ' 마감'
            : '';

        ui.renderSkeletons(els.slotGrid, state.meals, 'category');

        /* ── 이벤트 위임: 카드가 다시 그려져도 계속 동작한다 ── */
        els.slotGrid.addEventListener('click', function (e) {
            var btn = e.target.closest('.slot-swap-btn');
            if (btn) openPicker(Number(btn.dataset.slot));
        });

        els.pickerGrid.addEventListener('click', function (e) {
            var item = e.target.closest('.picker-item');
            if (item) chooseProduct(item.dataset.productId);
        });

        els.pickerChips.addEventListener('click', function (e) {
            var chip = e.target.closest('.picker-chip');
            if (!chip) return;
            pickerCategory = chip.dataset.category;
            renderPickerChips();
            renderPickerGrid();
        });

        els.pickerSearch.addEventListener('input', renderPickerGrid);
        els.pickerClose.addEventListener('click', closePicker);
        els.pickerBackdrop.addEventListener('click', closePicker);
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !els.picker.classList.contains('hidden')) closePicker();
        });

        els.saveBtn.addEventListener('click', saveWeek);
        els.resetBtn.addEventListener('click', resetWeek);

        els.planRange.min = plan.MIN_MEALS;
        els.planRange.max = plan.MAX_MEALS;
        els.planRange.addEventListener('input', function () { applyPlanSlider(els.planRange.value); });
        els.planSaveBtn.addEventListener('click', savePlan);

        mountLifecycleButtons();
        startCountdown(root.dataset.deadline);

        ui.fetchCatalog()
            .then(function (products) {
                catalog = products || [];
                catalog.forEach(function (p) { byId[p.productId] = p; });

                hydrate();
                renderSummary();
                renderSlots();
                renderSaveBar();

                els.planRange.value = state.planMeals;
                applyPlanSlider(state.planMeals);
            })
            .catch(function () {
                els.slotGrid.innerHTML = '';
                els.slotStatus.textContent = '구독 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
                els.slotStatus.classList.remove('hidden');
            });
    });
})();
