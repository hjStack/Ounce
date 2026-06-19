package ounce.market.demo.product.dto;

public record ProductRegisterCommand(
        String productCode,
        String name,
        int basePrice,
        String description,
        String imageUrl,
        int initialStock
) {
}
