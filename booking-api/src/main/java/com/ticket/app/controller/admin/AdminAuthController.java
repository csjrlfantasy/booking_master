package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.controller.user.dto.UserLoginRequest;
import com.ticket.app.controller.user.dto.UserTokenResponse;
import com.ticket.app.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "管理后台 - 认证", description = "管理员登录接口")
public class AdminAuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理后台用户名密码登录")
    public Result<UserTokenResponse> login(@Valid @RequestBody UserLoginRequest request, HttpServletRequest httpRequest) {
        String loginIp = getClientIp(httpRequest);
        UserTokenResponse response = (UserTokenResponse) userService.login(
                request.getUsername(),
                request.getPassword(),
                loginIp
        );
        log.info("管理员登录成功: username={}, ip={}", request.getUsername(), loginIp);
        return Result.success(response);
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
}
