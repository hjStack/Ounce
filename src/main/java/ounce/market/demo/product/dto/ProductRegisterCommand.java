package ounce.market.demo.product.dto;

public record ProductRegisterCommand(
        String productCode,
        String name,
        Long basePrice,
        String description,
        String imageUrl,
        int initialStock
) {
}
