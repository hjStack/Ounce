
document.getElementById('signupForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    // 1. 에러 초기화
    document.querySelectorAll('.error-text').forEach(el => el.textContent = '');
    document.querySelectorAll('input').forEach(input => input.classList.remove('error-input'));

    // 2. 값 가져오기
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    // 3. 프론트엔드 자체 검증 (비밀번호 확인)
    if (password !== confirmPassword) {
        document.getElementById('confirmPasswordError').textContent = "비밀번호가 일치하지 않습니다.";
        document.getElementById('confirmPassword').classList.add('error-input');
        return; // 서버로 안 보내고 중단
    }

    try {
        // 4. 백엔드 API 호출! (DTO 구조에 맞게 JSON 생성)
        const response = await fetch('/api/members/signup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        if (response.ok || response.status === 201) {
            alert("가입이 완료되었습니다!");
            window.location.href = '/login';
        }
        else if (response.status === 400 || response.status === 409) {
            // 우리가 백엔드에서 만든 예쁜 ErrorResponse 낚아채기
            const errorData = await response.json();
            const errorMessage = errorData.message;

            // 에러 내용에 따라 UI 표시
            if (errorMessage.includes('비밀번호')) {
                document.getElementById('passwordError').textContent = errorMessage;
                document.getElementById('password').classList.add('error-input');
            }
            else if (errorMessage.includes('이메일')) {
                document.getElementById('emailError').textContent = errorMessage;
                document.getElementById('email').classList.add('error-input');
            }
            else {
                alert(errorMessage);
            }
        }
    } catch (error) {
        console.error("서버 통신 실패:", error);
    }
});

// ====== 로그인 폼 처리 ======

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
                    alert("로그인 성공! 환영합니다.");
                    window.location.href = '/'; // 메인 페이지로 이동
                } else {
                    alert("토큰 실패");
                }
            } else {
                alert("로그인 실패: 이메일이나 비밀번호를 확인해주세요.");
            }
        } catch (error) {
            console.error("서버 통신 실패:", error);
            alert("서버와 통신 중 문제가 발생했습니다.");
        }
    });
}
