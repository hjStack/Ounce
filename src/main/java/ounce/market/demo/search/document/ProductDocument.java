package ounce.market.demo.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.*;
import ounce.market.demo.product.entity.Product;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;

@Document(indexName = "products", createIndex = false)  // 매핑은 파일로 관리
@Setting(settingPath = "elastic/product-settings.json")
@Mapping(mappingPath = "elastic/product-mappings.json")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long productId;

    @MultiField(
            mainField = @Field(type = FieldType.Text,
                    analyzer = "korean_index_analyzer",
                    searchAnalyzer = "korean_search_analyzer"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String name;

    @Field(type = FieldType.Keyword)
    private List<String> category;

    @Field(type = FieldType.Long)
    private Long salePrice;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime createdAt;

    public static ProductDocument from(Product p, List<String> categories) {
        return ProductDocument.builder()
                .id(String.valueOf(p.getProductId()))
                .productId(p.getProductId())
                .name(p.getName())
                .salePrice(p.getSalePrice())
                .stock(p.getStock())
                .category(categories)
                .build();
    }

}