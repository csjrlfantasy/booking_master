package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.user.dto.UserRegisterRequest;
import com.ticket.app.service.admin.AdminUserService;
import com.ticket.app.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserCreateControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest httpRequest;

    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        adminUserController = new AdminUserController(adminUserService, jwtUtils, userService);
    }

    @Test
    void createUserShouldRegisterUserFromAdminPanel() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("student001");
        request.setPassword("Password123");
        request.setPhone("13800138001");
        request.setEmail("student001@example.com");
        request.setNickname("测试学员001");

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);
        when(userService.register(any(UserRegisterRequest.class))).thenReturn(1001L);

        Result<Long> result = adminUserController.createUser(request, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals(1001L, result.getData());

        ArgumentCaptor<UserRegisterRequest> captor = ArgumentCaptor.forClass(UserRegisterRequest.class);
        verify(userService).register(captor.capture());
        assertEquals("student001", captor.getValue().getUsername());
        assertEquals("admin", captor.getValue().getSource());
    }
}
