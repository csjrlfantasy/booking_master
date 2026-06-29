package com.ticket.app.service.admin;

import com.ticket.app.controller.admin.dto.request.AdminAdjustBalanceRequest;
import com.ticket.app.entity.User;
import com.ticket.app.repository.admin.AdminUserRepository;
import com.ticket.app.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserBalanceServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private UserRepository userRepository;

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(adminUserRepository, userRepository);
    }

    @Test
    void addBalanceShouldIncreaseSelectedWallet() {
        User user = new User();
        user.setId(8L);
        user.setWechatBalance(new BigDecimal("100.00"));
        user.setAlipayBalance(new BigDecimal("200.00"));

        AdminAdjustBalanceRequest request = new AdminAdjustBalanceRequest();
        request.setChannel("wechat");
        request.setAmount(new BigDecimal("50.00"));

        when(userRepository.selectById(8L)).thenReturn(user);

        adminUserService.addBalance(8L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).updateById(captor.capture());
        assertEquals(new BigDecimal("150.00"), captor.getValue().getWechatBalance());
        assertEquals(new BigDecimal("200.00"), captor.getValue().getAlipayBalance());
    }
}
