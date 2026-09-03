package ounce.market.demo.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ounce.market.demo.subscription.entity.PaymentClient;
import ounce.market.demo.subscription.entity.StubPaymentClient;

@Configuration
public class PaymentConfig {

    @Bean
    public PaymentClient paymentClient() {
        return new StubPaymentClient();
    }
}