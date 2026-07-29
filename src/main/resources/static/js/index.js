// js/index.js
document.addEventListener("DOMContentLoaded", () => {
    checkLoginStatus();
});

function checkLoginStatus() {
    fetch('/api/members/me', {
        method: 'GET',
        credentials: 'include' // 💡 브라우저야, 아까 받은 쿠키 같이 보내!
    })
        .then(response => {
            if (response.ok) return response.json();
            throw new Error('비회원 상태');
        })
        .then(userData => {
            updateUIForLoggedIn(userData.name);
        })
        .catch(error => {
            console.log("현재 비회원 상태입니다.");
        });
}

function updateUIForLoggedIn(userName) {
    const loginBtn = document.getElementById("loginBtn");
    const signupBtn = document.getElementById("signupBtn");
    const userGreeting = document.getElementById("userGreeting");

    if (loginBtn) {
        loginBtn.innerText = "로그아웃";
        loginBtn.href = "#";
        loginBtn.onclick = () => {
            alert("로그아웃 되었습니다.");
            // 실제 배포 시에는 쿠키 삭제 API 호출 필요
            location.reload();
        };
    }
    if (signupBtn) signupBtn.style.display = "none";
    if (userGreeting) userGreeting.innerText = `${userName}님, 환영합니다! 🌿`;
}

document.addEventListener('DOMContentLoaded', function() {
    function updateMidnightTimer() {
        // 1. 화면에 타이머 태그가 있는지 먼저 찾습니다.
        const hoursEl = document.getElementById('td-hours');
        const minutesEl = document.getElementById('td-minutes');
        const secondsEl = document.getElementById('td-seconds');

        // 2. 태그가 하나라도 없으면(메인 페이지가 아니면) 여기서 함수를 종료합니다!
        if (!hoursEl || !minutesEl || !secondsEl) {
            return;
        }

        const now = new Date();
        const midnight = new Date();

        // 오늘 밤 12시(자정)로 설정
        midnight.setHours(24, 0, 0, 0);

        const diff = midnight - now;

        if (diff > 0) {
            const h = Math.floor((diff / (1000 * 60 * 60)) % 24).toString().padStart(2, '0');
            const m = Math.floor((diff / 1000 / 60) % 60).toString().padStart(2, '0');
            const s = Math.floor((diff / 1000) % 60).toString().padStart(2, '0');

            // 3. 태그가 있을 때만 텍스트를 변경합니다.
            hoursEl.textContent = h;
            minutesEl.textContent = m;
            secondsEl.textContent = s;
        }
    }

    // 1초마다 업데이트
    setInterval(updateMidnightTimer, 1000);
    updateMidnightTimer(); // 로드 시 즉시 실행
});