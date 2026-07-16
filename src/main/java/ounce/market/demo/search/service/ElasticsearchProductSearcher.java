package ounce.market.demo.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.stereotype.Component;
import ounce.market.demo.search.document.ProductDocument;
import ounce.market.demo.search.dto.*;
import ounce.market.demo.search.dto.response.ProductSearchResponse;
import ounce.market.demo.search.repository.ProductSearcher;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch")
public class ElasticsearchProductSearcher implements ProductSearcher {

    private final ElasticsearchOperations operations;

    @Override
    public Page<ProductSearchResponse> search(SearchCondition cond, Pageable pageable) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    if (cond.keyword() != null && !cond.keyword().isBlank()) {
                        b.must(m -> m.match(mt -> mt
                                .field("name")
                                .query(cond.keyword())));
                    } else {
                        b.must(m -> m.matchAll(ma -> ma));
                    }

                    if (cond.minPrice() != null || cond.maxPrice() != null) {
                        b.filter(f -> f.range(r -> r.number(n -> {
                            n.field("salePrice");
                            if (cond.minPrice() != null) n.gte(cond.minPrice().doubleValue());
                            if (cond.maxPrice() != null) n.lte(cond.maxPrice().doubleValue());
                            return n;
                        })));
                    }

                    if (Boolean.TRUE.equals(cond.inStockOnly())) {
                        b.filter(f -> f.range(r -> r.number(n -> n
                                .field("stock").gt(0.0))));
                    }
                    return b;
                }))
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> hits = operations.search(query, ProductDocument.class);

        List<ProductSearchResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ProductSearchResponse::from)
                .toList();

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }
}