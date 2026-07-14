package com.campus.trade.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductPublishDTO(
        @NotNull(message = "请选择商品类目") Long categoryId,
        @NotBlank(message = "商品标题不能为空")
        @Size(min = 2, max = 50, message = "商品标题长度必须为2-50字")
        String title,
        @NotBlank(message = "商品描述不能为空")
        @Size(min = 10, max = 1000, message = "商品描述长度必须为10-1000字")
        String description,
        @NotNull(message = "请输入商品价格")
        @DecimalMin(value = "0.01", message = "商品价格必须大于0")
        @DecimalMax(value = "99999.99", message = "商品价格不能超过99999.99")
        @Digits(integer = 5, fraction = 2, message = "商品价格最多保留2位小数")
        BigDecimal price,
        @Positive(message = "商品原价必须大于0")
        @Digits(integer = 5, fraction = 2, message = "商品原价最多保留2位小数")
        BigDecimal originalPrice,
        @NotBlank(message = "请选择商品成色")
        @Pattern(regexp = "NEW|LIKE_NEW|USED|OLD", message = "商品成色不正确")
        String conditionLevel,
        @NotBlank(message = "请选择交易方式")
        @Pattern(regexp = "PICKUP|DELIVERY|BOTH", message = "交易方式不正确")
        String tradeType,
        @Size(max = 255, message = "交易备注不能超过255字")
        String tradeRemark,
        @NotNull(message = "请上传商品图片")
        @Size(min = 1, max = 9, message = "商品图片数量必须为1-9张")
        List<@Valid ProductImageDTO> images
) {
}
