package ounce.market.demo.order.dto.response;
import lombok.Getter;
import ounce.market.demo.order.entity.OrderItem;

@Getter
public class OrderItemResponse {

    private final Long productId;
    private final String productName;
    private final int price;       // 주문 시점 스냅샷 가격
    private final int quantity;
    private final String imageUrl;

    private OrderItemResponse(OrderItem item) {
        this.productId = item.getProduct().getProductId();
        this.productName = item.getProduct().getName();
        this.price = item.getPrice();
        this.quantity = item.getQuantity();
        this.imageUrl = item.getProduct().getImageUrl();
    }

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item);
    }
}