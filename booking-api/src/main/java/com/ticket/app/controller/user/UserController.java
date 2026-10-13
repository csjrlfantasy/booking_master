package com.ticket.app.controller.user;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.user.dto.ChangePasswordRequest;
import com.ticket.app.controller.user.dto.UpdateUserRequest;
import com.ticket.app.controller.user.dto.UserIdentityRequest;
import com.ticket.app.controller.user.dto.UserIdentityResponse;
import com.ticket.app.controller.user.dto.UserLoginRequest;
import com.ticket.app.controller.user.dto.UserLoginResponse;
import com.ticket.app.controller.user.dto.UserRegisterRequest;
import com.ticket.app.controller.user.dto.UserTokenResponse;
import com.ticket.app.service.user.UserIdentityService;
import com.ticket.app.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户管理", description = "用户注册、登录、资料和常用观演人接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserIdentityService userIdentityService;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public Result<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录")
    public Result<UserTokenResponse> login(@Valid @RequestBody UserLoginRequest request, HttpServletRequest httpRequest) {
        UserTokenResponse response = (UserTokenResponse) userService.login(
                request.getUsername(),
                request.getPassword(),
                getClientIp(httpRequest)
        );
        return Result.success(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户信息", description = "仅允许查看自己的用户信息")
    public Result<UserLoginResponse> getUserById(
            @PathVariable("userId") Long userId,
            HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromRequest(httpRequest);
        if (!currentUserId.equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问其他用户信息");
        }
        return Result.success(userService.getUserById(userId));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名获取用户信息", description = "根据用户名获取用户信息")
    public Result<UserLoginResponse> getUserByUsername(@PathVariable("username") String username) {
        return Result.success(userService.getUserByUsername(username));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的个人信息")
    public Result<UserLoginResponse> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        return Result.success("用户信息更新成功", userService.updateUser(userId, request));
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public Result<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        userService.changePassword(userId, request);
        return Result.success("密码修改成功", null);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出当前登录状态")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        userService.logout(userId);
        return Result.success("登出成功", null);
    }

    @GetMapping("/identities")
    @Operation(summary = "常用观演人列表", description = "获取当前用户保存的常用观演人身份证")
    public Result<List<UserIdentityResponse>> listIdentities(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        return Result.success(userIdentityService.list(userId));
    }

    @PostMapping("/identities")
    @Operation(summary = "新增常用观演人", description = "保存常用观演人身份证")
    public Result<UserIdentityResponse> createIdentity(
            @Valid @RequestBody UserIdentityRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        return Result.success("观演人已保存", userIdentityService.create(userId, request));
    }

    @PutMapping("/identities/{identityId}")
    @Operation(summary = "更新常用观演人", description = "更新常用观演人身份证")
    public Result<UserIdentityResponse> updateIdentity(
            @PathVariable Long identityId,
            @Valid @RequestBody UserIdentityRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        return Result.success("观演人已更新", userIdentityService.update(userId, identityId, request));
    }

    @DeleteMapping("/identities/{identityId}")
    @Operation(summary = "删除常用观演人", description = "删除当前用户保存的常用观演人")
    public Result<Void> deleteIdentity(
            @PathVariable Long identityId,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        userIdentityService.delete(userId, identityId);
        return Result.success("观演人已删除", null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "请先登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new BusinessException(ResultCode.TOKEN_INVALID, "Token解析失败");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Token解析异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
    }
}
