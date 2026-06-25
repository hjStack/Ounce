
const loginForm = document.getElementById('loginForm');

if (loginForm) { // 로그인 화면에만 이 코드가 작동하도록 안전장치!
    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault(); // 새로고침 방지

        // 1. 값 가져오기
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        try {
            // 2. 백엔드 API 호출! (/api/members/login)
            const response = await fetch('/api/members/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    email: email,
                    password: password
                })
            });

            if (response.ok) {
                // 3. 백엔드가 준 JWT 토큰 꺼내기! (헤더에서 꺼냅니다)
                const token = response.headers.get('Authorization');

                if (token) {
                    // 성공하면 일단 로컬 스토리지에 토큰을 저장하고 메인으로 이동!
                    localStorage.setItem('accessToken', token);
                    alert("Ounce에 오신것을 환영합니다 !");
                    window.location.href = '/'; // 메인 페이지로 이동
                } else {
                    alert("토큰 실패");
                }
            } else {
                alert("이메일이나 비밀번호를 다시 확인해주세요.🥺");
            }
        } catch (error) {
            console.error("서버 통신 실패:", error);
            alert("서버와 통신 중 문제가 발생했습니다.");
        }
    });
}


