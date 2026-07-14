package com.campus.trade.order.controller;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.Result;
import com.campus.trade.order.dto.OrderCancelDTO;
import com.campus.trade.order.dto.OrderCreateDTO;
import com.campus.trade.order.dto.OrderQueryDTO;
import com.campus.trade.order.dto.OrderStatusDTO;
import com.campus.trade.order.service.OrderService;
import com.campus.trade.order.vo.OrderVO;
import com.campus.trade.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单")
@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderVO> create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(user.userId(), dto));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @ModelAttribute OrderQueryDTO query) {
        return Result.success(orderService.list(user.userId(), query));
    }

    @Operation(summary = "我买到的（任务书兼容路径）")
    @GetMapping("/buy")
    public Result<PageResult<OrderVO>> buy(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @ModelAttribute OrderQueryDTO query) {
        query.setRole("buyer");
        return Result.success(orderService.list(user.userId(), query));
    }

    @Operation(summary = "我卖出的（任务书兼容路径）")
    @GetMapping("/sell")
    public Result<PageResult<OrderVO>> sell(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @ModelAttribute OrderQueryDTO query) {
        query.setRole("seller");
        return Result.success(orderService.list(user.userId(), query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return Result.success(orderService.getDetail(user.userId(), id));
    }

    @Operation(summary = "推进订单状态")
    @PutMapping("/{id}/status")
    public Result<OrderVO> updateStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                        @Valid @RequestBody OrderStatusDTO dto) {
        return Result.success(orderService.updateStatus(user.userId(), id, dto));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<OrderVO> cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                  @Valid @RequestBody(required = false) OrderCancelDTO dto) {
        return Result.success(orderService.cancel(user.userId(), id, dto));
    }
}
