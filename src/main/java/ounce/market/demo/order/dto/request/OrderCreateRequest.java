package ounce.market.demo.order.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.delivery.entity.DeliveryType;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    private List<Long> selectedCartProductIds;
    private DeliveryType deliveryType;
    private Long couponId;
    private String receiverName;
    private String receiverPhone;
    private String zipCode;
    private String address;
    private String addressDetail;
}
