package ounce.market.demo.order.dto.request;


import lombok.Getter;
import lombok.NoArgsConstructor;
import ounce.market.demo.delivery.entity.DeliveryType;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {
    private List<Long> selectedCartProductIds;
    private DeliveryType deliveryType;
}