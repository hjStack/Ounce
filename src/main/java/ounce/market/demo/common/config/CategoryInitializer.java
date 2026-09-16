package ounce.market.demo.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ounce.market.demo.product.entity.Category;
import ounce.market.demo.product.repository.CategoryRepository;

@Component
@RequiredArgsConstructor
public class CategoryInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 기존 카테고리는 보존하고, 밥 카테고리만 누락된 환경에 추가한다.
        if (categoryRepository.findByCode("rice").isEmpty()) {
            categoryRepository.save(Category.builder()
                    .code("rice")
                    .name("밥·덮밥")
                    .build());
        }
    }
}
