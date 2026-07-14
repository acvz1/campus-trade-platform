package com.campus.trade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentAuthDTO(
        @NotBlank(message = "学号不能为空")
        @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "学号格式不正确")
        String studentId,
        @NotBlank(message = "真实姓名不能为空")
        @Size(min = 2, max = 50, message = "姓名长度必须为2-50位")
        String realName
) {
}
