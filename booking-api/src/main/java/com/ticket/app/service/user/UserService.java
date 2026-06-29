package com.ticket.app.service.user;

import com.ticket.app.controller.user.dto.ChangePasswordRequest;
import com.ticket.app.controller.user.dto.UpdateUserRequest;
import com.ticket.app.controller.user.dto.UserLoginResponse;
import com.ticket.app.controller.user.dto.UserRegisterRequest;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户ID
     */
    Long register(UserRegisterRequest request);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @param loginIp 登录IP
     * @return 登录响应(含Token)
     */
    UserLoginResponse login(String username, String password, String loginIp);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserLoginResponse getUserById(Long userId);

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    UserLoginResponse getUserByUsername(String username);

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    UserLoginResponse updateUser(Long userId, UpdateUserRequest request);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 用户登出
     *
     * @param userId 用户ID
     */
    void logout(Long userId);
}