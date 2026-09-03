package ounce.market.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /**
     * 23시 마감, 새벽 배송일, 2시간 재시도 간격이 전부 이 시계를 기준으로 계산된다.
     * 서버 기본 타임존에 맡기면 로컬에서는 KST, 배포 서버에서는 UTC로 돌아가
     * 배송일이 하루씩 어긋난다. 존을 명시적으로 고정한다.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}