package ounce.market.demo.funnel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FunnelJacksonConfig {

    @Bean
    public ObjectMapper funnelObjectMapper() {
        return new ObjectMapper();
    }
}
