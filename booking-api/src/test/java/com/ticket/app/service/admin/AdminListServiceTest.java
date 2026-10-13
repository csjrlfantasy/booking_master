package com.ticket.app.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.controller.admin.dto.request.AdminOrderListRequest;
import com.ticket.app.controller.admin.dto.request.AdminUserListRequest;
import com.ticket.app.controller.admin.dto.response.AdminOrderResponse;
import com.ticket.app.controller.admin.dto.response.AdminUserResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.User;
import com.ticket.app.repository.admin.AdminOrderRepository;
import com.ticket.app.repository.admin.AdminUserRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import com.ticket.app.service.coupon.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminListServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminOrderRepository adminOrderRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CouponService couponService;

    @Test
    void userListShouldUseBuiltInPaginationWhenNoXmlMapperExists() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(adminUserRepository, userRepository);
        User user = new User();
        user.setId(25L);
        user.setUsername("admin");
        user.setNickname("Admin");
        user.setStatus(1);
        user.setUserType(3);
        user.setCreatedAt(LocalDateTime.now());

        Page<User> page = new Page<>(1, 5);
        page.setRecords(List.of(user));
        page.setTotal(1);

        when(adminUserRepository.selectPage(any(Page.class), any())).thenReturn(page);
        when(adminUserRepository.selectUserOrderCount(25L)).thenReturn(3);
        when(adminUserRepository.selectUserPaidOrderAmount(25L)).thenReturn(new BigDecimal("580.00"));

        AdminUserListRequest request = new AdminUserListRequest();
        request.setPage(1);
        request.setPageSize(5);

        PageResult<AdminUserResponse> result = service.getUserList(request);

        assertEquals(1, result.getList().size());
        assertEquals("admin", result.getList().get(0).getUsername());
        assertEquals(3, result.getList().get(0).getOrderCount());
        assertEquals(new BigDecimal("580.00"), result.getList().get(0).getTotalOrderAmount());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void orderListShouldUseBuiltInPaginationWhenNoXmlMapperExists() {
        AdminOrderServiceImpl service = new AdminOrderServiceImpl(
                adminOrderRepository,
                orderRepository,
                userRepository,
                couponService);
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORDER-001");
        order.setUserId(25L);
        order.setTicketName("Test Ticket");
        order.setContactName("Buyer Zhang");
        order.setContactPhone("13800000000");
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setCreatedAt(LocalDateTime.now());

        Page<Order> page = new Page<>(1, 5);
        page.setRecords(List.of(order));
        page.setTotal(1);

        when(adminOrderRepository.selectPage(any(Page.class), any())).thenReturn(page);

        AdminOrderListRequest request = new AdminOrderListRequest();
        request.setPage(1);
        request.setPageSize(5);

        PageResult<AdminOrderResponse> result = service.getOrderList(request);

        assertEquals(1, result.getList().size());
        assertEquals("ORDER-001", result.getList().get(0).getOrderNo());
        assertEquals("Buyer Zhang", result.getList().get(0).getContactName());
        assertEquals("13800000000", result.getList().get(0).getContactPhone());
        assertEquals(1L, result.getTotal());
    }
}
