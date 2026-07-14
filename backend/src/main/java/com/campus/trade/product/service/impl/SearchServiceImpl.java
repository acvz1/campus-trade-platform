package com.campus.trade.product.service.impl;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.product.dto.ProductSearchDTO;
import com.campus.trade.product.service.ProductService;
import com.campus.trade.product.service.SearchService;
import com.campus.trade.product.vo.ProductVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    private static final List<String> HOT_KEYWORDS = List.of("教材", "自行车", "耳机", "台灯", "考研资料", "显示器");
    private final ProductService productService;

    public SearchServiceImpl(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public PageResult<ProductVO> search(ProductSearchDTO query) {
        return productService.list(query);
    }

    @Override
    public List<String> hotKeywords() {
        return HOT_KEYWORDS;
    }
}
