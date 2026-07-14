package com.campus.trade.user.controller;

import com.campus.trade.common.result.Result;
import com.campus.trade.security.AuthenticatedUser;
import com.campus.trade.user.dto.AddressDTO;
import com.campus.trade.user.dto.PasswordChangeDTO;
import com.campus.trade.user.dto.PhoneChangeDTO;
import com.campus.trade.user.dto.ProfileUpdateDTO;
import com.campus.trade.user.dto.SmsLoginDTO;
import com.campus.trade.user.dto.SmsSendDTO;
import com.campus.trade.user.dto.StudentAuthDTO;
import com.campus.trade.user.dto.UserLoginDTO;
import com.campus.trade.user.dto.UserRegisterDTO;
import com.campus.trade.user.service.UserService;
import com.campus.trade.user.vo.AddressVO;
import com.campus.trade.user.vo.AuthResponseVO;
import com.campus.trade.user.vo.StudentAuthVO;
import com.campus.trade.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户")
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "发送短信验证码（MVP固定为888888）")
    @PostMapping({"/sms/send", "/send-code"})
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsSendDTO dto) {
        userService.sendSmsCode(dto);
        return Result.success();
    }

    @Operation(summary = "手机号注册")
    @PostMapping("/register")
    public Result<AuthResponseVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @Operation(summary = "手机号密码登录")
    @PostMapping("/login")
    public Result<AuthResponseVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @Operation(summary = "短信验证码登录")
    @PostMapping("/login/sms")
    public Result<AuthResponseVO> loginBySms(@Valid @RequestBody SmsLoginDTO dto) {
        return Result.success(userService.loginBySms(dto));
    }

    @Operation(summary = "获取当前用户资料")
    @GetMapping({"/profile", "/info"})
    public Result<UserVO> getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(userService.getProfile(user.userId()));
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping({"/profile", "/info"})
    public Result<UserVO> updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody ProfileUpdateDTO dto) {
        return Result.success(userService.updateProfile(user.userId(), dto));
    }

    @Operation(summary = "修改登录密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody PasswordChangeDTO dto) {
        userService.changePassword(user.userId(), dto);
        return Result.success();
    }

    @Operation(summary = "更换绑定手机号")
    @PutMapping("/phone")
    public Result<UserVO> changePhone(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody PhoneChangeDTO dto) {
        return Result.success(userService.changePhone(user.userId(), dto));
    }

    @Operation(summary = "学号实名认证")
    @PostMapping("/auth/student")
    public Result<StudentAuthVO> authenticateStudent(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @Valid @RequestBody StudentAuthDTO dto) {
        return Result.success(userService.authenticateStudent(user.userId(), dto));
    }

    @Operation(summary = "收货地址列表")
    @GetMapping("/address")
    public Result<List<AddressVO>> listAddresses(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(userService.listAddresses(user.userId()));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping("/address")
    public Result<AddressVO> createAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody AddressDTO dto) {
        return Result.success(userService.createAddress(user.userId(), dto));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping("/address/{id}")
    public Result<AddressVO> updateAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        return Result.success(userService.updateAddress(user.userId(), id, dto));
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/address/{id}")
    public Result<Void> deleteAddress(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        userService.deleteAddress(user.userId(), id);
        return Result.success();
    }

    @Operation(summary = "设为默认收货地址")
    @PutMapping("/address/{id}/default")
    public Result<Void> setDefaultAddress(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        userService.setDefaultAddress(user.userId(), id);
        return Result.success();
    }
}
