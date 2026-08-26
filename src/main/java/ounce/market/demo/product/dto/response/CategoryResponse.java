package ounce.market.demo.product.dto.response;

import lombok.Getter;
import ounce.market.demo.product.entity.Category;

@Getter
public class CategoryResponse {

    private final Long id;
    private final String code;
    private final String name;

    public CategoryResponse(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getCode(), category.getName());
    }
}
