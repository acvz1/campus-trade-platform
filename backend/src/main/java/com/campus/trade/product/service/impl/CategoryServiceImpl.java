package com.campus.trade.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.entity.Category;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.product.service.CategoryService;
import com.campus.trade.product.vo.CategoryVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryVO> getCategoryTree() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, "ACTIVE")
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        Map<Long, List<CategoryVO>> children = new LinkedHashMap<>();
        for (Category category : categories) {
            if (category.getParentId() != null) {
                children.computeIfAbsent(category.getParentId(), ignored -> new ArrayList<>())
                        .add(toVO(category, List.of()));
            }
        }
        return categories.stream()
                .filter(category -> category.getParentId() == null)
                .map(category -> toVO(category, children.getOrDefault(category.getId(), List.of())))
                .toList();
    }

    private CategoryVO toVO(Category category, List<CategoryVO> children) {
        return new CategoryVO(category.getId(), category.getParentId(), category.getName(), category.getIcon(),
                category.getSortOrder(), children);
    }
}
