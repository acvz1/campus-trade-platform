package com.campus.trade.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchDTO {
    @Size(max = 100, message = "搜索关键词过长")
    private String keyword;
    private Long categoryId;
    private Long parentCategoryId;
    @DecimalMin(value = "0.00", message = "最低价格不能小于0")
    private BigDecimal minPrice;
    @DecimalMin(value = "0.00", message = "最高价格不能小于0")
    private BigDecimal maxPrice;
    @Pattern(regexp = "NEW|LIKE_NEW|USED|OLD", message = "商品成色不正确")
    private String conditionLevel;
    @Pattern(regexp = "PICKUP|DELIVERY|BOTH", message = "交易方式不正确")
    private String tradeType;
    @Pattern(regexp = "latest|price_asc|price_desc", message = "排序方式不正确")
    private String sort = "latest";
    @Min(value = 1, message = "页码最小为1")
    private int page = 1;
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private int size = 20;
    @Pattern(regexp = "PENDING_REVIEW|ON_SALE|REJECTED|OFF_SHELF|SOLD|VIOLATION_DELISTED|DELETED",
            message = "商品状态不正确")
    private String status;
}
