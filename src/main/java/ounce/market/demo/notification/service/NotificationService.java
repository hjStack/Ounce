package ounce.market.demo.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    // 💡 아까 설정한 스레드 풀의 이름을 지정해줍니다.
    @Async("ounceAsyncExecutor")
    public void sendWelcomeEmail(String email) {
        log.info("[비동기 시작] {} 님에게 환영 이메일을 발송합니다. (Thread: {})", email, Thread.currentThread().getName());

        try {
            // 이메일 발송에 3초가 걸린다고 가정 (스레드 지연)
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("[비동기 완료] 이메일 발송 완료! (Thread: {})", Thread.currentThread().getName());
    }
}