package com.campus.trade.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Favorite;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductImage;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.product.dto.ProductImageDTO;
import com.campus.trade.product.dto.ProductPublishDTO;
import com.campus.trade.product.dto.ProductSearchDTO;
import com.campus.trade.product.enums.ProductStatusEnum;
import com.campus.trade.product.service.ProductService;
import com.campus.trade.product.vo.ProductImageVO;
import com.campus.trade.product.vo.ProductStatusVO;
import com.campus.trade.product.vo.ProductVO;
import com.campus.trade.product.vo.SellerVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    private static final Set<String> ACTIVE_PRODUCT_STATUSES = Set.of("PENDING_REVIEW", "ON_SALE");
    private static final Set<String> EDITABLE_STATUSES = Set.of("PENDING_REVIEW", "ON_SALE", "REJECTED", "OFF_SHELF");
    private static final Set<String> DELETABLE_STATUSES = Set.of("PENDING_REVIEW", "REJECTED", "OFF_SHELF", "SOLD");
    private static final int DAILY_PUBLISH_LIMIT = 5;
    private static final int ACTIVE_PUBLISH_LIMIT = 50;

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final FavoriteMapper favoriteMapper;

    public ProductServiceImpl(ProductMapper productMapper, ProductImageMapper productImageMapper,
                              CategoryMapper categoryMapper, UserMapper userMapper,
                              OrderMapper orderMapper, FavoriteMapper favoriteMapper) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    @Transactional
    public ProductVO publish(Long userId, ProductPublishDTO dto) {
        User seller = requireEligibleSeller(userId);
        Category category = requireActiveCategory(dto.categoryId());
        validatePublish(dto);
        enforcePublishLimits(userId);

        Product product = new Product();
        applyProduct(product, userId, dto);
        product.setStatus(ProductStatusEnum.PENDING_REVIEW.name());
        product.setViewCount(0);
        product.setFavoriteCount(0);
        productMapper.insert(product);
        List<ProductImage> images = replaceImages(product.getId(), dto.images());
        return toProductVO(product, category, seller, images, 0);
    }

    @Override
    @Transactional
    public ProductVO getDetail(Long productId, Long viewerId) {
        Product product = productMapper.selectById(productId);
        if (product == null || (!ProductStatusEnum.ON_SALE.name().equals(product.getStatus())
                && !product.getSellerId().equals(viewerId))) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或不可查看");
        }
        if (ProductStatusEnum.ON_SALE.name().equals(product.getStatus())) {
            productMapper.incrementViewCount(productId);
            product.setViewCount(product.getViewCount() + 1);
        }
        return buildDetail(product);
    }

    @Override
    public PageResult<ProductVO> list(ProductSearchDTO query) {
        LambdaQueryWrapper<Product> wrapper = baseSearchWrapper(query)
                .eq(Product::getStatus, ProductStatusEnum.ON_SALE.name());
        return selectPage(query, wrapper);
    }

    @Override
    public PageResult<ProductVO> listMine(Long userId, ProductSearchDTO query) {
        requireUser(userId);
        LambdaQueryWrapper<Product> wrapper = baseSearchWrapper(query).eq(Product::getSellerId, userId);
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(Product::getStatus, query.getStatus());
        }
        return selectPage(query, wrapper);
    }

    @Override
    @Transactional
    public ProductVO update(Long userId, Long productId, ProductPublishDTO dto) {
        Product current = requireOwnedProduct(userId, productId);
        if (!EDITABLE_STATUSES.contains(current.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "当前商品状态不可编辑");
        }
        ensureNoActiveOrder(productId);
        Category category = requireActiveCategory(dto.categoryId());
        validatePublish(dto);

        Product update = new Product();
        update.setId(productId);
        applyProduct(update, userId, dto);
        if (!ProductStatusEnum.OFF_SHELF.name().equals(current.getStatus())) {
            update.setStatus(ProductStatusEnum.PENDING_REVIEW.name());
        } else {
            update.setStatus(current.getStatus());
        }
        productMapper.updateById(update);
        List<ProductImage> images = replaceImages(productId, dto.images());

        update.setViewCount(current.getViewCount());
        update.setFavoriteCount(current.getFavoriteCount());
        update.setCreatedAt(current.getCreatedAt());
        update.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toProductVO(update, category, requireUser(userId), images, soldCount(userId));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long productId) {
        Product product = requireOwnedProduct(userId, productId);
        if (!DELETABLE_STATUSES.contains(product.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "请先下架商品再删除");
        }
        ensureNoActiveOrder(productId);
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getProductId, productId));
        productMapper.deleteById(productId);
    }

    @Override
    @Transactional
    public ProductStatusVO changeShelf(Long userId, Long productId, boolean onShelf) {
        Product product = requireOwnedProduct(userId, productId);
        ensureNoActiveOrder(productId);
        String target;
        if (onShelf) {
            if (!ProductStatusEnum.OFF_SHELF.name().equals(product.getStatus())) {
                throw new BusinessException(ResultCode.CONFLICT, "只有已下架商品可以重新上架");
            }
            target = ProductStatusEnum.ON_SALE.name();
        } else {
            if (!ProductStatusEnum.ON_SALE.name().equals(product.getStatus())) {
                throw new BusinessException(ResultCode.CONFLICT, "只有在售商品可以下架");
            }
            target = ProductStatusEnum.OFF_SHELF.name();
        }
        Product update = new Product();
        update.setId(productId);
        update.setStatus(target);
        update.setPublishedAt(onShelf ? OffsetDateTime.now(ZoneOffset.UTC) : product.getPublishedAt());
        productMapper.updateById(update);
        return new ProductStatusVO(productId, target);
    }

    private PageResult<ProductVO> selectPage(ProductSearchDTO query, LambdaQueryWrapper<Product> wrapper) {
        Page<Product> page = productMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<ProductVO> records = enrichProducts(page.getRecords());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private LambdaQueryWrapper<Product> baseSearchWrapper(ProductSearchDTO query) {
        validatePriceRange(query.getMinPrice(), query.getMaxPrice());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(Product::getTitle, keyword).or().like(Product::getDescription, keyword));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        } else if (query.getParentCategoryId() != null) {
            List<Long> categoryIds = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                            .eq(Category::getStatus, "ACTIVE")
                            .eq(Category::getParentId, query.getParentCategoryId()))
                    .stream().map(Category::getId).collect(Collectors.toCollection(ArrayList::new));
            categoryIds.add(query.getParentCategoryId());
            wrapper.in(Product::getCategoryId, categoryIds);
        }
        if (query.getMinPrice() != null) wrapper.ge(Product::getPrice, query.getMinPrice());
        if (query.getMaxPrice() != null) wrapper.le(Product::getPrice, query.getMaxPrice());
        if (hasText(query.getConditionLevel())) wrapper.eq(Product::getConditionLevel, query.getConditionLevel());
        if (hasText(query.getTradeType())) wrapper.eq(Product::getTradeType, query.getTradeType());
        switch (query.getSort() == null ? "latest" : query.getSort()) {
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice).orderByDesc(Product::getCreatedAt);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice).orderByDesc(Product::getCreatedAt);
            default -> wrapper.orderByDesc(Product::getCreatedAt);
        }
        return wrapper;
    }

    private List<ProductVO> enrichProducts(List<Product> products) {
        if (products.isEmpty()) return List.of();
        Set<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        Set<Long> categoryIds = products.stream().map(Product::getCategoryId).collect(Collectors.toSet());
        Set<Long> sellerIds = products.stream().map(Product::getSellerId).collect(Collectors.toSet());

        Map<Long, List<ProductImage>> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds)
                        .orderByAsc(ProductImage::getSortOrder))
                .stream().collect(Collectors.groupingBy(ProductImage::getProductId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Category> categories = categoryMapper.selectByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, User> sellers = userMapper.selectByIds(sellerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Long> soldCounts = new HashMap<>();
        return products.stream().map(product -> toProductVO(product, categories.get(product.getCategoryId()),
                sellers.get(product.getSellerId()), images.getOrDefault(product.getId(), List.of()),
                soldCounts.computeIfAbsent(product.getSellerId(), this::soldCount))).toList();
    }

    private ProductVO buildDetail(Product product) {
        Category category = categoryMapper.selectById(product.getCategoryId());
        User seller = requireUser(product.getSellerId());
        List<ProductImage> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, product.getId()).orderByAsc(ProductImage::getSortOrder));
        return toProductVO(product, category, seller, images, soldCount(seller.getId()));
    }

    private ProductVO toProductVO(Product product, Category category, User seller,
                                  List<ProductImage> images, long sellerSoldCount) {
        List<ProductImageVO> imageVOs = images.stream().map(image -> new ProductImageVO(image.getId(),
                image.getImageUrl(), Boolean.TRUE.equals(image.getIsMain()), image.getSortOrder())).toList();
        String mainImage = imageVOs.stream().filter(ProductImageVO::isMain).map(ProductImageVO::url).findFirst()
                .orElseGet(() -> imageVOs.isEmpty() ? null : imageVOs.get(0).url());
        SellerVO sellerVO = seller == null ? null : new SellerVO(seller.getId(), seller.getNickname(),
                seller.getAvatarUrl(), sellerSoldCount);
        return new ProductVO(product.getId(), product.getTitle(), product.getDescription(), mainImage,
                product.getPrice(), product.getOriginalPrice(), product.getConditionLevel(), product.getTradeType(),
                product.getTradeLocation(), product.getStatus(), product.getViewCount(), product.getFavoriteCount(),
                product.getCategoryId(), category == null ? null : category.getName(), imageVOs, sellerVO,
                product.getCreatedAt(), product.getUpdatedAt());
    }

    private List<ProductImage> replaceImages(Long productId, List<ProductImageDTO> imageDTOs) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        List<ProductImageDTO> ordered = new ArrayList<>(imageDTOs);
        ordered.sort(Comparator.comparing(item -> item.sort() == null ? Integer.MAX_VALUE : item.sort()));
        boolean explicitMain = ordered.stream().anyMatch(item -> Boolean.TRUE.equals(item.isMain()));
        List<ProductImage> images = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            ProductImageDTO dto = ordered.get(index);
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setImageUrl(dto.url().trim());
            image.setSortOrder(index + 1);
            image.setIsMain(explicitMain ? Boolean.TRUE.equals(dto.isMain()) : index == 0);
            productImageMapper.insert(image);
            images.add(image);
        }
        return images;
    }

    private void applyProduct(Product product, Long userId, ProductPublishDTO dto) {
        product.setSellerId(userId);
        product.setCategoryId(dto.categoryId());
        product.setTitle(dto.title().trim());
        product.setDescription(dto.description().trim());
        product.setPrice(dto.price());
        product.setOriginalPrice(dto.originalPrice());
        product.setConditionLevel(dto.conditionLevel());
        product.setTradeType(dto.tradeType());
        product.setTradeLocation(hasText(dto.tradeRemark()) ? dto.tradeRemark().trim() : null);
    }

    private void validatePublish(ProductPublishDTO dto) {
        if (dto.originalPrice() != null && dto.originalPrice().compareTo(dto.price()) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品原价不能低于售价");
        }
        long mainCount = dto.images().stream().filter(item -> Boolean.TRUE.equals(item.isMain())).count();
        if (mainCount > 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能设置一张商品主图");
        }
        Set<Integer> sorts = new HashSet<>();
        for (ProductImageDTO image : dto.images()) {
            if (image.sort() != null && !sorts.add(image.sort())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "图片排序不能重复");
            }
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "最低价格不能高于最高价格");
        }
    }

    private void enforcePublishLimits(Long userId) {
        long activeCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, userId).in(Product::getStatus, ACTIVE_PRODUCT_STATUSES));
        if (activeCount >= ACTIVE_PUBLISH_LIMIT) {
            throw new BusinessException(ResultCode.CONFLICT, "同时在售和待审核商品不能超过50件");
        }
        OffsetDateTime todayStart = LocalDate.now(ZoneId.of("Asia/Shanghai"))
                .atStartOfDay(ZoneId.of("Asia/Shanghai")).toOffsetDateTime();
        long todayCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, userId).ge(Product::getCreatedAt, todayStart));
        if (todayCount >= DAILY_PUBLISH_LIMIT) {
            throw new BusinessException(ResultCode.CONFLICT, "每天最多发布5件商品");
        }
    }

    private Category requireActiveCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !"ACTIVE".equals(category.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品类目不存在或已停用");
        }
        return category;
    }

    private User requireEligibleSeller(Long userId) {
        User user = requireUser(userId);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账号不可发布商品");
        }
        if (!hasText(user.getStudentId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "请先完成校园身份认证");
        }
        return user;
    }

    private User requireUser(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private Product requireOwnedProduct(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (!product.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己发布的商品");
        }
        return product;
    }

    private long soldCount(Long sellerId) {
        return productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId).eq(Product::getStatus, ProductStatusEnum.SOLD.name()));
    }

    private void ensureNoActiveOrder(Long productId) {
        long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getProductId, productId).ne(Order::getStatus, "CANCELLED"));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "商品存在进行中的订单，暂不可修改、下架或删除");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
