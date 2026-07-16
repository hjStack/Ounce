package ounce.market.demo.search.indexer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.repository.ProductCategoryRepository;
import ounce.market.demo.product.repository.ProductRepository;
import ounce.market.demo.search.document.ProductDocument;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBulkIndexer {

    private static final int CHUNK = 1000;

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ElasticsearchOperations operations;

    @Transactional(readOnly = true)
    public long reindexAll() {
        long start = System.currentTimeMillis();
        long lastId = 0L;
        long total = 0L;

        while (true) {
            List<Product> products =
                    productRepository.findForIndexing(lastId, PageRequest.of(0, CHUNK));
            if (products.isEmpty()) break;

            // 카테고리를 한 방에 조회 → N+1 방지
            List<Long> ids = products.stream().map(Product::getProductId).toList();
            Map<Long, List<String>> categoryMap = productCategoryRepository
                    .findAllByProductIdIn(ids).stream()
                    .collect(Collectors.groupingBy(
                            pc -> pc.getProduct().getProductId(),
                            Collectors.mapping(pc -> pc.getCategory().getName(), Collectors.toList())
                    ));

            List<IndexQuery> queries = products.stream()
                    .map(p -> ProductDocument.from(
                            p, categoryMap.getOrDefault(p.getProductId(), List.of())))
                    .map(doc -> new IndexQueryBuilder()
                            .withId(doc.getId())
                            .withObject(doc)
                            .build())
                    .toList();

            operations.bulkIndex(queries, ProductDocument.class);

            total += products.size();
            lastId = products.get(products.size() - 1).getProductId();
            log.info("색인 진행: {}건 (lastId={})", total, lastId);
        }

        operations.indexOps(ProductDocument.class).refresh();
        log.info("색인 완료: {}건, {}ms", total, System.currentTimeMillis() - start);
        return total;
    }
}