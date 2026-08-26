
const ui = {
    toast: function(message, type = 'success') {
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
};


const signupForm = document.getElementById('signupForm');

if (signupForm) {
    signupForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        // 1. 에러 초기화
        document.querySelectorAll('.error-text').forEach(el => el.textContent = '');
        document.querySelectorAll('input').forEach(input => input.classList.remove('error-input'));

        // 2. 값 가져오기
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        const name = document.getElementById('name').value; // 💡 이름 값 가져오기

        if (!name) {
            ui.toast("이름을 입력해주세요.");
            document.getElementById('name').focus();
            return; // 서버로 안 보내고 중단
        }

        if (!email) {
            ui.toast("이메일을 입력해주세요.");
            document.getElementById('email').focus();
            return;
        }
        if (!password) {
            ui.toast("비밀번호를 입력해주세요.");
            document.getElementById('password').focus();
            return;
        }
        if (!confirmPassword) {
            ui.toast("비밀번호 확인을 입력해주세요.");
            document.getElementById('confirmPassword').focus();
            return;
        }


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
                    password: password,
                    name: name
                })
            });

            if (response.ok || response.status === 201) {
                ui.toast("Ounce 가입을 환영합니다! 축하 포인트 1,000P가 지급되었어요 🎁");

                setTimeout(() => {
                    window.location.href = '/login?welcome=true';
                }, 1000);
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
}

document.querySelectorAll('.password-toggle').forEach(function (btn) {
    btn.addEventListener('click', function () {
        const input = btn.parentElement.querySelector('input');
        const icon = btn.querySelector('i');
        const isHidden = input.type === 'password';

        input.type = isHidden ? 'text' : 'password';
        icon.classList.toggle('ri-eye-line', !isHidden);
        icon.classList.toggle('ri-eye-off-line', isHidden);
    });
});