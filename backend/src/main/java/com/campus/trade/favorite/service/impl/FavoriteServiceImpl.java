package com.campus.trade.favorite.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Favorite;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductImage;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.favorite.dto.FavoriteQueryDTO;
import com.campus.trade.favorite.service.FavoriteService;
import com.campus.trade.favorite.vo.FavoriteProductVO;
import com.campus.trade.favorite.vo.FavoriteStatusVO;
import com.campus.trade.favorite.vo.FavoriteVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, ProductMapper productMapper,
                               ProductImageMapper productImageMapper, CategoryMapper categoryMapper) {
        this.favoriteMapper = favoriteMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public FavoriteStatusVO toggle(Long userId, Long productId) {
        Favorite existing = find(userId, productId);
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            productMapper.decrementFavoriteCount(productId);
            return new FavoriteStatusVO(false);
        }
        Product product = requireFavoritableProduct(productId);
        if (userId.equals(product.getSellerId())) {
            throw new BusinessException(ResultCode.CONFLICT, "不能收藏自己发布的商品");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);
        productMapper.incrementFavoriteCount(productId);
        return new FavoriteStatusVO(true);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long productId) {
        Favorite existing = find(userId, productId);
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            productMapper.decrementFavoriteCount(productId);
        }
    }

    @Override
    public FavoriteStatusVO check(Long userId, Long productId) {
        return new FavoriteStatusVO(find(userId, productId) != null);
    }

    @Override
    public PageResult<FavoriteVO> list(Long userId, FavoriteQueryDTO query) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId);
        if (query.getCategoryId() != null) {
            List<Long> productIds = productMapper.selectList(new LambdaQueryWrapper<Product>()
                            .eq(Product::getCategoryId, query.getCategoryId()))
                    .stream().map(Product::getId).toList();
            if (productIds.isEmpty()) {
                return new PageResult<>(List.of(), 0, query.getPage(), query.getSize(), 0);
            }
            wrapper.in(Favorite::getProductId, productIds);
        }
        wrapper.orderByDesc(Favorite::getCreatedAt);
        Page<Favorite> page = favoriteMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<>(enrich(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private List<FavoriteVO> enrich(List<Favorite> favorites) {
        if (favorites.isEmpty()) return List.of();
        Set<Long> productIds = favorites.stream().map(Favorite::getProductId).collect(Collectors.toSet());
        Map<Long, Product> products = productMapper.selectByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Set<Long> categoryIds = products.values().stream().map(Product::getCategoryId).collect(Collectors.toSet());
        Map<Long, Category> categories = categoryIds.isEmpty() ? Map.of() : categoryMapper.selectByIds(categoryIds)
                .stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, String> images = mainImages(productIds);
        return favorites.stream().filter(item -> products.containsKey(item.getProductId())).map(item -> {
            Product product = products.get(item.getProductId());
            Category category = categories.get(product.getCategoryId());
            FavoriteProductVO productVO = new FavoriteProductVO(product.getId(), product.getTitle(),
                    images.get(product.getId()), product.getPrice(), product.getStatus(), product.getCategoryId(),
                    category == null ? null : category.getName(), product.getCreatedAt());
            return new FavoriteVO(item.getId(), productVO, item.getCreatedAt());
        }).toList();
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

    private Favorite find(Long userId, Long productId) {
        return favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId));
    }

    private Product requireFavoritableProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "商品当前不可收藏");
        }
        return product;
    }
}
