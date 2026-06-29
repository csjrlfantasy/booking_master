package com.ticket.app.config;

import com.ticket.app.config.interceptor.JwtAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.UUID;

/**
 * Web MVC配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    public static List<String> jwtExcludePathPatterns() {
        return List.of(
                "/api/user/register",
                "/api/user/login",
                "/api/admin/login",
                "/api/banners/**",
                "/api/uploads/**",
                "/uploads/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/doc.html",
                "/favicon.ico",
                "/error"
        );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 请求ID拦截器
        registry.addInterceptor(new RequestIdInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                        "/swagger-resources/**", "/webjars/**", "/doc.html", "/favicon.ico");

        // JWT认证拦截器
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(jwtExcludePathPatterns());
    }

    /**
     * 请求ID拦截器
     */
    public static class RequestIdInterceptor implements HandlerInterceptor {

        private static final String REQUEST_ID = "requestId";

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String requestId = request.getHeader(REQUEST_ID);
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString().replace("-", "");
            }
            MDC.put(REQUEST_ID, requestId);
            response.setHeader(REQUEST_ID, requestId);
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            MDC.remove(REQUEST_ID);
        }
    }
}
