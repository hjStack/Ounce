package ounce.market.demo.product.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ounce.market.demo.product.dto.response.CategoryResponse;
import ounce.market.demo.product.repository.CategoryRepository;

import java.util.List;

@Tag(name = "13. 카테고리", description = "상품 카테고리 조회")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByIdAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
