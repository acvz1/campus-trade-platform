package com.campus.trade.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageDTO(
        @NotBlank(message = "图片地址不能为空")
        @Size(max = 500, message = "图片地址过长")
        String url,
        Boolean isMain,
        @Min(value = 1, message = "图片排序最小为1")
        @Max(value = 9, message = "图片排序最大为9")
        Integer sort
) {
}
