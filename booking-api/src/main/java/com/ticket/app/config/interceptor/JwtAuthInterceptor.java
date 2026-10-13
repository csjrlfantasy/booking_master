package com.ticket.app.config.interceptor;

import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.entity.User;
import com.ticket.app.repository.user.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String USER_ID_ATTR = "userId";
    public static final String USERNAME_ATTR = "username";

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = getToken(request);
        String uri = request.getRequestURI();

        if (StringUtils.isEmpty(token)) {
            if (isPublicUri(uri)) {
                return true;
            }
            log.warn("request missing token: uri={}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\"}");
            return false;
        }

        try {
            Claims claims = jwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            request.setAttribute(USER_ID_ATTR, userId);
            request.setAttribute(USERNAME_ATTR, username);

            if (isAdminProtectedUri(uri) && !isAdminUser(userId)) {
                log.warn("non-admin attempted admin api: userId={}, uri={}", userId, uri);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":100001,\"message\":\"禁止访问后台接口\"}");
                return false;
            }

            log.debug("token verified: userId={}, username={}", userId, username);
            return true;
        } catch (Exception e) {
            log.warn("token verification failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\"}");
            return false;
        }
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(TOKEN_HEADER);
        if (StringUtils.isNotEmpty(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private boolean isPublicUri(String uri) {
        if (uri.startsWith("/api/user/register")
                || uri.startsWith("/api/user/login")
                || uri.startsWith("/api/admin/login")) {
            return true;
        }
        if (uri.startsWith("/api/categories") || uri.startsWith("/api/tickets") || uri.startsWith("/api/banners")) {
            return true;
        }
        return uri.contains("/swagger-ui")
                || uri.contains("/api-docs")
                || uri.contains("/v3/api-docs")
                || uri.endsWith("/error")
                || uri.startsWith("/api/actuator");
    }

    private boolean isAdminProtectedUri(String uri) {
        return uri != null && uri.startsWith("/api/admin/") && !uri.startsWith("/api/admin/login");
    }

    private boolean isAdminUser(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userRepository.selectById(userId);
        return user != null && user.getUserLevel() != null && user.getUserLevel() >= 10;
    }
}
