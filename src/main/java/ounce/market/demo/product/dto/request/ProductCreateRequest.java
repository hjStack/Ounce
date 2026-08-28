package ounce.market.demo.product.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.product.entity.Product;
import ounce.market.demo.product.entity.ProductStatus;

import java.util.List;

@Getter // 서비스에서 꺼내 쓰려면(get) 필수!
@NoArgsConstructor // 프론트에서 넘어온 JSON을 자바 객체로 바꿀 때 필수!
public class ProductCreateRequest {

    private String name;
    private Long basePrice; // (참고: ProductResponse에 Long으로 되어 있어서 맞췄습니다!)
    private long salePrice;
    private long discountPercent;
    private String description;

    // 💡 방금 우리가 포스트맨으로 테스트하고 받아올 그 S3 이미지 주소!
    private String imageUrl;

    private int stock;

    // 💡 아까 ProductService의 주석 코드에서 찾던 그 카테고리 아이디들!
    private List<Long> categoryIds;

    // 💡 서비스 계층에서 request.toEntity() 로 바로 변환할 수 있게 해주는 핵심 메서드
    public Product toEntity() {
        return Product.builder()
                .name(this.name)
                .basePrice(this.basePrice)

                .discountPercent(this.discountPercent)
                .description(this.description)
                .imageUrl(this.imageUrl) // S3 주소가 드디어 DB로 들어가는 순간!
                .stock(this.stock)
                .build();
    }
}