/* /support — 고객센터. 자주 묻는 질문(FAQ) + 1:1 문의(Q&A).
 *
 * 화면을 둘로 나눈 이유:
 * 문의로 들어오는 질문 대부분은 이미 답이 정해진 것들(마감 시각, 도착 시간, 보관 방식)이다.
 * 그래서 FAQ 를 기본 탭으로 두고 검색을 붙였고, 1:1 문의는 그걸로 안 풀릴 때 쓰는
 * 두 번째 탭으로 뒀다. 고객은 기다리지 않고 답을 얻고, 답변할 쪽은 진짜 문의만 받는다.
 *
 * FAQ 본문이 이 파일 안에 있는 이유: 검색·분류가 전부 클라이언트에서 끝나고
 * 서버에 FAQ 테이블도 없다. 문구 하나 고치려고 배포 파이프라인을 타는 건 맞지만,
 * 지금은 FAQ CRUD API 를 새로 만드는 비용이 더 크다.
 *
 * ⚠ FAQ 답변에는 확정된 사실만 적는다. 결제 수단·교환/환불 기준·유선 응대 시간처럼
 * 아직 정하지 않은 건 단정해서 쓰지 않고 "1:1 문의로 확인 후 안내"로 넘긴다.
 * 화면에 적은 순간 지켜야 하는 약속이 되기 때문이다.
 * (구독 가격·무료배송 기준은 공개물에 넣지 않는다는 규칙도 여기 적용된다.)
 *
 * 1:1 문의는 실제 API 를 쓴다:
 *   GET    /api/qna/me?page&size   내 문의 목록 (Page<QnaResponse>)
 *   POST   /api/qna                등록
 *   PATCH  /api/qna/{id}           수정 (답변 완료 건은 서버가 거절)
 *   DELETE /api/qna/{id}           삭제 (답변 완료 건은 서버가 거절)
 *
 * 토스트는 Ounce(product-card.js) 재사용.
 */
