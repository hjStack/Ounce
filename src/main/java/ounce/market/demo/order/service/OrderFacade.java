package ounce.market.demo.order.service;

import lombok.RequiredArgsConstructor;
import ounce.market.demo.order.entity.Order;
import ounce.market.demo.product.exception.OutOfStockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

// 10시 정각 트래픽 스파이크에서 같은 Stock row를 동시에 두드릴 때, 동시성 충돌은
// "진짜 품절"이 아니라 "남이 먼저 가져간 것뿐"일 수 있다. 그래서 충돌 시 최신 상태로 다시 읽어
// 재시도하고, 정말 수량이 없을 때만 OutOfStockException으로 확정한다.
// ConcurrencyFailureException은 낙관적 락 충돌(OptimisticLockingFailureException)뿐 아니라
// 고동시성에서 가끔 발생하는 DB 행 잠금 대기/데드락(CannotAcquireLockException)도 포괄하는
// 공통 부모 타입 - 둘 다 "잠깐의 경쟁일 뿐 진짜 품절은 아닐 수 있는" 일시적 실패라 같이 재시도해야 한다.
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private static final int MAX_RETRY = 100;

    // 재시도마다 새 트랜잭션(=새 DB 커넥션)을 잡기 때문에, 동시 요청이 몰리면 재시도 폭주가
    // HikariCP 풀을 고갈시켜 "진짜 품절"이 아닌 커넥션 타임아웃으로 실패하는 경우가 생긴다
    // (이 예외는 OptimisticLockingFailureException이 아니라 재시도되지 않고 그대로 실패해버림).
    // 동시 DB 시도 수를 풀 크기보다 작게 제한해 그런 거짓 실패를 막는다.
    private static final int MAX_CONCURRENT_DB_ATTEMPTS = 8;

    private final OrderService orderService;
    private final Semaphore dbAttemptLimiter = new Semaphore(MAX_CONCURRENT_DB_ATTEMPTS);

    public Order purchase(Long memberId, Long productId, int quantity) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            acquire();
            try {
                return orderService.placeOrder(memberId, productId, quantity);
            } catch (ConcurrencyFailureException e) {
                if (attempt == MAX_RETRY) {
                    throw new OutOfStockException(productId);
                }
                // 락 충돌은 "내가 졌다"는 뜻일 뿐 품절은 아닐 수 있음 -> 살짝 대기 후 최신 상태로 재시도
                sleepJitter();
            } finally {
                dbAttemptLimiter.release();
            }
        }
        throw new OutOfStockException(productId);
    }

    private void acquire() {
        try {
            dbAttemptLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("구매 처리 중 인터럽트되었습니다.", e);
        }
    }

    private void sleepJitter() {
        try {
            Thread.sleep((long) (Math.random() * 10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
