package com.campus.trade.order.vo;

public record OrderAddressVO(
        Long id,
        String contact,
        String phone,
        String campus,
        String building,
        String room,
        String detail
) {
}
