async function handleCheckout() {
    const selectedProductIds = [1, 2];

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // 🚨 핵심: 'Authorization': `Bearer ${token}` 코드를 삭제합니다!
            },

            // 💡 중요: 프론트와 백엔드의 도메인이나 포트가 다르다면(CORS 환경),
            // 브라우저가 쿠키를 챙겨가도록 반드시 이 옵션을 켜줘야 합니다!
            credentials: 'include',

            body: JSON.stringify({ cartProductIds: selectedProductIds })
        });

        if (response.status === 201) {
            const orderId = await response.text();
            alert(`결제가 완료되었습니다! (주문번호: ${orderId})`);
            window.location.href = `/orders/${orderId}`;
        } else {
            const errorMsg = await response.text();
            alert('결제 실패: ' + errorMsg);
        }
    } catch (error) {
        console.error('결제 에러:', error);
    }
}