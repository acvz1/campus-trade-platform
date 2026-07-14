package com.campus.trade.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductImage;
import com.campus.trade.entity.User;
import com.campus.trade.entity.UserAddress;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserAddressMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.order.dto.OrderCancelDTO;
import com.campus.trade.order.dto.OrderCreateDTO;
import com.campus.trade.order.dto.OrderQueryDTO;
import com.campus.trade.order.dto.OrderStatusDTO;
import com.campus.trade.order.enums.OrderStatusEnum;
import com.campus.trade.order.service.OrderService;
import com.campus.trade.order.vo.OrderAddressVO;
import com.campus.trade.order.vo.OrderPartyVO;
import com.campus.trade.order.vo.OrderVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter PICKUP_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final UserMapper userMapper;
    private final UserAddressMapper userAddressMapper;

    public OrderServiceImpl(OrderMapper orderMapper, ProductMapper productMapper,
                            ProductImageMapper productImageMapper, UserMapper userMapper,
                            UserAddressMapper userAddressMapper) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.userMapper = userMapper;
        this.userAddressMapper = userAddressMapper;
    }

    @Override
    @Transactional
    public OrderVO create(Long buyerId, OrderCreateDTO dto) {
        User buyer = requireVerifiedActiveUser(buyerId);
        Product product = productMapper.selectById(dto.productId());
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "商品当前不可下单");
        }
        if (buyerId.equals(product.getSellerId())) {
            throw new BusinessException(ResultCode.CONFLICT, "不能购买自己发布的商品");
        }
        UserAddress address = null;
        if (dto.addressId() != null) {
            address = requireOwnedAddress(buyerId, dto.addressId());
        }
        if ("DELIVERY".equals(product.getTradeType()) && address == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "送货交易必须选择收货地址");
        }
        Order active = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getProductId, product.getId())
                .ne(Order::getStatus, OrderStatusEnum.CANCELLED.name())
                .last("LIMIT 1"));
        if (active != null) {
            String message = buyerId.equals(active.getBuyerId()) ? "不可重复下单" : "该商品已有进行中的订单";
            throw new BusinessException(ResultCode.CONFLICT, message);
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setProductId(product.getId());
        order.setAddressId(address == null ? null : address.getId());
        order.setPrice(product.getPrice());
        order.setTradeType(product.getTradeType());
        order.setBuyerRemark(hasText(dto.remark()) ? dto.remark().trim() : null);
        order.setStatus(OrderStatusEnum.PENDING_COMMUNICATION.name());
        try {
            orderMapper.insert(order);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "该商品已有进行中的订单");
        }
        return toOrderVO(order, product, buyer, requireUser(product.getSellerId()), address, mainImage(product.getId()));
    }

    @Override
    public OrderVO getDetail(Long userId, Long orderId) {
        return buildDetail(requireParticipant(userId, orderId));
    }

    @Override
    public PageResult<OrderVO> list(Long userId, OrderQueryDTO query) {
        requireUser(userId);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if ("seller".equals(query.getRole())) wrapper.eq(Order::getSellerId, userId);
        else wrapper.eq(Order::getBuyerId, userId);
        if (hasText(query.getStatus())) wrapper.eq(Order::getStatus, query.getStatus());
        wrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> page = orderMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<>(enrich(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    @Transactional
    public OrderVO updateStatus(Long userId, Long orderId, OrderStatusDTO dto) {
        String action = resolveAction(dto);
        if ("CANCEL".equals(action)) {
            return cancelInternal(userId, orderId, dto.cancelReason());
        }
        Order order = requireParticipant(userId, orderId);
        OrderStatusEnum current = OrderStatusEnum.valueOf(order.getStatus());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Order update = new Order();
        update.setId(orderId);

        if ("CONFIRM_PICKUP".equals(action)) {
            if (!current.canAdvanceTo(OrderStatusEnum.PENDING_PICKUP)) {
                throw new BusinessException(ResultCode.CONFLICT, "订单不能从当前状态进入待自提");
            }
            if (!hasText(dto.pickupTime()) || !hasText(dto.pickupLocation())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "请填写约定时间和地点");
            }
            update.setStatus(OrderStatusEnum.PENDING_PICKUP.name());
            update.setPickupTime(parsePickupTime(dto.pickupTime()));
            update.setPickupLocation(dto.pickupLocation().trim());
            update.setConfirmedAt(now);
        } else if ("COMPLETE".equals(action)) {
            if (!userId.equals(order.getBuyerId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "仅买家可以确认交易完成");
            }
            if (!current.canAdvanceTo(OrderStatusEnum.COMPLETED)) {
                throw new BusinessException(ResultCode.CONFLICT, "订单不能从当前状态直接完成");
            }
            update.setStatus(OrderStatusEnum.COMPLETED.name());
            update.setCompletedAt(now);
            Product productUpdate = new Product();
            productUpdate.setId(order.getProductId());
            productUpdate.setStatus("SOLD");
            productUpdate.setSoldAt(now);
            productMapper.updateById(productUpdate);
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单操作不正确");
        }
        orderMapper.updateById(update);
        return buildDetail(orderMapper.selectById(orderId));
    }

    @Override
    @Transactional
    public OrderVO cancel(Long userId, Long orderId, OrderCancelDTO dto) {
        return cancelInternal(userId, orderId, dto == null ? null : dto.cancelReason());
    }

    private OrderVO cancelInternal(Long userId, Long orderId, String reason) {
        Order order = requireParticipant(userId, orderId);
        OrderStatusEnum current = OrderStatusEnum.valueOf(order.getStatus());
        if (!current.canCancel()) {
            throw new BusinessException(ResultCode.CONFLICT, "当前订单状态不可取消");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(OrderStatusEnum.CANCELLED.name());
        update.setCancelReason(hasText(reason) ? reason.trim() : "用户取消订单");
        update.setCancelledBy(userId);
        update.setCancelledAt(now);
        orderMapper.updateById(update);
        return buildDetail(orderMapper.selectById(orderId));
    }

    private String resolveAction(OrderStatusDTO dto) {
        if (hasText(dto.action())) return dto.action();
        return switch (dto.status()) {
            case "PENDING_PICKUP" -> "CONFIRM_PICKUP";
            case "COMPLETED" -> "COMPLETE";
            case "CANCELLED" -> "CANCEL";
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "目标状态不正确");
        };
    }

    private List<OrderVO> enrich(List<Order> orders) {
        if (orders.isEmpty()) return List.of();
        Set<Long> productIds = orders.stream().map(Order::getProductId).collect(Collectors.toSet());
        Set<Long> userIds = orders.stream().flatMap(order -> List.of(order.getBuyerId(), order.getSellerId()).stream())
                .collect(Collectors.toSet());
        Set<Long> addressIds = orders.stream().map(Order::getAddressId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Product> products = productMapper.selectByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, User> users = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, UserAddress> addresses = addressIds.isEmpty() ? new HashMap<>() : userAddressMapper.selectByIds(addressIds).stream()
                .collect(Collectors.toMap(UserAddress::getId, Function.identity()));
        Map<Long, String> images = mainImages(productIds);
        return orders.stream().map(order -> toOrderVO(order, products.get(order.getProductId()),
                users.get(order.getBuyerId()), users.get(order.getSellerId()), addresses.get(order.getAddressId()),
                images.get(order.getProductId()))).toList();
    }

    private OrderVO buildDetail(Order order) {
        Product product = productMapper.selectById(order.getProductId());
        UserAddress address = order.getAddressId() == null ? null : userAddressMapper.selectById(order.getAddressId());
        return toOrderVO(order, product, requireUser(order.getBuyerId()), requireUser(order.getSellerId()),
                address, mainImage(order.getProductId()));
    }

    private OrderVO toOrderVO(Order order, Product product, User buyer, User seller,
                              UserAddress address, String productImage) {
        return new OrderVO(order.getId(), order.getOrderNo(), order.getProductId(),
                product == null ? "商品已删除" : product.getTitle(), productImage, order.getPrice(), order.getTradeType(),
                order.getStatus(), toParty(buyer), toParty(seller), toAddress(address), order.getBuyerRemark(),
                order.getPickupTime(), order.getPickupLocation(), order.getConfirmedAt(), order.getCompletedAt(),
                order.getCancelledAt(), order.getCancelReason(), order.getCancelledBy(), order.getCreatedAt(), order.getUpdatedAt());
    }

    private OrderPartyVO toParty(User user) {
        return new OrderPartyVO(user.getId(), user.getNickname(), user.getAvatarUrl(), maskPhone(user.getPhone()));
    }

    private OrderAddressVO toAddress(UserAddress address) {
        if (address == null) return null;
        return new OrderAddressVO(address.getId(), address.getContactName(), address.getContactPhone(),
                address.getCampus(), address.getBuilding(), address.getRoom(), address.getDetail());
    }

    private Map<Long, String> mainImages(Set<Long> productIds) {
        Map<Long, List<ProductImage>> grouped = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds).orderByAsc(ProductImage::getSortOrder))
                .stream().collect(Collectors.groupingBy(ProductImage::getProductId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, String> result = new HashMap<>();
        grouped.forEach((productId, images) -> result.put(productId, images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsMain())).findFirst().orElse(images.get(0)).getImageUrl()));
        return result;
    }

    private String mainImage(Long productId) {
        ProductImage image = productImageMapper.selectOne(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId).orderByDesc(ProductImage::getIsMain)
                .orderByAsc(ProductImage::getSortOrder).last("LIMIT 1"));
        return image == null ? null : image.getImageUrl();
    }

    private Order requireParticipant(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅订单买卖双方可以查看或操作");
        }
        return order;
    }

    private User requireVerifiedActiveUser(Long userId) {
        User user = requireUser(userId);
        if (!"ACTIVE".equals(user.getStatus()) || !hasText(user.getStudentId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "请先完成校园认证并确认账号状态正常");
        }
        return user;
    }

    private User requireUser(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        return user;
    }

    private UserAddress requireOwnedAddress(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, addressId).eq(UserAddress::getUserId, userId));
        if (address == null) throw new BusinessException(ResultCode.BAD_REQUEST, "收货地址不存在或不属于当前用户");
        return address;
    }

    private OffsetDateTime parsePickupTime(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, PICKUP_TIME).atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime();
            } catch (DateTimeParseException exception) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "约定时间格式应为 yyyy-MM-dd HH:mm");
            }
        }
    }

    private String generateOrderNo() {
        return OffsetDateTime.now(ZoneOffset.UTC).format(ORDER_TIME)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
