package com.ticket.app.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.common.util.PasswordUtils;
import com.ticket.app.controller.user.dto.UserLoginResponse;
import com.ticket.app.controller.user.dto.UserRegisterRequest;
import com.ticket.app.controller.user.dto.UserTokenResponse;
import com.ticket.app.entity.User;
import com.ticket.app.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户中心测试用例
 * 
 * 测试范围：
 * 1. 用户注册 (register)
 * 2. 用户登录 (login)
 * 3. 用户信息查询 (getUserById, getUserByUsername)
 * 4. 异常情况处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户中心测试")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, jwtUtils);
        ReflectionTestUtils.setField(userService, "jwtExpiration", 86400000L);
    }

    // ==================== 用户注册测试 ====================

    @Nested
    @DisplayName("用户注册测试 - register")
    class UserRegisterTests {

        @Test
        @DisplayName("TC-REG-001: 正常注册成功")
        void testRegisterSuccess() {
            // Given
            UserRegisterRequest request = createRegisterRequest("testuser", "13800138000");
            when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(userRepository.insert(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(100L);
                return 1;
            });

            // When
            Long userId = userService.register(request);

            // Then
            assertEquals(100L, userId);
            
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).insert(userCaptor.capture());
            
            User savedUser = userCaptor.getValue();
            assertEquals("testuser", savedUser.getUsername());
            assertEquals("13800138000", savedUser.getPhone());
            assertEquals(User.USER_TYPE_NORMAL, savedUser.getUserType());
            assertEquals(1, savedUser.getUserLevel());
            assertEquals(User.STATUS_NORMAL, savedUser.getStatus());
            assertEquals(0, savedUser.getLoginFailCount());
        }

        @Test
        @DisplayName("TC-REG-002: 用户名已存在应注册失败")
        void testRegisterWithExistingUsername() {
            // Given
            UserRegisterRequest request = createRegisterRequest("existinguser", "13800138001");
            when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.register(request);
            });
            
            assertEquals(ResultCode.USER_ALREADY_EXISTS.getCode(), exception.getCode());
            verify(userRepository, never()).insert(any(User.class));
        }

        @Test
        @DisplayName("TC-REG-003: 手机号已存在应注册失败")
        void testRegisterWithExistingPhone() {
            // Given
            UserRegisterRequest request = createRegisterRequest("newuser", "13800138000");
            // 第一次查询用户名返回0（不冲突）
            when(userRepository.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)  // 用户名检查
                .thenReturn(1L); // 手机号检查

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.register(request);
            });
            
            assertEquals(ResultCode.USER_PHONE_EXISTS.getCode(), exception.getCode());
            verify(userRepository, never()).insert(any(User.class));
        }

        @Test
        @DisplayName("TC-REG-004: 邮箱已存在应注册失败")
        void testRegisterWithExistingEmail() {
            // Given
            UserRegisterRequest request = createRegisterRequest("newuser", "13800138000");
            request.setEmail("existing@test.com");
            
            when(userRepository.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)  // 用户名检查
                .thenReturn(0L)  // 手机号检查
                .thenReturn(1L); // 邮箱检查

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.register(request);
            });
            
            assertEquals(ResultCode.USER_EMAIL_EXISTS.getCode(), exception.getCode());
            verify(userRepository, never()).insert(any(User.class));
        }

        @Test
        @DisplayName("TC-REG-005: 不提供邮箱时注册应成功")
        void testRegisterWithoutEmail() {
            // Given
            UserRegisterRequest request = createRegisterRequest("testuser2", "13800138002");
            request.setEmail(null);
            
            when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(userRepository.insert(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(101L);
                return 1;
            });

            // When
            Long userId = userService.register(request);

            // Then
            assertEquals(101L, userId);
            verify(userRepository).insert(any(User.class));
        }

        @Test
        @DisplayName("TC-REG-006: 注册时无昵称应使用用户名作为昵称")
        void testRegisterWithoutNickname() {
            // Given
            UserRegisterRequest request = createRegisterRequest("testuser3", "13800138003");
            request.setNickname(null);
            
            when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(userRepository.insert(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(102L);
                return 1;
            });

            // When
            userService.register(request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).insert(userCaptor.capture());
            
            assertEquals("testuser3", userCaptor.getValue().getNickname());
        }
    }

    // ==================== 用户登录测试 ====================

    @Nested
    @DisplayName("用户登录测试 - login")
    class UserLoginTests {

        @Test
        @DisplayName("TC-LOGIN-001: 正常登录成功")
        void testLoginSuccess() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            user.setPassword(PasswordUtils.encode("password123"));
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
            when(jwtUtils.generateToken(anyLong(), anyString())).thenReturn("test_jwt_token");

            // When
            UserLoginResponse response = userService.login("testuser", "password123", "127.0.0.1");

            // Then
            assertNotNull(response);
            assertTrue(response instanceof UserTokenResponse);
            UserTokenResponse tokenResponse = (UserTokenResponse) response;
            assertEquals(1L, tokenResponse.getUserId());
            assertEquals("testuser", tokenResponse.getUsername());
            assertEquals("test_jwt_token", tokenResponse.getToken());
            assertEquals(86400L, tokenResponse.getExpiresIn());
        }

        @Test
        @DisplayName("TC-LOGIN-002: 用户不存在应登录失败")
        void testLoginWithNonExistentUser() {
            // Given
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.login("nonexistent", "password", "127.0.0.1");
            });
            
            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-LOGIN-003: 密码错误应登录失败")
        void testLoginWithWrongPassword() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            user.setPassword(PasswordUtils.encode("password123"));
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.login("testuser", "wrongpassword", "127.0.0.1");
            });
            
            assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-LOGIN-004: 禁用用户应登录失败")
        void testLoginWithDisabledUser() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            user.setStatus(User.STATUS_DISABLED);
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.login("testuser", "password", "127.0.0.1");
            });
            
            assertEquals(ResultCode.USER_DISABLED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-LOGIN-005: 连续失败5次应锁定账户")
        void testLoginFail5TimesShouldLockAccount() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            user.setPassword(PasswordUtils.encode("password123"));
            user.setLoginFailCount(4); // 已经失败了4次
            
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            // When: 第5次失败
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.login("testuser", "wrongpassword", "127.0.0.1");
            });
            
            assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), exception.getCode());

            // Then: 验证账户被锁定
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).updateById(userCaptor.capture());
            
            User updatedUser = userCaptor.getValue();
            assertNotNull(updatedUser.getLockUntil());
            assertTrue(updatedUser.getLockUntil().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("TC-LOGIN-006: 已锁定账户应拒绝登录")
        void testLoginWithLockedAccount() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            user.setPassword(PasswordUtils.encode("password123"));
            user.setLockUntil(LocalDateTime.now().plusMinutes(30)); // 锁定30分钟
            
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.login("testuser", "password", "127.0.0.1");
            });
            
            assertEquals(ResultCode.USER_LOGIN_LOCKED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-LOGIN-007: 登录成功应重置失败次数")
        void testLoginSuccessShouldResetFailCount() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            user.setPassword(PasswordUtils.encode("password123"));
            user.setLoginFailCount(4);
            user.setLockUntil(null);
            
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
            when(jwtUtils.generateToken(anyLong(), anyString())).thenReturn("test_jwt_token");

            // When
            userService.login("testuser", "password123", "127.0.0.1");

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, atLeastOnce()).updateById(userCaptor.capture());
            
            // 获取最后一次更新的用户对象
            User updatedUser = userCaptor.getAllValues().get(userCaptor.getAllValues().size() - 1);
            assertEquals(0, updatedUser.getLoginFailCount());
            assertNull(updatedUser.getLockUntil());
            assertNotNull(updatedUser.getLastLoginTime());
        }
    }

    // ==================== 用户信息查询测试 ====================

    @Nested
    @DisplayName("用户信息查询测试")
    class UserQueryTests {

        @Test
        @DisplayName("TC-QUERY-001: 根据用户ID查询成功")
        void testGetUserByIdSuccess() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            when(userRepository.selectById(1L)).thenReturn(user);

            // When
            UserLoginResponse response = userService.getUserById(1L);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getUserId());
            assertEquals("testuser", response.getUsername());
            assertEquals("13800138000", response.getPhone());
        }

        @Test
        @DisplayName("TC-QUERY-002: 根据用户ID查询 - 用户不存在")
        void testGetUserByIdNotFound() {
            // Given
            when(userRepository.selectById(999L)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.getUserById(999L);
            });
            
            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-QUERY-003: 根据用户名查询成功")
        void testGetUserByUsernameSuccess() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            // When
            UserLoginResponse response = userService.getUserByUsername("testuser");

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getUserId());
            assertEquals("testuser", response.getUsername());
        }

        @Test
        @DisplayName("TC-QUERY-004: 根据用户名查询 - 用户不存在")
        void testGetUserByUsernameNotFound() {
            // Given
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                userService.getUserByUsername("nonexistent");
            });
            
            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-QUERY-005: 用户信息应屏蔽敏感字段")
        void testUserInfoShouldMaskSensitiveFields() {
            // Given
            User user = createTestUser(1L, "testuser", "13800138000");
            user.setPassword("should_not_be_exposed");
            when(userRepository.selectById(1L)).thenReturn(user);

            // When
            UserLoginResponse response = userService.getUserById(1L);

            // Then
            // UserLoginResponse 应该不包含 password 字段
            assertNotNull(response);
            // 通过反射验证密码字段不被暴露
            try {
                var passwordField = UserLoginResponse.class.getDeclaredField("password");
                assertTrue(passwordField.isAnnotationPresent(Deprecated.class) || 
                          passwordField.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class),
                          "密码字段应该被标记为忽略");
            } catch (NoSuchFieldException e) {
                // password字段不存在是预期的
            }
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
     * 创建测试用户
     */
    private User createTestUser(Long id, String username, String phone) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("$2a$10$test_hash");
        user.setPhone(phone);
        user.setEmail(username + "@test.com");
        user.setNickname(username + "_nick");
        user.setGender(User.GENDER_MALE);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setUserType(User.USER_TYPE_NORMAL);
        user.setUserLevel(1);
        user.setStatus(User.STATUS_NORMAL);
        user.setLoginFailCount(0);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
