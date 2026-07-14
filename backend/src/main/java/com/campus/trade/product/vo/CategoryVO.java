package com.campus.trade.product.vo;

import java.util.List;

public record CategoryVO(
        Long id,
        Long parentId,
        String name,
        String icon,
        Integer sortOrder,
        List<CategoryVO> children
) {
}
