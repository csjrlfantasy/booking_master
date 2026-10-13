package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.user.dto.UserLoginRequest;
import com.ticket.app.controller.user.dto.UserTokenResponse;
import com.ticket.app.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest httpRequest;

    private AdminAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminAuthController(userService);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void loginShouldRejectNonAdminUsers() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("student");
        request.setPassword("Password@123");

        UserTokenResponse response = new UserTokenResponse();
        response.setUserId(1001L);
        response.setUsername("student");
        response.setUserType(1);
        response.setUserLevel(1);
        response.setToken("user-token");
        when(userService.login(anyString(), anyString(), anyString())).thenReturn(response);

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.login(request, httpRequest));

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void loginShouldAllowAdminUsers() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123456");

        UserTokenResponse response = new UserTokenResponse();
        response.setUserId(25L);
        response.setUsername("admin");
        response.setUserType(3);
        response.setUserLevel(10);
        response.setToken("admin-token");
        when(userService.login(anyString(), anyString(), anyString())).thenReturn(response);

        Result<UserTokenResponse> result = controller.login(request, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals("admin-token", result.getData().getToken());
    }
}