(function () {
    'use strict';

    var ui = window.Ounce;

    var PAGE_SIZE = 10;

    /* 문의 유형. FAQ 분류와 문의 폼의 선택지를 같은 목록에서 뽑는다.
     * 서버 QnA.category 는 자유 문자열이라 label 을 그대로 저장한다.
     * (관리자 화면에서 코드값을 사람 말로 다시 매핑하지 않아도 되게) */
    var CATEGORIES = [
        { key: 'order', label: '주문·결제', icon: 'ri-shopping-bag-3-line' },
        { key: 'delivery', label: '배송', icon: 'ri-truck-line' },
        { key: 'product', label: '상품·보관', icon: 'ri-restaurant-line' },
        { key: 'subscription', label: '구독', icon: 'ri-calendar-check-line' },
        { key: 'refund', label: '취소·환불', icon: 'ri-arrow-go-back-line' },
        { key: 'account', label: '회원·기타', icon: 'ri-user-settings-line' }
    ];

    /* FAQ. q 는 고객이 실제로 쓸 말로, a 는 사이트에 이미 적혀 있는 사실로 쓴다. */
    var FAQS = [
        {
            cat: 'order',
            q: '주문은 몇 시까지 하면 되나요?',
            a: '매일 밤 23:00까지 결제된 주문이 그날 회차에 포함됩니다. 23시를 넘긴 주문은 다음 회차로 넘어갑니다.'
        },
        {
            cat: 'order',
            q: '주문한 내용을 확인하고 싶어요.',
            a: '주문 내역에서 결제한 상품과 배송 상태를 확인할 수 있습니다.',
            link: { href: '/orders', text: '주문 내역 보기' }
        },
        {
            cat: 'order',
            q: '비회원으로도 주문할 수 있나요?',
            a: '아니요. 주문·배송 조회와 1:1 문의 답변이 모두 계정에 연결되기 때문에 로그인이 필요합니다.',
            link: { href: '/signup', text: '회원가입' }
        },
        {
            cat: 'delivery',
            q: '언제 도착하나요?',
            a: '수도권은 주문 다음 날 새벽 7시 도착이 기준입니다. 지역에 따라 배송 방식과 도착 시간이 달라질 수 있어, ' +
                '수도권 외 지역은 주문서에서 예상 도착일을 확인해 주세요.'
        },
        {
            cat: 'delivery',
            q: '새벽에 받으면 바로 냉장고에 넣어야 하나요?',
            a: '보냉 포장으로 배송하지만, 받으신 뒤에는 되도록 빨리 냉장 또는 냉동 보관해 주세요. ' +
                '상품별 보관 방식은 상품 상세 페이지에 표기되어 있습니다.'
        },
        {
            cat: 'delivery',
            q: '배송지를 바꾸고 싶어요.',
            a: '주문 마감(밤 23:00) 전까지는 주문 내역에서 변경하거나 취소 후 다시 주문할 수 있습니다. ' +
                '마감이 지난 뒤에는 1:1 문의로 주문번호를 남겨주시면 배송 상태를 확인해 안내드립니다.'
        },
        {
            cat: 'product',
            q: '몇 인분인가요?',
            a: '모든 밀키트가 1인분 정량으로 소분되어 있습니다. 계량하거나 남은 재료를 처리할 일이 없도록 만든 구성입니다.'
        },
        {
            cat: 'product',
            q: '냉장인지 냉동인지 어디서 볼 수 있나요?',
            a: '상품 상세 페이지에 상품별로 표기됩니다. 냉장 상품은 수령 후 5일 내 조리를 권장하고, ' +
                '냉동 상품은 손질과 계량이 끝난 재료를 급속 동결한 것이라 더 오래 두고 드실 수 있습니다.',
            link: { href: '/products', text: '전체 밀키트 보기' }
        },
        {
            cat: 'subscription',
            q: '구독하면 메뉴는 누가 정하나요?',
            a: '고객이 직접 고릅니다. 이번 주에 받을 밀키트를 내 구독 화면에서 하나씩 바꿀 수 있고, ' +
                '변경 마감 전까지는 몇 번이든 다시 손볼 수 있습니다. 남은 시간은 화면 맨 위에 표시됩니다.',
            link: { href: '/subscription', text: '내 구독으로 이동' }
        },
        {
            cat: 'subscription',
            q: '한 주만 쉬거나 해지할 수 있나요?',
            a: '내 구독 화면에서 "이번 주 건너뛰기"로 한 주를 넘기거나 바로 해지할 수 있습니다. ' +
                '해지하면 다음 주부터 배송이 멈추고, 이번 주 구성은 그대로 도착합니다. 해지 후에도 밀키트를 낱개로 주문할 수 있습니다.'
        },
        {
            cat: 'refund',
            q: '주문을 취소하고 싶어요.',
            a: '주문 마감(밤 23:00) 전까지는 주문 내역에서 직접 취소할 수 있습니다. ' +
                '마감 후에는 이미 제조·출고 준비가 시작되므로, 1:1 문의로 주문번호를 남겨주시면 가능한 방법을 안내드립니다.',
            link: { href: '/orders', text: '주문 내역 보기' }
        },
        {
            cat: 'refund',
            q: '상품에 문제가 있었어요.',
            a: '받으신 상품의 상태가 이상하거나 구성품이 빠졌다면 1:1 문의로 주문번호와 함께 알려주세요. ' +
                '사진이 필요한 경우가 많아 이메일(hye_jun0209@icloud.com)로 함께 보내주시면 확인 후 개별 안내드립니다.'
        },
        {
            cat: 'account',
            q: '비밀번호를 잊었어요.',
            a: '구글 계정으로 가입하셨다면 로그인 화면에서 소셜 로그인으로 바로 들어올 수 있습니다. ' +
                '이메일로 가입한 계정의 비밀번호 재설정은 준비 중이라, 1:1 문의로 접수해 주시면 도와드립니다.',
            link: { href: '/login', text: '로그인 화면' }
        },
        {
            cat: 'account',
            q: '탈퇴하고 싶어요.',
            a: '내 계정 화면 아래쪽에서 직접 탈퇴할 수 있습니다. 탈퇴하면 주문 내역·포인트·쿠폰·장바구니가 모두 삭제되고 되돌릴 수 없습니다.',
            link: { href: '/account', text: '내 계정으로 이동' }
        },
        {
            cat: 'account',
            q: '문의하면 답변은 얼마나 걸리나요?',
            a: '1:1 문의는 365일 접수하며, 영업일 기준 1일 내에 답변드립니다. ' +
                '답변이 등록되면 고객센터의 "내 문의" 탭에서 바로 확인할 수 있습니다.'
        }
    ];

    var TAB_ON = 'flex-1 flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-semibold rounded-lg ' +
        'transition-colors bg-white text-foreground-950 shadow-sm';
    var TAB_OFF = 'flex-1 flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-semibold rounded-lg ' +
        'transition-colors text-foreground-500 hover:text-foreground-800';
    var CHIP_ON = 'px-3.5 py-1.5 text-xs font-semibold rounded-full bg-primary-500 text-white transition-colors';
    var CHIP_OFF = 'px-3.5 py-1.5 text-xs font-medium rounded-full bg-white border border-background-200 ' +
        'text-foreground-600 hover:border-primary-300 hover:text-primary-600 transition-colors';

    var els = {};

    /* 문의 목록 상태. 서버 페이징을 그대로 따라간다(더 보기 방식). */
    var qna = {
        page: 0,
        last: true,
        total: 0,
        items: [],
        loading: false,
        editingId: null   // null 이면 신규 작성, 값이 있으면 그 문의를 수정 중
    };

    function $(id) { return document.getElementById(id); }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function labelOf(key) {
        for (var i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].key === key) return CATEGORIES[i].label;
        }
        return '';
    }

    /** '2026-08-26T14:03:00' → '2026.08.26 14:03'. 값이 없으면 빈 문자열. */
    function formatDate(iso, withTime) {
        if (!iso) return '';
        var d = new Date(iso);
        if (isNaN(d.getTime())) return '';

        function pad(n) { return (n < 10 ? '0' : '') + n; }

        var date = d.getFullYear() + '.' + pad(d.getMonth() + 1) + '.' + pad(d.getDate());
        return withTime ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date;
    }

    /* ── 탭 ────────────────────────────────────────────────── */
    function showTab(name) {
        var isInquiry = name === 'inquiry';

        els.tabFaq.className = isInquiry ? TAB_OFF : TAB_ON;
        els.tabInquiry.className = isInquiry ? TAB_ON : TAB_OFF;
        els.tabFaq.setAttribute('aria-selected', String(!isInquiry));
        els.tabInquiry.setAttribute('aria-selected', String(isInquiry));
        els.panelFaq.classList.toggle('hidden', isInquiry);
        els.panelInquiry.classList.toggle('hidden', !isInquiry);

        /* 문의 탭은 처음 열 때 한 번만 불러온다. 탭을 왕복할 때마다 재조회하면
           작성 폼을 열어둔 상태가 날아간다. */
        if (isInquiry && qna.page === 0 && !qna.items.length && !qna.loading) loadQnas();
    }

    /* ── FAQ ───────────────────────────────────────────────── */
    var faqCategory = 'all';
    var faqKeyword = '';

    function renderChips() {
        var chips = [{ key: 'all', label: '전체' }].concat(CATEGORIES);

        els.faqChips.textContent = chips.map(function (c) {
            return '<button type="button" class="' + (c.key === faqCategory ? CHIP_ON : CHIP_OFF) + '"' +
                ' data-cat="' + c.key + '">' + escapeHtml(c.label) + '</button>';
        }).join('');
    }

    function faqMatches(item) {
        if (faqCategory !== 'all' && item.cat !== faqCategory) return false;
        if (!faqKeyword) return true;

        var haystack = (item.q + ' ' + item.a + ' ' + labelOf(item.cat)).toLowerCase();
        return haystack.indexOf(faqKeyword) !== -1;
    }

    function renderFaq() {
        var matched = FAQS.filter(faqMatches);

        els.faqEmpty.classList.toggle('hidden', matched.length > 0);
        if (!matched.length) {
            els.faqEmpty.textContent = faqKeyword
                ? '"' + faqKeyword + '" 에 해당하는 질문이 없습니다. 아래에서 1:1 문의를 남겨주세요.'
                : '등록된 질문이 없습니다.';
        }

        els.faqList.innerHTML = matched.map(function (item, index) {
            var linkHtml = item.link
                ? '<a href="' + item.link.href + '" class="inline-flex items-center gap-1 mt-3 text-sm font-semibold' +
                  ' text-primary-600 hover:text-primary-700 transition-colors">' +
                  escapeHtml(item.link.text) + '<i class="ri-arrow-right-s-line"></i></a>'
                : '';

            return '' +
                '<div class="rounded-xl border border-background-200 bg-white overflow-hidden">' +
                '  <button type="button" class="faq-toggle w-full flex items-start gap-3 px-4 py-4 text-left' +
                '          hover:bg-background-100/50 transition-colors" aria-expanded="false"' +
                '          aria-controls="faq-panel-' + index + '">' +
                '    <span class="shrink-0 mt-0.5 text-primary-500 font-bold text-sm">Q</span>' +
                '    <span class="flex-1 text-sm font-semibold text-foreground-900 break-keep">' + escapeHtml(item.q) + '</span>' +
                '    <i class="acc-chevron shrink-0 ri-arrow-down-s-line text-lg text-foreground-400"></i>' +
                '  </button>' +
                '  <div id="faq-panel-' + index + '" class="hidden px-4 pb-4 pl-11">' +
                '    <p class="text-sm text-foreground-600 leading-relaxed break-keep">' + escapeHtml(item.a) + '</p>' +
                     linkHtml +
                '  </div>' +
                '</div>';
        }).join('');
    }

    /* ── 문의 목록 ─────────────────────────────────────────── */

    /** 답변 여부에 따른 배지. 상태는 서버 enum(WAITING·ANSWERED)을 그대로 쓴다. */
    function statusBadge(status) {
        return status === 'ANSWERED'
            ? '<span class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-bold rounded-full' +
              ' bg-primary-50 text-primary-700"><i class="ri-check-line"></i>답변 완료</span>'
            : '<span class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-bold rounded-full' +
              ' bg-background-200 text-foreground-600"><i class="ri-time-line"></i>답변 대기</span>';
    }

    function qnaItemHTML(item, index) {
        var answered = item.status === 'ANSWERED';

        /* 수정·삭제는 답변 대기 상태에서만 보여준다. 서버도 답변 완료 건을 거절하므로,
           눌러보고 실패하는 버튼을 두지 않는다. */
        var actions = answered ? '' : '' +
            '<div class="flex items-center gap-2 mt-4 pt-4 border-t border-background-200">' +
            '  <button type="button" class="qna-edit-btn flex items-center gap-1.5 px-4 py-2 text-xs font-semibold' +
            '          text-foreground-700 bg-white border border-background-200 rounded-lg' +
            '          hover:bg-background-100 transition-colors" data-id="' + item.qnaId + '">' +
            '    <i class="ri-pencil-line"></i>수정</button>' +
            '  <button type="button" class="qna-delete-btn flex items-center gap-1.5 px-4 py-2 text-xs font-semibold' +
            '          text-red-500 bg-white border border-background-200 rounded-lg' +
            '          hover:bg-red-50 hover:border-red-200 transition-colors" data-id="' + item.qnaId + '">' +
            '    <i class="ri-delete-bin-line"></i>삭제</button>' +
            '</div>';

        var answerBlock = answered
            ? '<div class="mt-4 rounded-xl bg-primary-50/70 border border-primary-100 px-4 py-3.5">' +
              '  <div class="flex items-center gap-2 mb-2">' +
              '    <span class="text-primary-600 font-bold text-sm">A</span>' +
              '    <span class="text-xs font-semibold text-primary-800">Ounce 답변</span>' +
              '    <span class="ml-auto text-[11px] text-primary-700/70">' + formatDate(item.answeredAt, true) + '</span>' +
              '  </div>' +
              '  <p class="qna-body text-sm text-primary-900/90 leading-relaxed">' + escapeHtml(item.answer) + '</p>' +
              '</div>'
            : '<p class="mt-4 flex items-center gap-1.5 text-xs text-foreground-400">' +
              '  <i class="ri-time-line text-sm"></i>답변을 준비하고 있습니다. 영업일 기준 1일 내에 답변드립니다.</p>';

        var category = item.category
            ? '<span class="px-2 py-0.5 text-[11px] font-medium rounded-full bg-background-100 text-foreground-500">' +
              escapeHtml(item.category) + '</span>'
            : '';

        return '' +
            '<div class="rounded-xl border border-background-200 bg-white overflow-hidden">' +
            '  <button type="button" class="qna-toggle w-full flex items-start gap-3 px-4 py-4 text-left' +
            '          hover:bg-background-100/50 transition-colors" aria-expanded="false"' +
            '          aria-controls="qna-panel-' + index + '">' +
            '    <div class="flex-1 min-w-0">' +
            '      <div class="flex flex-wrap items-center gap-2 mb-1.5">' + statusBadge(item.status) + category +
            '        <span class="text-[11px] text-foreground-400">' + formatDate(item.createdAt) + '</span>' +
            '      </div>' +
            '      <p class="clamp-1 text-sm font-semibold text-foreground-900">' + escapeHtml(item.title) + '</p>' +
            '    </div>' +
            '    <i class="acc-chevron shrink-0 mt-1 ri-arrow-down-s-line text-lg text-foreground-400"></i>' +
            '  </button>' +
            '  <div id="qna-panel-' + index + '" class="hidden px-4 pb-4">' +
            '    <p class="qna-body text-sm text-foreground-600 leading-relaxed">' + escapeHtml(item.content) + '</p>' +
                 answerBlock + actions +
            '  </div>' +
            '</div>';
    }

    function renderQnas() {
        els.qnaList.innerHTML = qna.items.map(qnaItemHTML).join('');

        var empty = qna.items.length === 0;
        els.qnaStatus.classList.toggle('hidden', !empty);
        if (empty) els.qnaStatus.textContent = '아직 남긴 문의가 없습니다. 위의 "문의 남기기"로 접수해 주세요.';

        els.moreBtn.classList.toggle('hidden', qna.last);

        /* 탭 옆 개수 배지. 0건일 때 '0' 을 띄우면 오류처럼 보이므로 숨긴다. */
        els.count.textContent = qna.total > 99 ? '99+' : qna.total;
        els.count.classList.toggle('hidden', qna.total === 0);
    }

    function showGuest() {
        els.guest.classList.remove('hidden');
        els.member.classList.add('hidden');
    }

    function loadQnas() {
        if (qna.loading) return;
        qna.loading = true;
        els.moreBtn.disabled = true;

        if (!qna.items.length) {
            els.qnaStatus.classList.remove('hidden');
            els.qnaStatus.textContent = '문의를 불러오는 중…';
        }

        fetch('/api/qna/me?page=' + qna.page + '&size=' + PAGE_SIZE, { credentials: 'include' })
            .then(function (res) {
                /* 401·403 은 로그인이 풀린 상태다. 페이지 자체는 비로그인도 볼 수 있게
                   열어뒀으니(FAQ), 여기서만 로그인 안내로 갈아낀다. */
                if (res.status === 401 || res.status === 403) {
                    showGuest();
                    return null;
                }
                if (!res.ok) throw new Error('문의 조회 실패: ' + res.status);
                return res.json();
            })
            .then(function (page) {
                if (!page) return;

                els.guest.classList.add('hidden');
                els.member.classList.remove('hidden');

                qna.items = qna.items.concat(page.content || []);
                qna.total = typeof page.totalElements === 'number' ? page.totalElements : qna.items.length;
                qna.last = page.last !== false;
                qna.page += 1;
                renderQnas();
            })
            .catch(function () {
                els.qnaStatus.classList.remove('hidden');
                els.qnaStatus.textContent = '문의를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
            })
            .finally(function () {
                qna.loading = false;
                els.moreBtn.disabled = false;
            });
    }

    /** 목록을 처음부터 다시 읽는다. 등록·수정·삭제 후 상태를 서버 기준으로 맞추기 위해. */
    function reloadQnas() {
        qna.page = 0;
        qna.items = [];
        qna.last = true;
        els.qnaList.innerHTML = '';
        loadQnas();
    }

    /* ── 작성·수정 폼 ──────────────────────────────────────── */
    function renderCategoryOptions() {
        els.category.innerHTML = CATEGORIES.map(function (c) {
            return '<option value="' + escapeHtml(c.label) + '">' + escapeHtml(c.label) + '</option>';
        }).join('');
    }

    function setFormError(message) {
        els.error.classList.toggle('hidden', !message);
        if (message) els.error.querySelector('span').textContent = message;
    }

    function updateCounter() {
        els.counter.textContent = els.content.value.length;
    }

    function openCompose(item) {
        qna.editingId = item ? item.qnaId : null;

        els.composeTitle.textContent = item ? '문의 수정' : '문의 남기기';
        els.composeHint.textContent = item
            ? '답변이 등록되기 전까지 내용을 고칠 수 있습니다.'
            : '주문 관련 문의는 주문번호를 함께 적어주시면 더 빠릅니다.';
        els.submitBtn.textContent = item ? '수정 저장' : '문의 등록';

        els.subject.value = item ? item.title : '';
        els.content.value = item ? item.content : '';
        els.category.value = item && item.category ? item.category : CATEGORIES[0].label;
        /* 예전에 다른 유형 이름으로 저장된 문의는 select 에 없는 값이라 빈칸이 된다.
           그때는 목록의 첫 유형으로 떨어뜨려 저장이 막히지 않게 한다. */
        if (!els.category.value) els.category.value = CATEGORIES[0].label;

        setFormError('');
        updateCounter();
        els.form.classList.remove('hidden');
        els.form.scrollIntoView({ behavior: 'smooth', block: 'center' });
        els.subject.focus();
    }

    function closeCompose() {
        els.form.classList.add('hidden');
        qna.editingId = null;
        setFormError('');
    }

    function submitCompose(e) {
        e.preventDefault();

        var title = els.subject.value.trim();
        var content = els.content.value.trim();

        /* 서버도 @NotBlank 로 막지만, 왕복 없이 바로 알려주는 게 낫다. */
        if (!title) { setFormError('문의 제목을 입력해 주세요.'); els.subject.focus(); return; }
        if (!content) { setFormError('문의 내용을 입력해 주세요.'); els.content.focus(); return; }

        setFormError('');
        els.submitBtn.disabled = true;

        var editing = qna.editingId != null;
        var body = JSON.stringify({ title: title, content: content, category: els.category.value });

        fetch(editing ? '/api/qna/' + qna.editingId : '/api/qna', {
            method: editing ? 'PATCH' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: body
        })
            .then(function (res) {
                if (res.status === 401 || res.status === 403) { showGuest(); return; }
                if (!res.ok) {
                    /* 답변이 이미 달렸다거나 검증에 걸린 경우. 서버 메시지를 그대로 보여주는 게
                       "실패했습니다" 보다 훨씬 쓸모 있다. */
                    return res.json().then(function (err) {
                        throw new Error(err && err.message ? err.message : '요청을 처리하지 못했습니다.');
                    }, function () {
                        throw new Error('요청을 처리하지 못했습니다.');
                    });
                }

                closeCompose();
                ui.toast(editing ? '문의를 수정했습니다.' : '문의를 접수했습니다.');
                reloadQnas();
            })
            .catch(function (err) {
                setFormError(err.message || '요청을 처리하지 못했습니다.');
            })
            .finally(function () {
                els.submitBtn.disabled = false;
            });
    }

    function deleteQna(qnaId) {
        if (!confirm('이 문의를 삭제할까요?\n삭제한 문의는 되돌릴 수 없습니다.')) return;

        fetch('/api/qna/' + qnaId, { method: 'DELETE', credentials: 'include' })
            .then(function (res) {
                if (res.status === 401 || res.status === 403) { showGuest(); return; }
                if (!res.ok) throw new Error('삭제 실패');

                /* 수정 중이던 문의를 지웠으면 폼도 닫는다 */
                if (String(qna.editingId) === String(qnaId)) closeCompose();

                ui.toast('문의를 삭제했습니다.');
                reloadQnas();
            })
            .catch(function () {
                ui.toast('문의를 삭제하지 못했습니다.', 'error');
            });
    }

    /* ── 아코디언 ──────────────────────────────────────────── */
    function toggleAccordion(button) {
        var panel = document.getElementById(button.getAttribute('aria-controls'));
        if (!panel) return;

        var open = button.getAttribute('aria-expanded') === 'true';
        button.setAttribute('aria-expanded', String(!open));
        panel.classList.toggle('hidden', open);
    }

    /* ── 배선 ──────────────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', function () {
        els = {
            tabFaq: $('tab-faq'),
            tabInquiry: $('tab-inquiry'),
            panelFaq: $('panel-faq'),
            panelInquiry: $('panel-inquiry'),
            count: $('inquiry-count'),

            faqChips: $('faq-chips'),
            faqList: $('faq-list'),
            faqEmpty: $('faq-empty'),
            search: $('faq-search'),
            searchClear: $('faq-search-clear'),

            guest: $('inquiry-guest'),
            member: $('inquiry-member'),
            form: $('compose-form'),
            composeTitle: $('compose-title'),
            composeHint: $('compose-hint'),
            category: $('compose-category'),
            subject: $('compose-subject'),
            content: $('compose-content'),
            counter: $('compose-counter'),
            error: $('compose-error'),
            submitBtn: $('compose-submit-btn'),
            qnaList: $('qna-list'),
            qnaStatus: $('qna-status'),
            moreBtn: $('qna-more-btn')
        };

        renderChips();
        renderFaq();
        renderCategoryOptions();

        els.tabFaq.addEventListener('click', function () {
            history.replaceState(null, '', location.pathname);
            showTab('faq');
        });
        els.tabInquiry.addEventListener('click', function () {
            history.replaceState(null, '', '#inquiry');
            showTab('inquiry');
        });
        $('go-inquiry-btn').addEventListener('click', function () {
            history.replaceState(null, '', '#inquiry');
            showTab('inquiry');
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });

        els.faqChips.addEventListener('click', function (e) {
            var chip = e.target.closest('[data-cat]');
            if (!chip) return;
            faqCategory = chip.dataset.cat;
            renderChips();
            renderFaq();
        });

        els.faqList.addEventListener('click', function (e) {
            var button = e.target.closest('.faq-toggle');
            if (button) toggleAccordion(button);
        });

        els.search.addEventListener('input', function () {
            faqKeyword = els.search.value.trim().toLowerCase();
            els.searchClear.classList.toggle('hidden', !faqKeyword);
            renderFaq();
        });
        els.searchClear.addEventListener('click', function () {
            els.search.value = '';
            faqKeyword = '';
            els.searchClear.classList.add('hidden');
            renderFaq();
            els.search.focus();
        });

        $('compose-open-btn').addEventListener('click', function () { openCompose(null); });
        $('compose-close-btn').addEventListener('click', closeCompose);
        $('compose-cancel-btn').addEventListener('click', closeCompose);
        els.form.addEventListener('submit', submitCompose);
        els.content.addEventListener('input', updateCounter);

        els.qnaList.addEventListener('click', function (e) {
            var edit = e.target.closest('.qna-edit-btn');
            if (edit) {
                var found = qna.items.filter(function (item) {
                    return String(item.qnaId) === edit.dataset.id;
                })[0];
                if (found) openCompose(found);
                return;
            }

            var del = e.target.closest('.qna-delete-btn');
            if (del) { deleteQna(del.dataset.id); return; }

            var toggle = e.target.closest('.qna-toggle');
            if (toggle) toggleAccordion(toggle);
        });

        els.moreBtn.addEventListener('click', loadQnas);

        showTab(location.hash === '#inquiry' ? 'inquiry' : 'faq');
    });
})();
