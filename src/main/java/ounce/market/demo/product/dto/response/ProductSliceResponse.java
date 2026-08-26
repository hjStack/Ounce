package ounce.market.demo.product.dto.response;

import lombok.Getter;
import java.util.List;

// 페이징 처리를 위한 response

@Getter
public class ProductSliceResponse {

    private final List<ProductResponse> products;
    private final int page;
    private final int size;
    private final boolean hasNext;

    public ProductSliceResponse(List<ProductResponse> products, int page, int size, boolean hasNext) {
        this.products = products;
        this.page = page;
        this.size = size;
        this.hasNext = hasNext;
    }
}
