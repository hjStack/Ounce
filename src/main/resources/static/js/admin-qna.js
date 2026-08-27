/* /admin/qna — 관리자 문의 관리. 고객이 /support 에서 남긴 1:1 문의에 답변하는 화면.
 *
 * 이 화면의 목적은 "밀린 문의를 빠짐없이 처리하는 것" 하나다. 그래서 고객 화면과
 * 반대로 설계했다:
 *   · 기본 필터가 '답변 대기' — 처리할 일부터 보여준다.
 *   · 1일 초과 대기 건수를 맨 위에 세운다 — 고객에게 "영업일 1일 내 답변"을 약속했으니
 *     그 약속을 깨고 있는 건수가 이 화면에서 가장 중요한 숫자다.
 *   · 목록은 최신순 그대로 두되(서버가 qnaId DESC 로 고정), 오래된 대기 건은
 *     행마다 "N일 경과" 배지로 표시해서 순서와 무관하게 눈에 걸리게 했다.
 *
 * API:
 *   GET  /api/admin/qna?status=WAITING&page&size   전체 문의 (status 생략 = 전부)
 *   POST /api/admin/qna/{id}/answer  { answer }    답변 등록 → 상태가 ANSWERED 로 바뛴다
 *
 * ⚠ 답변 수정·삭제 API 는 없다. 한 번 등록하면 고객 화면에 그대로 나가므로
 * 등록 전에 확인 창을 한 번 띄운다.
 * ⚠ /api/admin/** 은 ADMIN 권한이 필요하다(SecurityConfig). 403 이면 권한 안내로 바뛴다.
 */
