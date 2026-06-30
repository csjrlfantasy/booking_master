package com.ticket.app.controller.user;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.user.dto.UserLoginRequest;
import com.ticket.app.controller.user.dto.UserLoginResponse;
import com.ticket.app.controller.user.dto.UserRegisterRequest;
import com.ticket.app.controller.user.dto.UserTokenResponse;
import com.ticket.app.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户控制器测试
 * 
 * 测试范围：
 * 1. POST /user/register 用户注册
 * 2. POST /user/login 用户登录
 * 3. GET /user/{userId} 获取用户信息
 * 4. GET /user/username/{username} 根据用户名获取用户信息
 * 5. 未登录访问测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户中心接口测试")
public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpRequest;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, jwtUtils);
    }

    // ==================== 用户注册测试 ====================

    @Nested
    @DisplayName("用户注册测试 - POST /user/register")
    class UserRegisterTests {

        @Test
        @DisplayName("TC-API-USER-001: 正常注册成功")
        void testRegisterSuccess() {
            // Given
            UserRegisterRequest request = createRegisterRequest("testuser", "13800138000");
            when(userService.register(any(UserRegisterRequest.class))).thenReturn(100L);

            // When
            Result<Long> result = userController.register(request);

            // Then
            assertTrue(result.isSuccess());
            assertEquals(100L, result.getData());
            verify(userService).register(any(UserRegisterRequest.class));
        }

        @Test
        @DisplayName("TC-API-USER-002: 注册失败 - 用户名已存在")
        void testRegisterFailUsernameExists() {
            // Given
            UserRegisterRequest request = createRegisterRequest("existinguser", "13800138000");
            when(userService.register(any(UserRegisterRequest.class)))
                .thenThrow(new BusinessException(ResultCode.USER_ALREADY_EXISTS, "用户名已存在"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userController.register(request);
            });
            
            assertEquals(ResultCode.USER_ALREADY_EXISTS.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-API-USER-003: 注册失败 - 手机号已存在")
        void testRegisterFailPhoneExists() {
            // Given
            UserRegisterRequest request = createRegisterRequest("newuser", "13800138000");
            when(userService.register(any(UserRegisterRequest.class)))
                .thenThrow(new BusinessException(ResultCode.USER_PHONE_EXISTS, "手机号已被注册"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userController.register(request);
            });
            
            assertEquals(ResultCode.USER_PHONE_EXISTS.getCode(), exception.getCode());
        }
    }

    // ==================== 用户登录测试 ====================

    @Nested
    @DisplayName("用户登录测试 - POST /user/login")
    class UserLoginTests {

        @Test
        @DisplayName("TC-API-USER-010: 正常登录成功")
        void testLoginSuccess() {
            // Given
            UserLoginRequest request = new UserLoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            
            UserTokenResponse tokenResponse = createTokenResponse(1L, "testuser");
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(userService.login(eq("testuser"), eq("password123"), anyString()))
                .thenReturn(tokenResponse);

            // When
            Result<UserTokenResponse> result = userController.login(request, httpRequest);

            // Then
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(1L, result.getData().getUserId());
            assertEquals("testuser", result.getData().getUsername());
            assertNotNull(result.getData().getToken());
        }

        @Test
        @DisplayName("TC-API-USER-011: 登录失败 - 用户不存在")
        void testLoginFailUserNotFound() {
            // Given
            UserLoginRequest request = new UserLoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("password123");
            
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(userService.login(anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userController.login(request, httpRequest);
            });
            
            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-API-USER-012: 登录失败 - 密码错误")
        void testLoginFailWrongPassword() {
            // Given
            UserLoginRequest request = new UserLoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");
            
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(userService.login(anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException(ResultCode.USER_PASSWORD_ERROR, "用户名或密码错误"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userController.login(request, httpRequest);
            });
            
            assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-API-USER-013: 登录失败 - 账户已锁定")
        void testLoginFailAccountLocked() {
            // Given
            UserLoginRequest request = new UserLoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(userService.login(anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException(ResultCode.USER_LOGIN_LOCKED, "账号已被锁定"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userController.login(request, httpRequest);
            });
            
            assertEquals(ResultCode.USER_LOGIN_LOCKED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-API-USER-014: 登录成功应记录IP")
        void testLoginShouldRecordIP() {
            // Given
            UserLoginRequest request = new UserLoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            
            UserTokenResponse tokenResponse = createTokenResponse(1L, "testuser");
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.100");
            when(userService.login(eq("testuser"), eq("password123"), eq("192.168.1.100")))
                .thenReturn(tokenResponse);

            // When
            userController.login(request, httpRequest);

            // Then
            verify(userService).login("testuser", "password123", "192.168.1.100");
        }
    }

    // ==================== 用户信息查询测试 ====================

    @Nested
    @DisplayName("用户信息查询测试 - GET /user/{userId}")
    class UserQueryTests {

        @Test
        @DisplayName("TC-API-USER-020: 根据用户ID查询成功")
        void testGetUserByIdSuccess() {
            // Given
            Long userId = 1L;
            UserLoginResponse userResponse = createUserLoginResponse(userId, "testuser");
            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer test_token");
            when(userService.getUserById(userId)).thenReturn(userResponse);
            when(jwtUtils.getUserIdFromToken("test_token")).thenReturn(userId);

            // When
            Result<UserLoginResponse> result = userController.getUserById(userId, httpRequest);

            // Then
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(userId, result.getData().getUserId());
            assertEquals("testuser", result.getData().getUsername());
        }

        @Test
        @DisplayName("TC-API-USER-021: 根据用户ID查询 - 用户不存在")
        void testGetUserByIdNotFound() {
            // Given
            Long userId = 999L;
            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer test_token");
            when(jwtUtils.getUserIdFromToken("test_token")).thenReturn(999L);
            when(userService.getUserById(userId))
                .thenThrow(new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userController.getUserById(userId, httpRequest);
            });
            
            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-API-USER-022: 根据用户名查询成功")
        void testGetUserByUsernameSuccess() {
            // Given
            String username = "testuser";
            UserLoginResponse userResponse = createUserLoginResponse(1L, username);
            when(userService.getUserByUsername(username)).thenReturn(userResponse);

            // When
            Result<UserLoginResponse> result = userController.getUserByUsername(username);

            // Then
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(username, result.getData().getUsername());
        }

        @Test
        @DisplayName("TC-API-USER-023: 根据用户名查询 - 用户不存在")
        void testGetUserByUsernameNotFound() {
            // Given
            String username = "nonexistent";
            when(userService.getUserByUsername(username))
                .thenThrow(new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userController.getUserByUsername(username);
            });
            
            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建注册请求
     */
    private UserRegisterRequest createRegisterRequest(String username, String phone) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(username);
        request.setPassword("password123");
        request.setPhone(phone);
        request.setNickname(username + "_nick");
        request.setSource("app");
        return request;
    }

    /**
     * 创建Token响应
     */
    private UserTokenResponse createTokenResponse(Long userId, String username) {
        UserTokenResponse response = new UserTokenResponse();
        response.setUserId(userId);
        response.setUsername(username);
        response.setNickname(username + "_nick");
        response.setPhone("13800138000");
        response.setEmail(username + "@test.com");
        response.setUserType(1);
        response.setUserLevel(1);
        response.setToken("test_jwt_token_" + userId);
        response.setExpiresIn(86400L);
        return response;
    }

    /**
     * 创建用户登录响应
     */
    private UserLoginResponse createUserLoginResponse(Long userId, String username) {
        UserLoginResponse response = new UserLoginResponse();
        response.setUserId(userId);
        response.setUsername(username);
        response.setNickname(username + "_nick");
        response.setPhone("13800138000");
        response.setEmail(username + "@test.com");
        response.setUserType(1);
        response.setUserLevel(1);
        return response;
    }
}
