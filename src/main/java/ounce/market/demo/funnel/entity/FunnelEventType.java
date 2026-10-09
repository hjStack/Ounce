package ounce.market.demo.funnel.entity;

public enum FunnelEventType {
    PAGE_VIEW("page_view"),
    SIGNUP("signup"),
    PRODUCT_VIEW("product_view"),
    CART_ADD("cart_add"),
    ORDER_COMPLETE("order_complete");

    private final String value;

    FunnelEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
