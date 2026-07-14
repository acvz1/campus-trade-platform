package com.campus.trade.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateDTO(
        @Size(min = 1, max = 50, message = "昵称长度必须为1-50位")
        String nickname,
        @Size(max = 500, message = "头像地址过长")
        String avatar,
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
        String contactPhone
) {
}
