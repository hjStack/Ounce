package ounce.market.demo.delivery.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ounce.shipping")
public class ShippingPolicy {

    // 배송비 3천원, 3만원이상 상시 무료 배송

    private int baseFee = 3_000;
    private int freeThreshold = 30_000;

    /** 오픈 프로모션 종료일. null이면 프로모션 없음 */
    private LocalDate promotionEndDate;

    /** @param productAmount 배송비 제외, 쿠폰 할인 전 상품금액 */
    public int calculateShippingFee(int productAmount, LocalDateTime now) {
        if (promotionEndDate != null && !now.toLocalDate().isAfter(promotionEndDate)) {
            return 0;
        }
        return productAmount >= freeThreshold ? 0 : baseFee;
    }
}