(function () {
    'use strict';

    var ui = window.Ounce;

    var PAGE_SIZE = 10;
    var STATS_SCAN = 100;          // 경과 일수 계산을 위해 훑는 대기 건 수 상한
    var SLA_HOURS = 24;            // 이 시간을 넘긴 대기 건을 '초과'로 센다

    var STATUSES = [
        { key: 'WAITING', label: '답변 대기' },
        { key: 'ANSWERED', label: '답변 완료' },
        { key: 'ALL', label: '전체' }
    ];

    /* 자주 쓰는 답변. 그대로 보내는 용도가 아니라 첫 문장을 대신 쓰는 용도다.
     * (확정되지 않은 정책을 단정하는 문구는 넣지 않는다) */
    var TEMPLATES = [
        {
            label: '배송 지연',
            text: '문의 주신 주문의 배송 상태를 확인했습니다.\n\n' +
                '현재 배송이 지연되어 도착이 늦어지고 있습니다. 확인되는 도착 예정 시점은 ' +
                '(  )이며, 진행 상황은 주문 내역에서도 확인하실 수 있습니다.\n\n불편을 드려 죄송합니다.'
        },
        {
            label: '상품 상태 확인',
            text: '알려주신 상품 상태를 확인했습니다.\n\n' +
                '보내주신 내용 기준으로 (  ) 처리해 드리겠습니다. 추가로 확인이 필요한 사항이 있으면 ' +
                '이 문의에 이어서 남겨주세요.\n\n불편을 드려 죄송합니다.'
        },
        {
            label: '주문 취소·변경',
            text: '문의 주신 주문 건을 확인했습니다.\n\n' +
                '주문 마감(밤 23:00) 전 건은 주문 내역에서 직접 취소하실 수 있고, ' +
                '마감이 지난 건은 확인 후 개별로 안내드립니다. 해당 주문은 (  ) 상태입니다.'
        },
        {
            label: '확인 후 회신',
            text: '문의 주셔서 감사합니다.\n\n' +
                '말씀 주신 내용은 담당 확인이 필요해 조금 더 시간이 걸립니다. ' +
                '확인되는 즉시 이 문의에 답변으로 알려드리겠습니다.'
        }
    ];

    var TAB_ON = 'px-4 py-2 text-sm font-semibold rounded-lg bg-white text-foreground-950 shadow-sm transition-colors';
    var TAB_OFF = 'px-4 py-2 text-sm font-medium rounded-lg text-foreground-500 hover:text-foreground-800 transition-colors';

    var els = {};
    var state = {
        status: 'WAITING',
        page: 0,
        totalPages: 1,
        items: [],
        keyword: '',
        loading: false,
        openId: null      // 펼쳐둔 문의. 새로고침·답변 후에도 같은 건을 계속 보게 유지한다
    };

    function $(id) { return document.getElementById(id); }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function formatDate(iso, withTime) {
        if (!iso) return '';
        var d = new Date(iso);
        if (isNaN(d.getTime())) return '';

        function pad(n) { return (n < 10 ? '0' : '') + n; }

        var date = d.getFullYear() + '.' + pad(d.getMonth() + 1) + '.' + pad(d.getDate());
        return withTime ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date;
    }

    /** 접수 후 지난 시간(시간 단위). 값이 이상하면 0. */
    function hoursSince(iso) {
        var d = new Date(iso);
        if (isNaN(d.getTime())) return 0;
        return (Date.now() - d.getTime()) / 3600000;
    }

    function query(status, page, size) {
        var params = 'page=' + page + '&size=' + size;
        return '/api/admin/qna?' + (status && status !== 'ALL' ? 'status=' + status + '&' : '') + params;
    }

    /* ── 권한 ──────────────────────────────────────────────── */
    function showForbidden() {
        els.list.textContent = '';
        els.pager.classList.add('hidden');
        els.status.classList.remove('hidden');
        els.status.textContent = '관리자 권한이 필요한 화면입니다. ' +
            'ADMIN 계정으로 <a href="/login" class="font-semibold text-primary-600 underline">로그인</a> 후 다시 열어주세요.';
    }

    /* ── 상단 현황 ─────────────────────────────────────────── */
    function loadStats() {
        /* 대기 건은 경과 일수를 세야 해서 목록까지 받아온다. 100건을 넘으면
           초과 건수에 '+' 를 붙여 실제로는 더 많을 수 있다는 걸 숨기지 않는다. */
        fetch(query('WAITING', 0, STATS_SCAN), { credentials: 'include' })
            .then(function (res) {
                if (res.status === 401 || res.status === 403) { showForbidden(); return null; }
                if (!res.ok) throw new Error('현황 조회 실패');
                return res.json();
            })
            .then(function (page) {
                if (!page) return;

                var items = page.content || [];
                var total = typeof page.totalElements === 'number' ? page.totalElements : items.length;
                var overdue = items.filter(function (item) { return hoursSince(item.createdAt) > SLA_HOURS; }).length;
                var capped = total > items.length;

                els.statWaiting.textContent = total;
                els.statOverdue.textContent = overdue + (capped ? '+' : '');

                /* 초과 건이 있으면 카드 색을 바꾼다. 숫자만 바뛰면 안 본다. */
                els.overdueCard.className = overdue > 0
                    ? 'rounded-xl border border-red-200 bg-red-50 p-4'
                    : 'rounded-xl border border-background-300 bg-white p-4';
                els.statOverdue.className = overdue > 0
                    ? 'text-xl font-bold leading-none text-red-600 tabular-nums'
                    : 'text-xl font-bold leading-none text-foreground-950 tabular-nums';
            })
            .catch(function () { /* 현황은 보조 정보다. 실패해도 목록은 그대로 쓴다 */ });

        fetch(query('ANSWERED', 0, 1), { credentials: 'include' })
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function (page) {
                if (page) els.statAnswered.textContent = page.totalElements;
            })
            .catch(function () { /* 위와 같음 */ });
    }

    /* ── 목록 ──────────────────────────────────────────────── */
    function renderTabs() {
        els.tabs.innerHTML = STATUSES.map(function (s) {
            return '<button type="button" data-status="' + s.key + '"' +
                ' class="' + (s.key === state.status ? TAB_ON : TAB_OFF) + '">' + s.label + '</button>';
        }).join('');
    }

    function statusBadge(status) {
        return status === 'ANSWERED'
            ? '<span class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-bold rounded-full' +
              ' bg-primary-50 text-primary-700"><i class="ri-check-line"></i>답변 완료</span>'
            : '<span class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-bold rounded-full' +
              ' bg-deal-100 text-deal-700"><i class="ri-time-line"></i>답변 대기</span>';
    }

    /** 대기 건이 SLA(1일)를 넘겼을 때만 붙는 경과 배지. */
    function elapsedBadge(item) {
        if (item.status === 'ANSWERED') return '';

        var hours = hoursSince(item.createdAt);
        if (hours <= SLA_HOURS) return '';

        var days = Math.floor(hours / 24);
        var label = days >= 1 ? days + '일 경과' : Math.floor(hours) + '시간 경과';
        return '<span class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-bold rounded-full' +
            ' bg-red-100 text-red-700"><i class="ri-alarm-warning-line"></i>' + label + '</span>';
    }

    function answerFormHTML(item) {
        var templates = TEMPLATES.map(function (t, i) {
            return '<button type="button" class="tpl-btn px-3 py-1.5 text-xs font-medium rounded-full bg-white' +
                ' border border-background-300 text-foreground-600 hover:border-primary-300 hover:text-primary-600' +
                ' transition-colors" data-id="' + item.qnaId + '" data-tpl="' + i + '">' +
                escapeHtml(t.label) + '</button>';
        }).join('');

        return '' +
            '<div class="mt-4 pt-4 border-t border-background-200">' +
            '  <div class="flex flex-wrap items-center gap-2 mb-2.5">' +
            '    <span class="text-xs font-semibold text-foreground-500 mr-1">자주 쓰는 답변</span>' + templates +
            '  </div>' +
            '  <label for="answer-' + item.qnaId + '" class="sr-only">답변 내용</label>' +
            '  <textarea id="answer-' + item.qnaId + '" rows="5" maxlength="2000"' +
            '            placeholder="고객에게 그대로 보이는 글입니다. 확인한 사실과 다음 조치를 함께 적어주세요."' +
            '            class="answer-input w-full px-3.5 py-2.5 text-sm text-foreground-950 placeholder-foreground-400' +
            '                   bg-white border border-background-300 rounded-lg resize-y' +
            '                   focus:border-primary-400 focus:outline-none"></textarea>' +
            '  <div class="flex flex-wrap items-center gap-2 mt-3">' +
            '    <button type="button" class="answer-submit-btn px-5 py-2.5 bg-primary-500 text-white text-sm' +
            '            font-semibold rounded-lg hover:bg-primary-600 transition-colors disabled:opacity-40"' +
            '            data-id="' + item.qnaId + '">답변 등록</button>' +
            '    <p class="text-xs text-foreground-400">등록하면 고객 화면에 바로 표시되고, 이후 수정할 수 없습니다.</p>' +
            '  </div>' +
            '  <p class="answer-error hidden mt-2 flex items-center gap-1.5 text-xs text-red-600">' +
            '    <i class="ri-error-warning-line text-sm"></i><span></span></p>' +
            '</div>';
    }

    function answeredHTML(item) {
        return '' +
            '<div class="mt-4 rounded-xl bg-primary-50/70 border border-primary-100 px-4 py-3.5">' +
            '  <div class="flex items-center gap-2 mb-2">' +
            '    <span class="text-primary-600 font-bold text-sm">A</span>' +
            '    <span class="text-xs font-semibold text-primary-800">등록된 답변</span>' +
            '    <span class="ml-auto text-[11px] text-primary-700/70">' + formatDate(item.answeredAt, true) + '</span>' +
            '  </div>' +
            '  <p class="qna-body text-sm text-primary-900/90 leading-relaxed">' + escapeHtml(item.answer) + '</p>' +
            '</div>';
    }

    function itemHTML(item) {
        var open = String(state.openId) === String(item.qnaId);

        var category = item.category
            ? '<span class="px-2 py-0.5 text-[11px] font-medium rounded-full bg-background-100 text-foreground-500">' +
              escapeHtml(item.category) + '</span>'
            : '';

        return '' +
            '<div class="rounded-xl border border-background-300 bg-white overflow-hidden">' +
            '  <button type="button" class="qna-toggle w-full flex items-start gap-3 px-4 py-3.5 text-left' +
            '          hover:bg-background-100/60 transition-colors" aria-expanded="' + open + '"' +
            '          aria-controls="admin-panel-' + item.qnaId + '" data-id="' + item.qnaId + '">' +
            '    <div class="flex-1 min-w-0">' +
            '      <div class="flex flex-wrap items-center gap-2 mb-1.5">' +
                     statusBadge(item.status) + elapsedBadge(item) + category +
            '        <span class="text-[11px] text-foreground-400">' + formatDate(item.createdAt, true) + '</span>' +
            '        <span class="text-[11px] text-foreground-400">·</span>' +
            '        <span class="text-[11px] font-medium text-foreground-600">' +
                     escapeHtml(item.memberName || ('회원 ' + item.memberId)) + '</span>' +
            '        <span class="text-[11px] text-foreground-300">#' + item.qnaId + '</span>' +
            '      </div>' +
            '      <p class="clamp-1 text-sm font-semibold text-foreground-900">' + escapeHtml(item.title) + '</p>' +
            '    </div>' +
            '    <i class="acc-chevron shrink-0 mt-1 ri-arrow-down-s-line text-lg text-foreground-400"></i>' +
            '  </button>' +
            '  <div id="admin-panel-' + item.qnaId + '" class="' + (open ? '' : 'hidden ') + 'px-4 pb-4">' +
            '    <div class="rounded-xl bg-background-100/70 px-4 py-3.5">' +
            '      <p class="qna-body text-sm text-foreground-700 leading-relaxed">' + escapeHtml(item.content) + '</p>' +
            '    </div>' +
                 (item.status === 'ANSWERED' ? answeredHTML(item) : answerFormHTML(item)) +
            '  </div>' +
            '</div>';
    }

    /** 현재 페이지 안에서만 걸러낸다. 서버에 검색 API 가 없어서 전역 검색은 못 한다. */
    function visibleItems() {
        if (!state.keyword) return state.items;

        return state.items.filter(function (item) {
            var haystack = (
                (item.title || '') + ' ' + (item.content || '') + ' ' +
                (item.memberName || '') + ' ' + (item.category || '')
            ).toLowerCase();
            return haystack.indexOf(state.keyword) !== -1;
        });
    }

    function render() {
        var items = visibleItems();

        els.list.innerHTML = items.map(itemHTML).join('');

        var empty = items.length === 0;
        els.status.classList.toggle('hidden', !empty);
        if (empty) {
            els.status.textContent = state.keyword
                ? '이 페이지에서 "' + state.keyword + '" 에 해당하는 문의가 없습니다.'
                : (state.status === 'WAITING' ? '답변 대기 중인 문의가 없습니다.' : '표시할 문의가 없습니다.');
        }

        els.pager.classList.toggle('hidden', state.totalPages <= 1);
        els.pager.classList.toggle('flex', state.totalPages > 1);
        els.pageCurrent.textContent = state.page + 1;
        els.pageTotal.textContent = state.totalPages;
        els.prevBtn.disabled = state.page <= 0;
        els.nextBtn.disabled = state.page >= state.totalPages - 1;
    }

    function load() {
        if (state.loading) return;
        state.loading = true;

        els.list.innerHTML = '';
        els.status.classList.remove('hidden');
        els.status.textContent = '문의를 불러오는 중…';

        fetch(query(state.status, state.page, PAGE_SIZE), { credentials: 'include' })
            .then(function (res) {
                if (res.status === 401 || res.status === 403) { showForbidden(); return null; }
                if (!res.ok) throw new Error('문의 조회 실패: ' + res.status);
                return res.json();
            })
            .then(function (page) {
                if (!page) return;

                state.items = page.content || [];
                state.totalPages = page.totalPages || 1;
                render();
            })
            .catch(function () {
                els.status.classList.remove('hidden');
                els.status.textContent = '문의를 불러오지 못했습니다. 잠시 후 새로고침해 주세요.';
            })
            .finally(function () { state.loading = false; });
    }

    function reload() {
        loadStats();
        load();
    }

    /* ── 답변 등록 ─────────────────────────────────────────── */
    function submitAnswer(qnaId, button) {
        var panel = $('admin-panel-' + qnaId);
        if (!panel) return;

        var input = panel.querySelector('.answer-input');
        var errorBox = panel.querySelector('.answer-error');
        var answer = input.value.trim();

        function setError(message) {
            errorBox.classList.toggle('hidden', !message);
            if (message) errorBox.querySelector('span').textContent = message;
        }

        if (!answer) { setError('답변 내용을 입력해 주세요.'); input.focus(); return; }

        /* 수정 API 가 없어서 오타도 그대로 고객에게 남는다. 한 번 되묻는다. */
        if (!confirm('이 답변을 등록할까요?\n등록하면 고객 화면에 바로 표시되고 수정할 수 없습니다.')) return;

        setError('');
        button.disabled = true;

        fetch('/api/admin/qna/' + qnaId + '/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ answer: answer })
        })
            .then(function (res) {
                if (res.status === 401 || res.status === 403) { showForbidden(); return; }
                if (!res.ok) {
                    return res.json().then(function (err) {
                        throw new Error(err && err.message ? err.message : '답변을 등록하지 못했습니다.');
                    }, function () {
                        throw new Error('답변을 등록하지 못했습니다.');
                    });
                }

                ui.toast('답변을 등록했습니다.');
                /* 대기 목록을 보고 있었다면 이 건은 목록에서 빠진다. 펼친 상태를 놔두면
                   엉뚱한 건이 열리므로 닫고 다시 읽는다. */
                state.openId = state.status === 'WAITING' ? null : qnaId;
                reload();
            })
            .catch(function (err) {
                setError(err.message || '답변을 등록하지 못했습니다.');
            })
            .finally(function () { button.disabled = false; });
    }

    /* ── 배선 ──────────────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', function () {
        els = {
            tabs: $('status-tabs'),
            list: $('qna-list'),
            status: $('qna-status'),
            search: $('admin-search'),
            refresh: $('refresh-btn'),
            pager: $('pager'),
            prevBtn: $('prev-btn'),
            nextBtn: $('next-btn'),
            pageCurrent: $('page-current'),
            pageTotal: $('page-total'),
            statWaiting: $('stat-waiting'),
            statAnswered: $('stat-answered'),
            statOverdue: $('stat-overdue'),
            overdueCard: $('stat-overdue-card')
        };

        renderTabs();

        els.tabs.addEventListener('click', function (e) {
            var tab = e.target.closest('[data-status]');
            if (!tab || tab.dataset.status === state.status) return;

            state.status = tab.dataset.status;
            state.page = 0;
            state.openId = null;
            renderTabs();
            load();
        });

        els.list.addEventListener('click', function (e) {
            var tpl = e.target.closest('.tpl-btn');
            if (tpl) {
                var box = $('answer-' + tpl.dataset.id);
                /* 이미 쓴 글을 상용구로 지워버리지 않는다 */
                if (box && (!box.value.trim() || confirm('작성 중인 내용을 상용구로 바꿀까요?'))) {
                    box.value = TEMPLATES[Number(tpl.dataset.tpl)].text;
                    box.focus();
                }
                return;
            }

            var submit = e.target.closest('.answer-submit-btn');
            if (submit) { submitAnswer(submit.dataset.id, submit); return; }

            var toggle = e.target.closest('.qna-toggle');
            if (toggle) {
                var panel = $(toggle.getAttribute('aria-controls'));
                var open = toggle.getAttribute('aria-expanded') === 'true';

                toggle.setAttribute('aria-expanded', String(!open));
                panel.classList.toggle('hidden', open);
                state.openId = open ? null : toggle.dataset.id;

                if (!open) {
                    var input = panel.querySelector('.answer-input');
                    if (input) input.focus();
                }
            }
        });

        els.search.addEventListener('input', function () {
            state.keyword = els.search.value.trim().toLowerCase();
            render();
        });

        els.refresh.addEventListener('click', reload);
        els.prevBtn.addEventListener('click', function () {
            if (state.page > 0) { state.page -= 1; state.openId = null; load(); }
        });
        els.nextBtn.addEventListener('click', function () {
            if (state.page < state.totalPages - 1) { state.page += 1; state.openId = null; load(); }
        });

        reload();
    });
})();
