/* Ounce 상품 카드 — 홈(index)과 상품목록(products)이 공유하는 단일 렌더러.
 *
 * 왜 JS 인가: 두 페이지 모두 /api/products 를 fetch 해서 그리기 때문에
 * Thymeleaf fragment 로는 같은 카드를 쓸 수 없다. 카드 정의가 두 군데로
 * 갈라지는 걸 막기 위해 JS 쪽을 단일 출처로 잡았다.
 *
 * 표시하는 값은 API 가 실제로 주는 것만 쓴다:
 *   name / description / basePrice / stock / status / imageUrl
 * 조리시간·평점·리뷰수 같은 필드는 서버에 없으므로 카드에 만들어 붙이지 않는다.
 * salePrice 도 쓰지 않는다 (서버 계산이 discountPercent * basePrice 라 값이 깨져 있음).
 */
window.Ounce = (function () {
    'use strict';

    // 이미지 로드 실패용 플레이스홀더.
    // 예전엔 onerror 가 /img/no-image.png 를 가리켰는데 그 파일이 없어서
    // 404 → onerror → 404 무한 루프가 났다. 인라인 SVG 라 절대 실패하지 않는다.
    var PLACEHOLDER = 'data:image/svg+xml;utf8,' + encodeURIComponent(
        '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">' +
        '<rect width="400" height="400" fill="#f7f6f4"/>' +
        '<path d="M170 210h60M175 185a8 8 0 1116 0 8 8 0 01-16 0zm34 0a8 8 0 1116 0 8 8 0 01-16 0z" ' +
        'stroke="#d9d6d0" stroke-width="6" fill="none" stroke-linecap="round"/>' +
        '<text x="200" y="260" text-anchor="middle" fill="#a8a29e" ' +
        'font-family="sans-serif" font-size="15">준비 중인 이미지</text></svg>'
    );

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function won(value) {
        return Number(value || 0).toLocaleString('ko-KR') + '원';
    }

    /* 상품명이 전부 "[1인분] 김치찌개 밀키트" 형태다.
     * 20개 카드에 "[1인분]"과 "밀키트"가 반복되면 제목이 안 읽히니
     * 접두사는 배지로 빼고 제목은 요리 이름만 남긴다. */
    function splitName(name) {
        var raw = String(name || '');
        var servingMatch = raw.match(/^\s*\[([^\]]+)\]\s*/);
        return {
            serving: servingMatch ? servingMatch[1] : '',
            title: raw.replace(/^\s*\[[^\]]+\]\s*/, '').trim() || raw
        };
    }

    /* 재고/상태로만 판단 — 없는 데이터를 추측하지 않는다. */
    function stockState(product) {
        var stock = Number(product.stock);
        if (product.status === 'SOLD_OUT' || stock <= 0) return 'soldout';
        if (stock <= 10) return 'low';
        return 'ok';
    }

    /** 상품 카드 마크업.
     *  링크는 카드 전체를 덮는 오버레이 <a> 로 처리한다.
     *  담기 버튼을 <a> 안에 넣으면 잘못된 중첩이라 형제로 두고 z-index 로 띄웠다. */
    function cardHTML(product) {
        var parts = splitName(product.name);
        var state = stockState(product);
        var isTimeDeal = product.status === 'TIME_DEAL';
        var soldOut = state === 'soldout';

        var badges = '';
        if (isTimeDeal) {
            // 세일 배지만 테라코타(deal). 나머지 강조는 초록(primary) 이라 세일이 튄다.
            badges += '<span class="px-2 py-1 rounded-md bg-deal-500 text-white text-[11px] font-bold' +
                ' shadow-sm">미드나이트</span>';
        }
        if (parts.serving) {
            badges += '<span class="px-2 py-1 rounded-md bg-white/95 text-foreground-700 text-[11px]' +
                ' font-semibold shadow-sm backdrop-blur-sm">' + escapeHtml(parts.serving) + '</span>';
        }
        if (state === 'low') {
            badges += '<span class="px-2 py-1 rounded-md bg-red-600 text-white text-[11px] font-bold' +
                ' shadow-sm">품절임박</span>';
        }

        return '' +
        '<article class="group relative flex flex-col overflow-hidden rounded-xl border border-background-200' +
            ' bg-white transition-all duration-200 hover:-translate-y-0.5 hover:border-background-300' +
            ' hover:shadow-lg' + (soldOut ? ' opacity-70' : '') + '">' +

            '<div class="relative aspect-square overflow-hidden bg-background-100">' +
                '<img src="' + escapeHtml(product.imageUrl || PLACEHOLDER) + '"' +
                    ' alt="' + escapeHtml(parts.title) + '" loading="lazy"' +
                    ' class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.06]"' +
                    ' onerror="this.onerror=null;this.src=Ounce.PLACEHOLDER"/>' +
                '<div class="absolute left-2 top-2 flex flex-wrap gap-1.5">' + badges + '</div>' +
                (soldOut
                    ? '<div class="absolute inset-0 flex items-center justify-center bg-foreground-950/55">' +
                        '<span class="rounded-full bg-white/95 px-4 py-1.5 text-xs font-bold' +
                        ' text-foreground-800">품절</span></div>'
                    : '') +
            '</div>' +

            '<div class="flex flex-1 flex-col gap-1 p-3.5 md:p-4">' +
                '<h3 class="clamp-1 text-[15px] font-semibold leading-snug text-foreground-950">' +
                    escapeHtml(parts.title) + '</h3>' +
                '<p class="clamp-1 text-xs leading-relaxed text-foreground-500">' +
                    escapeHtml(product.description) + '</p>' +

                '<div class="mt-1 flex items-center gap-1.5">' +
                    '<span class="h-1 w-1 rounded-full bg-secondary-600"></span>' +
                    '<span class="text-[11px] font-medium text-secondary-600">내일 새벽 7시 도착</span>' +
                '</div>' +

                '<div class="mt-2.5 flex items-end justify-between gap-2">' +
                    '<p class="text-base font-bold tracking-tight text-foreground-950">' +
                        won(product.basePrice) + '</p>' +
                    (soldOut
                        ? '<span class="relative z-20 flex h-9 w-9 items-center justify-center rounded-full' +
                            ' bg-background-200 text-foreground-400" aria-hidden="true">' +
                            '<i class="ri-shopping-cart-2-line text-sm"></i></span>'
                        : '<button type="button" class="add-to-cart-btn relative z-20 flex h-9 w-9 shrink-0' +
                            ' items-center justify-center rounded-full bg-primary-500 text-white shadow-sm' +
                            ' transition-all duration-150 hover:scale-105 hover:bg-primary-600 hover:shadow-md' +
                            ' active:scale-95" data-product-id="' + escapeHtml(product.productId) + '"' +
                            ' aria-label="' + escapeHtml(parts.title) + ' 장바구니에 담기">' +
                            '<i class="ri-shopping-cart-2-line text-sm"></i></button>') +
                '</div>' +
            '</div>' +

            /* 카드 전체를 덮는 링크. 담기 버튼(z-20)보다 아래에 둔다. */
            '<a href="/products-detail/' + encodeURIComponent(product.productId) + '"' +
                ' class="absolute inset-0 z-10 rounded-xl focus-visible:outline focus-visible:outline-2' +
                ' focus-visible:outline-offset-2 focus-visible:outline-primary-500"' +
                ' aria-label="' + escapeHtml(parts.title) + ' 상세 보기"></a>' +
        '</article>';
    }

    /* ── 스켈레톤 ──────────────────────────────────────────────
     * 실제 카드와 같은 골격(정사각 이미지 + 제목/설명/가격 줄)을 그려서
     * 상품이 도착할 때 레이아웃이 튀지 않게 한다. */
    function skeletonHTML() {
        return '' +
        '<div class="animate-pulse overflow-hidden rounded-xl border border-background-200 bg-white"' +
            ' aria-hidden="true">' +
            '<div class="aspect-square w-full bg-background-200"></div>' +
            '<div class="flex flex-col gap-2 p-3.5 md:p-4">' +
                '<div class="h-4 w-3/4 rounded bg-background-200"></div>' +
                '<div class="h-3 w-full rounded bg-background-100"></div>' +
                '<div class="mt-1 h-3 w-1/3 rounded bg-background-100"></div>' +
                '<div class="mt-2.5 flex items-center justify-between">' +
                    '<div class="h-5 w-20 rounded bg-background-200"></div>' +
                    '<div class="h-9 w-9 rounded-full bg-background-200"></div>' +
                '</div>' +
            '</div>' +
        '</div>';
    }

    /** 카테고리 카드용 스켈레톤 (홈 '오늘은 뭐 먹지?' 섹션). */
    function categorySkeletonHTML() {
        return '<div class="animate-pulse aspect-[4/5] md:aspect-square rounded-xl bg-background-200"' +
            ' aria-hidden="true"></div>';
    }

    /** 컨테이너를 스켈레톤 n개로 채운다. */
    function renderSkeletons(container, count, kind) {
        if (!container) return;
        var make = kind === 'category' ? categorySkeletonHTML : skeletonHTML;
        var html = '';
        for (var i = 0; i < count; i++) html += make();
        container.innerHTML = html;
        container.setAttribute('aria-busy', 'true');
    }

    /* ── 카테고리 ──────────────────────────────────────────────
     * 서버에 category 매핑 API 가 없어서(ProductCategory 엔티티는 있지만 노출 안 됨)
     * 상품명 키워드로 분류한다. 20개 상품 전부 정확히 한 곳에 들어가도록 맞춘 규칙이고,
     * 매칭 안 되는 신규 상품은 '전체'에서만 보인다 — 어디에도 안 걸려 사라지는 게 아니다.
     * 서버가 카테고리를 내려주게 되면 이 블록만 갈아내면 된다. */
    var CATEGORIES = [
        { key: 'stew',  label: '찌개·국물', icon: 'ri-fire-line',       pattern: /찌개|전골|육개장|마라탕|볶음탕/ },
        { key: 'noodle', label: '면·파스타', icon: 'ri-bowl-line',      pattern: /파스타|올리오|라멘|우동|짜장|국수/ },
        { key: 'meat',  label: '고기·구이',  icon: 'ri-restaurant-line', pattern: /불고기|갈비|제육|삼겹/ },
        { key: 'nabe',  label: '나베·샤브',  icon: 'ri-drop-line',       pattern: /나베|스키야키|샤브/ },
        { key: 'etc',   label: '분식·해물',  icon: 'ri-star-smile-line', pattern: /떡볶이|감바스|새우|어묵/ }
    ];

    function categoryOf(product) {
        var name = String(product.name || '');
        for (var i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].pattern.test(name)) return CATEGORIES[i].key;
        }
        return null;
    }

    function inCategory(product, key) {
        if (!key || key === 'all') return true;
        return categoryOf(product) === key;
    }

    /* ── 카탈로그 ──────────────────────────────────────────────
     * 상품이 20개짜리 한 페이지라 전체를 한 번 받아 캐시하고,
     * 검색·카테고리·정렬은 클라이언트에서 처리한다.
     * /api/products/search 를 쓰지 않는 이유: 그 응답에는 basePrice 가 없고
     * salePrice(=0) 만 있어서 가격을 0원으로 표시하게 된다.
     * (상품이 수백 개로 늘면 서버 페이징/검색으로 다시 옮겨야 한다.) */
    var catalogPromise = null;

    function fetchCatalog() {
        if (!catalogPromise) {
            catalogPromise = fetch('/api/products?page=0&size=200', { credentials: 'include' })
                .then(function (res) {
                    if (!res.ok) throw new Error('상품 조회 실패: ' + res.status);
                    return res.json();
                })
                .then(function (page) { return page.content || []; })
                .catch(function (err) {
                    catalogPromise = null;   // 다음 호출에서 재시도할 수 있게
                    throw err;
                });
        }
        return catalogPromise;
    }

    /* ── 장바구니 ────────────────────────────────────────────── */
    var loginState = null;

    function isLoggedIn() {
        if (!loginState) {
            loginState = fetch('/api/members/me', { credentials: 'include' })
                .then(function (res) { return res.ok; })
                .catch(function () { return false; });
        }
        return loginState;
    }

    function toast(message, type) {
        var existing = document.getElementById('ounce-js-toast');
        if (existing) existing.remove();

        var el = document.createElement('div');
        el.id = 'ounce-js-toast';
        el.className = 'fixed top-24 left-1/2 -translate-x-1/2 z-[70] flex items-center gap-2 px-5 py-3' +
            ' rounded-lg shadow-lg text-sm font-medium text-white transition-opacity duration-300 ' +
            (type === 'error' ? 'bg-red-600' : 'bg-secondary-600');
        el.innerHTML = '<i class="' + (type === 'error' ? 'ri-error-warning-line' : 'ri-check-line') +
            ' text-lg"></i><span></span>';
        el.querySelector('span').textContent = message;
        document.body.appendChild(el);

        setTimeout(function () {
            el.style.opacity = '0';
            setTimeout(function () { el.remove(); }, 300);
        }, 2600);
    }

    function goLogin() {
        if (confirm('로그인이 필요합니다.\n로그인 페이지로 이동할까요?')) {
            window.location.href = '/login';
        }
    }

    function bumpCartBadge(delta) {
        document.querySelectorAll('.cart-badge').forEach(function (badge) {
            var current = parseInt(badge.textContent, 10);
            var next = (isNaN(current) ? 0 : current) + delta;
            badge.textContent = next > 99 ? '99+' : next;
            badge.classList.toggle('hidden', next <= 0);
        });
    }

    function addToCart(productId, button) {
        return isLoggedIn().then(function (loggedIn) {
            if (!loggedIn) { goLogin(); return; }
            if (button) button.disabled = true;

            return fetch('/api/carts/items', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                credentials: 'include',
                body: 'productId=' + encodeURIComponent(productId) + '&quantity=1'
            }).then(function (res) {
                if (res.status === 401) { loginState = null; goLogin(); return; }
                if (!res.ok) { toast('장바구니에 담지 못했습니다.', 'error'); return; }
                bumpCartBadge(1);
                toast('장바구니에 담았습니다.');
            }).catch(function () {
                toast('오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
            }).finally(function () {
                if (button) button.disabled = false;
            });
        });
    }

    /** 컨테이너에 담기 버튼 위임 핸들러를 붙인다 (카드가 나중에 그려져도 동작). */
    function mountCartHandlers(root) {
        var container = root || document;
        if (container.dataset && container.dataset.cartMounted === '1') return;
        if (container.dataset) container.dataset.cartMounted = '1';

        container.addEventListener('click', function (e) {
            var button = e.target.closest('.add-to-cart-btn');
            if (!button) return;
            e.preventDefault();
            e.stopPropagation();      // 카드 전체를 덮는 링크로 이동하지 않게
            addToCart(button.dataset.productId, button);
        });
    }

    /** 상품 배열을 컨테이너에 렌더. */
    function render(container, products) {
        if (!container) return;
        container.innerHTML = products.map(cardHTML).join('');
        container.removeAttribute('aria-busy');
    }

    return {
        PLACEHOLDER: PLACEHOLDER,
        CATEGORIES: CATEGORIES,
        categoryOf: categoryOf,
        inCategory: inCategory,
        cardHTML: cardHTML,
        skeletonHTML: skeletonHTML,
        renderSkeletons: renderSkeletons,
        splitName: splitName,
        won: won,
        fetchCatalog: fetchCatalog,
        addToCart: addToCart,
        mountCartHandlers: mountCartHandlers,
        render: render,
        toast: toast
    };
})();
