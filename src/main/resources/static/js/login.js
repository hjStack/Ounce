
const loginForm = document.getElementById('loginForm');

if (loginForm) {
    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch('/api/members/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                // 💡 이게 없으면 브라우저가 백엔드에서 주는 쿠키를 튕겨냅니다!
                credentials: 'include',
                body: JSON.stringify({ email: email, password: password })
            });

            if (response.ok) {
                alert("오늘도 맛있는 하루! Ounce와 함께 열어볼까요?");
                window.location.href = '/';
            } else {
                alert("아직 Ounce의 문이 열리지 않았어요! 이메일과 비밀번호를 다시 한번 확인해 볼까요?");
            }
        } catch (error) {
            console.error("서버 통신 실패:", error);
            alert("서버와 통신 중 문제가 발생했습니다.");
        }
    });
}

window.addEventListener('DOMContentLoaded', function () {
    const ua = navigator.userAgent.toLowerCase();
    const isKakao = ua.includes('kakaotalk');
    const isInApp =
        isKakao ||
        ua.includes('instagram') ||
        ua.includes('line') ||
        ua.includes('fban') ||
        ua.includes('fbav') ||
        ua.includes('naver');

    if (!isInApp) return;

    const targetUrl = 'https://ouncefresh.com/login';

    if (isKakao) {
        location.href =
            'kakaotalk://web/openExternal?url=' + encodeURIComponent(targetUrl);
        return;
    }

    document.body.innerHTML = `
    <div style="padding:24px; text-align:center; font-family:sans-serif;">
      <h2>외부 브라우저로 열어주세요</h2>
      <p>구글 로그인은 앱 내 브라우저에서 지원되지 않습니다.</p>
      <p>오른쪽 위 메뉴(⋮)를 눌러<br>
      <b>'다른 브라우저로 열기'</b> 또는 <b>'Chrome으로 열기'</b>를<br>
      선택해 주세요.</p>
    </div>`;
});

/*
todo
 - 비밀번호 일치 하지 않을 시 메인화면으로 리다이렉팅 되는 문제
 - 비밀번호가 틀렸다면 비밀번호를 다시 입력해주세요 띄우기
 - 이메일을 입력하지 않을시 이메일을 입력해주세요 띄우기
 - 이메일이 틀렸다면 검증해서 이메일을 확인해주세요 띄우기
 - 비밀번호가 틀렸다면 비밀번호를 다시 확인해주세요 띄우기
 - 마이페이지에서 회원 탈퇴 버튼 만들기
 */