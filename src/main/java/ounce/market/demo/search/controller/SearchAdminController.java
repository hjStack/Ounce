package ounce.market.demo.search.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.web.bind.annotation.*;
import ounce.market.demo.search.document.ProductDocument;
import ounce.market.demo.search.indexer.ProductBulkIndexer;

@Hidden
@Profile("local")
@RestController
@RequestMapping("/dev/search")
@RequiredArgsConstructor
public class SearchAdminController {

    private final ElasticsearchOperations operations;
    private final ProductBulkIndexer indexer;

    @PostMapping("/index")
    public String createIndex() {
        IndexOperations ops = operations.indexOps(ProductDocument.class);
        if (ops.exists()) ops.delete();
        ops.create();
        ops.putMapping();
        return "created";
    }

    @PostMapping("/reindex")
    public String reindex() {
        long count = indexer.reindexAll();
        return count + "건 색인 완료";
    }
}