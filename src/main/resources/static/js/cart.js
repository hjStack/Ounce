
async function handleCheckout() {
    // 장바구니가 비어 있으면 막기
    if (!cart || cart.length === 0) {
        ui.toast('장바구니가 비어 있습니다.');
        return;
    }

    // 하드코딩 대신, 실제 담긴 장바구니 아이템 ID를 사용
    const selectedProductIds = cart.map(item => item.cartId);

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({ cartProductIds: selectedProductIds })
        });

        if (response.status === 201) {
            const orderId = await response.text();
            ui.toast(`결제가 완료되었습니다! (주문번호: ${orderId})`);
            window.location.href = `/orders/${orderId}`;
        } else if (response.status === 401) {
            ui.confirm('로그인이 필요한 서비스입니다.');
            window.location.href = '/login';
        } else {
            const errorMsg = await response.text();
            ui.confirm('결제 실패: ' + errorMsg);
        }
    } catch (error) {
        console.error('결제 에러:', error);
        ui,confirm('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    }
}