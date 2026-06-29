package com.ticket.app.controller.order;

import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 安全测试：OrderController安全漏洞验证
 * 
 * 问题1: 越权漏洞 - Token解析失败时使用默认用户ID=1L
 * 测试目标：验证Token无效时应拒绝请求，而不是返回默认用户ID
 */
@ExtendWith(MockitoExtension.class)
public class OrderControllerSecurityTest {

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private JwtUtils jwtUtils;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(null, jwtUtils);
    }

    /**
     * 安全测试 - Token解析失败应拒绝请求
     * 
     * 当前漏洞: getUserIdFromRequest()在Token解析失败时返回默认用户ID=1L
     * 期望行为: Token无效时应抛出异常或返回null，让Controller层处理拒绝请求
     */
    @Test
    void testTokenInvalidShouldRejectRequest() throws Exception {
        // Given: 无效的Token
        String invalidToken = "invalid.token.string";
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        
        // 模拟Token解析失败抛出异常
        when(jwtUtils.getUserIdFromToken(invalidToken))
            .thenThrow(new BusinessException(ResultCode.TOKEN_INVALID, "Token无效"));

        // When & Then: 验证是否抛出异常而不是返回默认用户ID
        // 当前实现返回1L，这是安全漏洞
        // 修复后应抛出异常
        try {
            // 获取私有的getUserIdFromRequest方法结果
            var method = OrderController.class.getDeclaredMethod("getUserIdFromRequest", HttpServletRequest.class);
            method.setAccessible(true);
            
            Long userId = (Long) method.invoke(orderController, httpRequest);
            
            // BUG: 当前返回1L，应该拒绝请求
            assertNotEquals(1L, userId, "Token无效时不应返回默认用户ID");
            assertNull(userId, "Token无效时应返回null");
        } catch (Exception e) {
            // 期望抛出业务异常
            assertTrue(e.getCause() instanceof BusinessException);
        }
    }

    /**
     * 安全测试 - 无Token时应拒绝请求
     */
    @Test
    void testNoTokenShouldRejectRequest() {
        // Given: 没有Token
        when(httpRequest.getHeader("Authorization")).thenReturn(null);

        // When & Then
        try {
            var method = OrderController.class.getDeclaredMethod("getUserIdFromRequest", HttpServletRequest.class);
            method.setAccessible(true);
            
            Long userId = (Long) method.invoke(orderController, httpRequest);
            
            // BUG: 当前返回1L
            assertNotEquals(1L, userId, "无Token时不应返回默认用户ID");
            assertNull(userId, "无Token时应返回null");
        } catch (Exception e) {
            // 期望抛出异常
            assertTrue(e.getCause() instanceof BusinessException);
        }
    }
}