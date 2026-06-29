package com.ticket.app.config.interceptor;

import com.ticket.app.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    /**
     * 请求头中Token的Key
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 用户ID attribute key
     */
    public static final String USER_ID_ATTR = "userId";

    /**
     * 用户名 attribute key
     */
    public static final String USERNAME_ATTR = "username";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Token
        String token = getToken(request);
        
        if (StringUtils.isEmpty(token)) {
            // 没有Token，检查是否是公开接口
            String uri = request.getRequestURI();
            if (isPublicUri(uri)) {
                return true;
            }
            log.warn("请求缺少Token: uri={}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\"}");
            return false;
        }

        // 验证Token
        try {
            Claims claims = jwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            // 将用户信息存入request attributes，供后续使用
            request.setAttribute(USER_ID_ATTR, userId);
            request.setAttribute(USERNAME_ATTR, username);

            log.debug("Token验证成功: userId={}, username={}", userId, username);
            return true;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\"}");
            return false;
        }
    }

    /**
     * 从请求中获取Token
     */
    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(TOKEN_HEADER);
        if (StringUtils.isNotEmpty(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 判断是否是公开接口(不需要登录)
     */
    private boolean isPublicUri(String uri) {
        // 公开接口列表 - 用户模块 (实际路径: /api/user/register, /api/user/login)
        if (uri.startsWith("/api/user/register")
                || uri.startsWith("/api/user/login")
                || uri.startsWith("/api/admin/login")) {
            return true;
        }
        // 票务模块公开接口
        if (uri.startsWith("/api/categories") || uri.startsWith("/api/tickets") || uri.startsWith("/api/banners")) {
            return true;
        }
        // Swagger和API文档
        return uri.contains("/swagger-ui")
                || uri.contains("/api-docs")
                || uri.contains("/v3/api-docs")
                || uri.endsWith("/error")
                || uri.startsWith("/api/actuator");
    }
}
