package com.campus.trade.user.service;

import com.campus.trade.user.dto.AddressDTO;
import com.campus.trade.user.dto.PasswordChangeDTO;
import com.campus.trade.user.dto.PhoneChangeDTO;
import com.campus.trade.user.dto.ProfileUpdateDTO;
import com.campus.trade.user.dto.SmsLoginDTO;
import com.campus.trade.user.dto.SmsSendDTO;
import com.campus.trade.user.dto.StudentAuthDTO;
import com.campus.trade.user.dto.UserLoginDTO;
import com.campus.trade.user.dto.UserRegisterDTO;
import com.campus.trade.user.vo.AddressVO;
import com.campus.trade.user.vo.AuthResponseVO;
import com.campus.trade.user.vo.StudentAuthVO;
import com.campus.trade.user.vo.UserVO;

import java.util.List;

public interface UserService {
    void sendSmsCode(SmsSendDTO dto);

    AuthResponseVO register(UserRegisterDTO dto);

    AuthResponseVO login(UserLoginDTO dto);

    AuthResponseVO loginBySms(SmsLoginDTO dto);

    UserVO getProfile(Long userId);

    UserVO updateProfile(Long userId, ProfileUpdateDTO dto);

    void changePassword(Long userId, PasswordChangeDTO dto);

    UserVO changePhone(Long userId, PhoneChangeDTO dto);

    StudentAuthVO authenticateStudent(Long userId, StudentAuthDTO dto);

    List<AddressVO> listAddresses(Long userId);

    AddressVO createAddress(Long userId, AddressDTO dto);

    AddressVO updateAddress(Long userId, Long addressId, AddressDTO dto);

    void deleteAddress(Long userId, Long addressId);

    void setDefaultAddress(Long userId, Long addressId);
}
