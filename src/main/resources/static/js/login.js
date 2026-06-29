
const loginForm = document.getElementById('loginForm');

if (loginForm) {
    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch('/api/members/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email, password })
            });

            if (response.ok) {
                alert("Ounce에 오신 것을 환영합니다!");
                window.location.href = '/';
            } else {
                alert("이메일이나 비밀번호를 다시 확인해주세요.🥺");
            }
        } catch (error) {
            console.error("서버 통신 실패:", error);
            alert("서버와 통신 중 문제가 발생했습니다.");
        }
    });
}