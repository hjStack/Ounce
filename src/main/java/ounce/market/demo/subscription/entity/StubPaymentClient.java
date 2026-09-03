package ounce.market.demo.subscription.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ounce.market.demo.subscription.entity.PaymentClient;

import java.util.UUID;

/**
 * 개발용 스텁. 실제 PG를 붙이기 전까지 배치 흐름을 돌려보기 위한 것이다.
 * <p>
 * @Profile("!prod")를 반드시 유지해야 한다. 이게 운영에 올라가면
 * 아무한테도 돈을 안 받고 전원에게 밀키트를 배송하게 된다.
 * <p>
 * 실패 경로(재시도 3회, 3주 연속 실패 해지)를 테스트하려면
 * failureRate를 올리거나 특정 memberId만 실패하도록 조건을 넣으면 된다.
 */
@Slf4j
@Component
@Profile("!prod")
public class StubPaymentClient implements PaymentClient {


    @Override
    public PaymentResult charge(String idempotencyKey, Long memberId, long amount, String orderName) {
        log.warn("스텁 결제 사용 중. 실제 청구되지 않음. key={} memberId={} amount={}",
                idempotencyKey, memberId, amount);

        // 실패 시나리오를 확인하려면 아래 주석을 켜서 특정 회원만 실패시킨다.
        if (memberId % 10 == 3) {
            return PaymentResult.retryableFailure("INSUFFICIENT_BALANCE");
        }

        return PaymentResult.success("stub-" + UUID.randomUUID());
    }
}