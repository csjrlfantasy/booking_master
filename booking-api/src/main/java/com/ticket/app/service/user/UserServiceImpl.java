package com.ticket.app.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.common.util.PasswordUtils;
import com.ticket.app.controller.user.dto.ChangePasswordRequest;
import com.ticket.app.controller.user.dto.UpdateUserRequest;
import com.ticket.app.controller.user.dto.UserLoginResponse;
import com.ticket.app.controller.user.dto.UserRegisterRequest;
import com.ticket.app.controller.user.dto.UserTokenResponse;
import com.ticket.app.entity.User;
import com.ticket.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration = 86400000L;

    private Long getTokenExpirationSeconds() {
        return jwtExpiration / 1000;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, request.getUsername());
        Long existCount = userRepository.selectCount(usernameWrapper);
        if (existCount > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "用户名已存在");
        }

        // 检查手机号是否已存在
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, request.getPhone());
        Long phoneCount = userRepository.selectCount(phoneWrapper);
        if (phoneCount > 0) {
            throw new BusinessException(ResultCode.USER_PHONE_EXISTS, "手机号已被注册");
        }

        // 检查邮箱是否已存在(如果提供了邮箱)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(User::getEmail, request.getEmail());
            Long emailCount = userRepository.selectCount(emailWrapper);
            if (emailCount > 0) {
                throw new BusinessException(ResultCode.USER_EMAIL_EXISTS, "邮箱已被注册");
            }
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtils.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setUserType(User.USER_TYPE_NORMAL);
        user.setUserLevel(1);
        user.setStatus(User.STATUS_NORMAL);
        user.setSource(request.getSource());
        user.setInviterId(request.getInviterId());
        user.setLoginFailCount(0);
        user.setWechatBalance(new BigDecimal("10000.00"));
        user.setAlipayBalance(new BigDecimal("10000.00"));

        userRepository.insert(user);
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        return user.getId();
    }

    @Override
    public UserLoginResponse login(String username, String password, String loginIp) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userRepository.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 检查用户状态
        if (User.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED, "用户已被禁用");
        }

        // 检查账户是否被锁定
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.USER_LOGIN_LOCKED, "账号已被锁定，请30分钟后再试");
        }

        // 验证密码
        if (!PasswordUtils.matches(password, user.getPassword())) {
            // 记录登录失败次数
            handleLoginFail(user, loginIp);
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "用户名或密码错误");
        }

        // 检查密码错误锁定
        if (User.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED, "用户已被禁用");
        }

        // 登录成功，更新登录信息
        handleLoginSuccess(user, loginIp);

        // 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        // 构建响应
        UserTokenResponse response = new UserTokenResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setUserType(user.getUserType());
        response.setUserLevel(user.getUserLevel());
        response.setAvatar(user.getAvatar());
        response.setWechatBalance(user.getWechatBalance());
        response.setAlipayBalance(user.getAlipayBalance());
        response.setToken(token);
        response.setExpiresIn(getTokenExpirationSeconds());

        log.info("用户登录成功: userId={}, username={}, ip={}", user.getId(), user.getUsername(), loginIp);

        return response;
    }

    /**
     * 处理登录失败
     */
    private void handleLoginFail(User user, String loginIp) {
        int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
        user.setLoginFailCount(failCount);

        // 连续失败5次，锁定30分钟
        if (failCount >= 5) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("用户登录失败次数过多，账户已被锁定: userId={}, failCount={}", user.getId(), failCount);
        }

        userRepository.updateById(user);
        log.warn("用户登录失败: userId={}, username={}, ip={}, failCount={}",
                user.getId(), user.getUsername(), loginIp, failCount);
    }

    /**
     * 处理登录成功
     */
    private void handleLoginSuccess(User user, String loginIp) {
        user.setLoginFailCount(0);
        user.setLockUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        userRepository.updateById(user);
    }

    @Override
    public UserLoginResponse getUserById(Long userId) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        return buildUserLoginResponse(user);
    }

    @Override
    public UserLoginResponse getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userRepository.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        return buildUserLoginResponse(user);
    }

    /**
     * 构建用户登录响应
     */
    private UserLoginResponse buildUserLoginResponse(User user) {
        UserLoginResponse response = new UserLoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setUserType(user.getUserType());
        response.setUserLevel(user.getUserLevel());
        response.setAvatar(user.getAvatar());
        response.setWechatBalance(user.getWechatBalance());
        response.setAlipayBalance(user.getAlipayBalance());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 检查手机号是否被其他用户使用
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(User::getPhone, request.getPhone());
            phoneWrapper.ne(User::getId, userId);
            Long count = userRepository.selectCount(phoneWrapper);
            if (count > 0) {
                throw new BusinessException(ResultCode.USER_PHONE_EXISTS, "手机号已被其他用户使用");
            }
            user.setPhone(request.getPhone());
        }

        // 检查邮箱是否被其他用户使用
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(User::getEmail, request.getEmail());
            emailWrapper.ne(User::getId, userId);
            Long count = userRepository.selectCount(emailWrapper);
            if (count > 0) {
                throw new BusinessException(ResultCode.USER_EMAIL_EXISTS, "邮箱已被其他用户使用");
            }
            user.setEmail(request.getEmail());
        }

        // 更新其他字段
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            try {
                user.setBirthday(LocalDate.parse(request.getBirthday(), DateTimeFormatter.ISO_DATE));
            } catch (Exception e) {
                log.warn("生日格式解析失败: {}", request.getBirthday());
            }
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        userRepository.updateById(user);
        log.info("用户信息更新成功: userId={}", userId);

        return buildUserLoginResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 验证旧密码
        if (!PasswordUtils.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "旧密码不正确");
        }

        // 验证确认密码
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        // 检查新密码是否与旧密码相同
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与旧密码相同");
        }

        // 更新密码
        user.setPassword(PasswordUtils.encode(request.getNewPassword()));
        userRepository.updateById(user);

        log.info("用户密码修改成功: userId={}", userId);
    }

    @Override
    public void logout(Long userId) {
        log.info("用户登出: userId={}", userId);
        // JWT是无状态的，客户端直接删除Token即可
        // 这里可以添加Token黑名单等逻辑（如果使用Redis存储Token）
    }
}
