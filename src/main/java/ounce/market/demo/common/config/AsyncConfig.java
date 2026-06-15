package ounce.market.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "ounceAsyncExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);
        executor.setQueueCapacity(50); // 2. 대기줄 의자 수 (10명이 다 바쁘면 50명까지 큐에 대기)
        executor.setMaxPoolSize(30);   // 3. 최대 창구 직원 수 (50명 대기줄까지 꽉 차면, 최대 20명을 추가 투입)

        executor.setThreadNamePrefix("Ounce-Async-"); // 스레드 이름에 예쁜 명찰 달아주기
        executor.initialize();
        return executor;
    }

}
