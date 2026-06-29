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

