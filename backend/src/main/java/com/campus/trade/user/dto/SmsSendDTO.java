package com.campus.trade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SmsSendDTO(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "验证码场景不能为空")
        @Pattern(regexp = "REGISTER|LOGIN|CHANGE_PHONE", message = "验证码场景不正确")
        String scene
) {
    public SmsSendDTO {
        if (scene == null || scene.isBlank()) {
            scene = "REGISTER";
        }
    }
}
