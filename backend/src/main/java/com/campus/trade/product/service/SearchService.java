package com.campus.trade.product.service;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.product.dto.ProductSearchDTO;
import com.campus.trade.product.vo.ProductVO;

import java.util.List;

public interface SearchService {
    PageResult<ProductVO> search(ProductSearchDTO query);

    List<String> hotKeywords();
}
