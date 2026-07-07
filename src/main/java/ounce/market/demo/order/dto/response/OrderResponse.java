package ounce.market.demo.order.dto.response;


import lombok.Getter;
import ounce.market.demo.order.entity.Order;

@Getter
public class OrderResponse {
    private final Long orderId;
    private final int totalAmount;
    private final String status;

    private OrderResponse(Order order) {
        this.orderId = order.getOrderId();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus().name();
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order);
    }
}