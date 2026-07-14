package com.campus.trade.order.enums;

public enum OrderStatusEnum {
    PENDING_COMMUNICATION,
    PENDING_PICKUP,
    COMPLETED,
    CANCELLED;

    public boolean canAdvanceTo(OrderStatusEnum target) {
        return (this == PENDING_COMMUNICATION && target == PENDING_PICKUP)
                || (this == PENDING_PICKUP && target == COMPLETED);
    }

    public boolean canCancel() {
        return this == PENDING_COMMUNICATION || this == PENDING_PICKUP;
    }
}
