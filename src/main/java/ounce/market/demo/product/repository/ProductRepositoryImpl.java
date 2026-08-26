
package ounce.market.demo.product.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ounce.market.demo.product.dto.request.ProductSearchCondition;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ounce.market.demo.product.entity.QCategory.category;
import static ounce.market.demo.product.entity.QProduct.product;
import static ounce.market.demo.product.entity.QProductCategory.productCategory;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * hasNext 판별을 위해 요청한 size보다 1건 더 조회한다.
     * 호출하는 쪽에서 size 초과분을 잘라내고 hasNext로 사용할 것.
     */
    @Override
    public List<Product> search(ProductSearchCondition cond, List<ProductStatus> visible) {
        return queryFactory
                .selectFrom(product)
                .where(
                        product.status.in(visible),
                        categoryEq(cond.getCategories()),
                        keywordContains(cond.getKeyword())
                )
                .orderBy(soldOutLast(), sortOrder(cond.getSort()))
                .offset(cond.getOffset())
                .limit(cond.getSafeSize() + 1)
                .fetch();
    }

    @Override
    public Map<String, Long> countByCategory(String keyword, List<ProductStatus> visible) {
        List<Tuple> rows = queryFactory
                .select(category.code, product.countDistinct())
                .from(productCategory)
                .join(productCategory.category, category)
                .join(productCategory.product, product)
                .where(
                        product.status.in(visible),
                        keywordContains(keyword)
                )
                .groupBy(category.code)
                .fetch();

        Map<String, Long> result = new LinkedHashMap<>();
        for (Tuple row : rows) {
            result.put(row.get(category.code), row.get(product.countDistinct()));
        }
        return result;
    }

    @Override
    public long countAll(String keyword, List<ProductStatus> visible) {
        Long count = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        product.status.in(visible),
                        keywordContains(keyword)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    /* ── 동적 조건 ─────────────────────────────────────────
       null을 반환하면 where 절에서 해당 조건이 자동으로 무시된다.
       이게 QueryDSL 동적 쿼리의 핵심 패턴. */

    /** 한 상품이 여러 카테고리에 속하므로 join 대신 서브쿼리로 필터 (중복 row 방지) */
    private BooleanExpression categoryEq(String code) {
        if (code == null || code.isBlank() || code.equals("all")) {
            return null;
        }
        return product.productId.in(
                JPAExpressions
                        .select(productCategory.product.productId)
                        .from(productCategory)
                        .join(productCategory.category, category)
                        .where(category.code.eq(code))
        );
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return product.name.containsIgnoreCase(keyword)
                .or(product.description.containsIgnoreCase(keyword));
    }

    /* ── 정렬 ─────────────────────────────────────────────── */

    /** 품절(SOLD_OUT 또는 재고 0)을 항상 뒤로 — 프론트 isSoldOut 로직과 동일 */
    private OrderSpecifier<Integer> soldOutLast() {
        NumberExpression<Integer> rank = new CaseBuilder()
                .when(product.status.eq(ProductStatus.SOLD_OUT).or(product.stock.loe(0)))
                .then(1)
                .otherwise(0);
        return rank.asc();
    }

    private OrderSpecifier<?> sortOrder(String sort) {
        if (sort == null) {
            return product.productId.desc();
        }
        return switch (sort) {
            case "price-asc"  -> product.basePrice.asc();
            case "price-desc" -> product.basePrice.desc();
            case "name"       -> product.name.asc();
            default           -> product.productId.desc();   // "new"
        };
    }
}