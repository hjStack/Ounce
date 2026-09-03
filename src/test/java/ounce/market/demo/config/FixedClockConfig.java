package ounce.market.demo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@TestConfiguration
public class FixedClockConfig {
    @Bean
    public Clock clock() {
        // 월요일 22:59 — 마감 1분 전
        return Clock.fixed(
                ZonedDateTime.of(2026, 9, 7, 22, 59, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
    }
}