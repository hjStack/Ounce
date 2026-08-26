package ounce.market.demo.product.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchCondition {

    private String categories;
    private String keyword;
    private String sort = "new";
    private int page = 0;
    private int size = 10;

    /** 잘못된 값이 들어와도 쿼리가 깨지지 않게 방어 */
    public int getSafeSize() {
        if (size < 1) return 10;
        return Math.min(size, 100);   // 상한 제한 (전체 덤프 방지)
    }

    public int getSafePage() {
        return Math.max(page, 0);
    }

    public long getOffset() {
        return (long) getSafePage() * getSafeSize();
    }
}