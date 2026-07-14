package com.campus.trade.order.service;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.order.dto.OrderCancelDTO;
import com.campus.trade.order.dto.OrderCreateDTO;
import com.campus.trade.order.dto.OrderQueryDTO;
import com.campus.trade.order.dto.OrderStatusDTO;
import com.campus.trade.order.vo.OrderVO;

public interface OrderService {
    OrderVO create(Long buyerId, OrderCreateDTO dto);

    OrderVO getDetail(Long userId, Long orderId);

    PageResult<OrderVO> list(Long userId, OrderQueryDTO query);

    OrderVO updateStatus(Long userId, Long orderId, OrderStatusDTO dto);

    OrderVO cancel(Long userId, Long orderId, OrderCancelDTO dto);
}
