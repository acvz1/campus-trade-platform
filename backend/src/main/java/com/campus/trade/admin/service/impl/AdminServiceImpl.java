package com.campus.trade.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.admin.dto.AdminProductQueryDTO;
import com.campus.trade.admin.dto.AuditDTO;
import com.campus.trade.admin.service.AdminService;
import com.campus.trade.admin.vo.AdminProductVO;
import com.campus.trade.admin.vo.AdminSellerVO;
import com.campus.trade.admin.vo.DashboardVO;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.entity.AuditLog;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductImage;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.AuditLogMapper;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.product.vo.ProductStatusVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {
    private static final Set<String> REVIEW_STATUSES = Set.of("PENDING_REVIEW", "ON_SALE", "REJECTED", "VIOLATION_DELISTED");
    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Shanghai");

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final AuditLogMapper auditLogMapper;

    public AdminServiceImpl(ProductMapper productMapper, ProductImageMapper productImageMapper,
                            CategoryMapper categoryMapper, UserMapper userMapper,
                            OrderMapper orderMapper, AuditLogMapper auditLogMapper) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public PageResult<AdminProductVO> listProducts(AdminProductQueryDTO query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        String status = hasText(query.getStatus()) ? query.getStatus().trim().toUpperCase() : "PENDING_REVIEW";
        if (!REVIEW_STATUSES.contains(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的商品状态筛选");
        }
        wrapper.eq(Product::getStatus, status);
        if (hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(Product::getTitle, keyword).or().like(Product::getDescription, keyword));
        }
        if (query.getCategoryId() != null) wrapper.eq(Product::getCategoryId, query.getCategoryId());
        wrapper.orderByAsc(Product::getCreatedAt);
        Page<Product> page = productMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<>(enrich(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    @Transactional
    public ProductStatusVO audit(Long operatorId, Long productId, AuditDTO dto) {
        Product product = requireProduct(productId);
        if (!"PENDING_REVIEW".equals(product.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "只有待审核商品可以审核");
        }
        String action = dto.action().toUpperCase();
        String target = "APPROVE".equals(action) ? "ON_SALE" : "REJECTED";
        updateProductStatus(product, target, "APPROVE".equals(action));
        insertLog(operatorId, product, action, target, normalizedReason(dto.reason()));
        return new ProductStatusVO(productId, target);
    }

    @Override
    @Transactional
    public void violation(Long operatorId, Long productId, String reason) {
        Product product = requireProduct(productId);
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "只有在售商品可以违规下架");
        }
        updateProductStatus(product, "VIOLATION_DELISTED", false);
        insertLog(operatorId, product, "VIOLATION_DELIST", "VIOLATION_DELISTED", normalizedReason(reason));
    }

    @Override
    @Transactional
    public void relieveViolation(Long operatorId, Long productId, String reason) {
        Product product = requireProduct(productId);
        if (!"VIOLATION_DELISTED".equals(product.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "只有违规下架商品可以解除违规");
        }
        updateProductStatus(product, "ON_SALE", true);
        insertLog(operatorId, product, "RELIEVE_VIOLATION", "ON_SALE", normalizedReason(reason));
    }

    @Override
    public DashboardVO dashboard() {
        OffsetDateTime start = LocalDate.now(CAMPUS_ZONE).atStartOfDay(CAMPUS_ZONE).toOffsetDateTime();
        return new DashboardVO(
                userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER")),
                productMapper.selectCount(null),
                orderMapper.selectCount(null),
                orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getStatus, "COMPLETED")),
                userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER").ge(User::getCreatedAt, start)),
                productMapper.selectCount(new LambdaQueryWrapper<Product>().ge(Product::getCreatedAt, start)),
                orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreatedAt, start)),
                productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, "PENDING_REVIEW"))
        );
    }

    private List<AdminProductVO> enrich(List<Product> products) {
        if (products.isEmpty()) return List.of();
        Set<Long> ids = products.stream().map(Product::getId).collect(Collectors.toSet());
        Map<Long, User> sellers = userMapper.selectByIds(products.stream().map(Product::getSellerId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Category> categories = categoryMapper.selectByIds(products.stream().map(Product::getCategoryId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, String> images = new HashMap<>();
        productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>().in(ProductImage::getProductId, ids)
                        .orderByDesc(ProductImage::getIsMain).orderByAsc(ProductImage::getSortOrder))
                .forEach(image -> images.putIfAbsent(image.getProductId(), image.getImageUrl()));
        return products.stream().map(product -> {
            User seller = sellers.get(product.getSellerId());
            Category category = categories.get(product.getCategoryId());
            AdminSellerVO sellerVO = seller == null ? null : new AdminSellerVO(seller.getId(), seller.getNickname(),
                    seller.getPhone(), seller.getStudentId());
            return new AdminProductVO(product.getId(), product.getTitle(), product.getDescription(), images.get(product.getId()),
                    product.getPrice(), product.getStatus(), product.getCategoryId(), category == null ? null : category.getName(),
                    sellerVO, product.getCreatedAt(), product.getUpdatedAt());
        }).toList();
    }

    private Product requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        return product;
    }

    private void updateProductStatus(Product product, String status, boolean publish) {
        Product update = new Product();
        update.setId(product.getId());
        update.setStatus(status);
        if (publish) update.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        productMapper.updateById(update);
    }

    private void insertLog(Long operatorId, Product product, String action, String target, String reason) {
        AuditLog log = new AuditLog();
        log.setProductId(product.getId());
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setFromStatus(product.getStatus());
        log.setToStatus(target);
        log.setReason(reason);
        auditLogMapper.insert(log);
    }

    private String normalizedReason(String reason) {
        return hasText(reason) ? reason.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
