package com.campus.trade.product.service;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.product.dto.ProductPublishDTO;
import com.campus.trade.product.dto.ProductSearchDTO;
import com.campus.trade.product.vo.ProductStatusVO;
import com.campus.trade.product.vo.ProductVO;

public interface ProductService {
    ProductVO publish(Long userId, ProductPublishDTO dto);

    ProductVO getDetail(Long productId, Long viewerId);

    PageResult<ProductVO> list(ProductSearchDTO query);

    PageResult<ProductVO> listMine(Long userId, ProductSearchDTO query);

    ProductVO update(Long userId, Long productId, ProductPublishDTO dto);

    void delete(Long userId, Long productId);

    ProductStatusVO changeShelf(Long userId, Long productId, boolean onShelf);
}
