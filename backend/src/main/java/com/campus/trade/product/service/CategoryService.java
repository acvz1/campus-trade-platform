package com.campus.trade.product.service;

import com.campus.trade.product.vo.CategoryVO;

import java.util.List;

public interface CategoryService {
    List<CategoryVO> getCategoryTree();
}
