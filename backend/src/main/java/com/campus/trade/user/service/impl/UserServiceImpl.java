package com.campus.trade.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.trade.common.constant.Constant;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.common.utils.JwtUtils;
import com.campus.trade.entity.User;
import com.campus.trade.entity.UserAddress;
import com.campus.trade.entity.UserAuth;
import com.campus.trade.mapper.UserAddressMapper;
import com.campus.trade.mapper.UserAuthMapper;
import com.campus.trade.mapper.UserMapper;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserServiceImpl implements UserService {
    private static final String MVP_SMS_CODE = "888888";
    private static final Duration SMS_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ADDRESS_COUNT = 10;

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserAddressMapper userAddressMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final Map<String, Instant> smsSentAt = new ConcurrentHashMap<>();

    public UserServiceImpl(UserMapper userMapper, UserAuthMapper userAuthMapper,
                           UserAddressMapper userAddressMapper, PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.userAuthMapper = userAuthMapper;
        this.userAddressMapper = userAddressMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void sendSmsCode(SmsSendDTO dto) {
        String key = dto.phone() + ':' + dto.scene().toUpperCase(Locale.ROOT);
        Instant now = Instant.now();
        Instant previous = smsSentAt.putIfAbsent(key, now);
        if (previous != null && Duration.between(previous, now).compareTo(SMS_COOLDOWN) < 0) {
            throw new BusinessException(ResultCode.CONFLICT, "验证码发送过于频繁，请60秒后重试");
        }
        smsSentAt.put(key, now);
    }

    @Override
    @Transactional
    public AuthResponseVO register(UserRegisterDTO dto) {
        verifySmsCode(dto.smsCode());
        ensurePhoneAvailable(dto.phone(), null);
        if (hasText(dto.studentId())) {
            ensureStudentIdAvailable(dto.studentId(), null);
        }

        User user = new User();
        user.setPhone(dto.phone());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setNickname(hasText(dto.nickname()) ? dto.nickname().trim() : defaultNickname(dto.phone()));
        user.setRole(Constant.ROLE_USER);
        user.setStatus("ACTIVE");
        if (hasText(dto.studentId())) {
            user.setStudentId(dto.studentId().trim());
            user.setRealName(dto.realName().trim());
        }

        try {
            userMapper.insert(user);
            insertAuth(user.getId(), "PHONE", "VERIFIED", dto.phone());
            if (hasText(dto.studentId())) {
                insertAuth(user.getId(), "STUDENT_ID", "VERIFIED", dto.studentId().trim());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号或学号已被注册");
        }
        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseVO login(UserLoginDTO dto) {
        User user = findByPhone(dto.phone());
        if (user == null || !passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "手机号或密码错误");
        }
        ensureUserCanLogin(user);
        updateLastLogin(user);
        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseVO loginBySms(SmsLoginDTO dto) {
        verifySmsCode(dto.smsCode());
        User user = findByPhone(dto.phone());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        ensureUserCanLogin(user);
        updateLastLogin(user);
        return toAuthResponse(user);
    }

    @Override
    public UserVO getProfile(Long userId) {
        return toUserVO(getRequiredUser(userId));
    }

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, ProfileUpdateDTO dto) {
        User current = getRequiredUser(userId);
        User update = new User();
        update.setId(userId);
        if (dto.nickname() != null) {
            update.setNickname(dto.nickname().trim());
            current.setNickname(update.getNickname());
        }
        if (dto.avatar() != null) {
            update.setAvatarUrl(dto.avatar().trim());
            current.setAvatarUrl(update.getAvatarUrl());
        }
        if (dto.contactPhone() != null) {
            update.setContactPhone(dto.contactPhone());
            current.setContactPhone(update.getContactPhone());
        }
        userMapper.updateById(update);
        return toUserVO(current);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        User user = getRequiredUser(userId);
        if (!passwordEncoder.matches(dto.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码错误");
        }
        if (passwordEncoder.matches(dto.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.CONFLICT, "新密码不能与原密码相同");
        }
        User update = new User();
        update.setId(userId);
        update.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        userMapper.updateById(update);
    }

    @Override
    @Transactional
    public UserVO changePhone(Long userId, PhoneChangeDTO dto) {
        verifySmsCode(dto.smsCode());
        User user = getRequiredUser(userId);
        if (dto.newPhone().equals(user.getPhone())) {
            throw new BusinessException(ResultCode.CONFLICT, "新手机号不能与原手机号相同");
        }
        ensurePhoneAvailable(dto.newPhone(), userId);
        User update = new User();
        update.setId(userId);
        update.setPhone(dto.newPhone());
        try {
            userMapper.updateById(update);
            insertAuth(userId, "PHONE", "VERIFIED", dto.newPhone());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号已被注册");
        }
        user.setPhone(dto.newPhone());
        return toUserVO(user);
    }

    @Override
    @Transactional
    public StudentAuthVO authenticateStudent(Long userId, StudentAuthDTO dto) {
        User user = getRequiredUser(userId);
        ensureStudentIdAvailable(dto.studentId(), userId);

        User update = new User();
        update.setId(userId);
        update.setStudentId(dto.studentId().trim());
        update.setRealName(dto.realName().trim());
        try {
            userMapper.updateById(update);
            if (!dto.studentId().trim().equals(user.getStudentId())) {
                insertAuth(userId, "STUDENT_ID", "VERIFIED", dto.studentId().trim());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "学号已被其他账号认证");
        }
        user.setStudentId(update.getStudentId());
        user.setRealName(update.getRealName());
        return new StudentAuthVO("VERIFIED");
    }

    @Override
    public List<AddressVO> listAddresses(Long userId) {
        getRequiredUser(userId);
        return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getUpdatedAt))
                .stream().map(this::toAddressVO).toList();
    }

    @Override
    @Transactional
    public AddressVO createAddress(Long userId, AddressDTO dto) {
        getRequiredUser(userId);
        long count = userAddressMapper.selectCount(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId));
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ResultCode.CONFLICT, "最多只能保存10个收货地址");
        }
        boolean isDefault = Boolean.TRUE.equals(dto.isDefault()) || count == 0;
        if (isDefault) {
            clearDefaultAddresses(userId);
        }
        UserAddress address = new UserAddress();
        applyAddress(address, userId, dto, isDefault);
        userAddressMapper.insert(address);
        return toAddressVO(address);
    }

    @Override
    @Transactional
    public AddressVO updateAddress(Long userId, Long addressId, AddressDTO dto) {
        UserAddress current = getRequiredAddress(userId, addressId);
        boolean isDefault = dto.isDefault() == null ? Boolean.TRUE.equals(current.getIsDefault())
                : Boolean.TRUE.equals(dto.isDefault());
        if (isDefault) {
            clearDefaultAddresses(userId);
        }
        UserAddress update = new UserAddress();
        update.setId(addressId);
        applyAddress(update, userId, dto, isDefault);
        userAddressMapper.updateById(update);
        update.setCreatedAt(current.getCreatedAt());
        return toAddressVO(update);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = getRequiredAddress(userId, addressId);
        userAddressMapper.deleteById(addressId);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            UserAddress replacement = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getUpdatedAt)
                    .last("LIMIT 1"));
            if (replacement != null) {
                setDefaultAddress(userId, replacement.getId());
            }
        }
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        getRequiredAddress(userId, addressId);
        clearDefaultAddresses(userId);
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, true));
    }

    private void applyAddress(UserAddress address, Long userId, AddressDTO dto, boolean isDefault) {
        String detail = hasText(dto.detail()) ? dto.detail().trim() : buildDetail(dto);
        address.setUserId(userId);
        address.setContactName(dto.contactName().trim());
        address.setContactPhone(dto.contactPhone());
        address.setCampus(hasText(dto.campus()) ? dto.campus().trim() : "校内");
        address.setBuilding(hasText(dto.building()) ? dto.building().trim() : detail);
        address.setRoom(hasText(dto.room()) ? dto.room().trim() : null);
        address.setDetail(detail);
        address.setIsDefault(isDefault);
    }

    private String buildDetail(AddressDTO dto) {
        StringBuilder builder = new StringBuilder(dto.campus().trim()).append(' ').append(dto.building().trim());
        if (hasText(dto.room())) {
            builder.append(' ').append(dto.room().trim());
        }
        return builder.toString();
    }

    private void clearDefaultAddresses(Long userId) {
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, true)
                .set(UserAddress::getIsDefault, false));
    }

    private User getRequiredUser(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserAddress getRequiredAddress(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    private User findByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    private void ensurePhoneAvailable(String phone, Long currentUserId) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().eq(User::getPhone, phone);
        if (currentUserId != null) {
            query.ne(User::getId, currentUserId);
        }
        if (userMapper.selectCount(query) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号已被注册");
        }
    }

    private void ensureStudentIdAvailable(String studentId, Long currentUserId) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().eq(User::getStudentId, studentId);
        if (currentUserId != null) {
            query.ne(User::getId, currentUserId);
        }
        if (userMapper.selectCount(query) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "学号已被其他账号认证");
        }
    }

    private void insertAuth(Long userId, String type, String status, String identifier) {
        UserAuth auth = new UserAuth();
        auth.setUserId(userId);
        auth.setAuthType(type);
        auth.setAuthStatus(status);
        auth.setIdentifier(identifier);
        if ("VERIFIED".equals(status)) {
            auth.setVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        userAuthMapper.insert(auth);
    }

    private void updateLastLogin(User user) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        User update = new User();
        update.setId(user.getId());
        update.setLastLoginAt(now);
        userMapper.updateById(update);
        user.setLastLoginAt(now);
    }

    private void ensureUserCanLogin(User user) {
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号当前不可登录");
        }
    }

    private void verifySmsCode(String code) {
        if (!MVP_SMS_CODE.equals(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "短信验证码错误或已过期");
        }
    }

    private AuthResponseVO toAuthResponse(User user) {
        return new AuthResponseVO(user.getId(), maskPhone(user.getPhone()), user.getNickname(),
                user.getRole(), user.getStatus(), hasText(user.getStudentId()),
                jwtUtils.generateToken(user.getId(), user.getRole()));
    }

    private UserVO toUserVO(User user) {
        return new UserVO(user.getId(), maskPhone(user.getPhone()), user.getNickname(), user.getAvatarUrl(),
                user.getContactPhone(), user.getStudentId(), user.getRealName(), user.getRole(), user.getStatus(),
                hasText(user.getStudentId()), user.getCreatedAt());
    }

    private AddressVO toAddressVO(UserAddress address) {
        return new AddressVO(address.getId(), address.getContactName(), address.getContactPhone(),
                address.getCampus(), address.getBuilding(), address.getRoom(), address.getDetail(),
                Boolean.TRUE.equals(address.getIsDefault()), address.getCreatedAt());
    }

    private String defaultNickname(String phone) {
        return "校园用户" + phone.substring(phone.length() - 4);
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
