package com.campus.trade.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterDTO(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
        @JsonAlias("code")
        @NotBlank(message = "短信验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "短信验证码必须为6位数字")
        String smsCode,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度必须为6-20位")
        String password,
        @Size(min = 1, max = 50, message = "昵称长度必须为1-50位")
        String nickname,
        @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "学号格式不正确")
        String studentId,
        @Size(min = 2, max = 50, message = "姓名长度必须为2-50位")
        String realName
) {
    @AssertTrue(message = "学号和姓名必须同时填写")
    public boolean isStudentIdentityComplete() {
        return (studentId == null || studentId.isBlank()) == (realName == null || realName.isBlank());
    }
}
