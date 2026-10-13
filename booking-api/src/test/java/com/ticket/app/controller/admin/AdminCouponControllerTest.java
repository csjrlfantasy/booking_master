package com.ticket.app.controller.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminCouponCreateRequest;
import com.ticket.app.controller.admin.dto.request.AdminCouponIssueRequest;
import com.ticket.app.controller.admin.dto.response.AdminCouponListItemResponse;
import com.ticket.app.service.admin.AdminCouponService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCouponControllerTest {

    @Mock
    private AdminCouponService adminCouponService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpRequest;

    private AdminCouponController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminCouponController(adminCouponService, jwtUtils);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);
    }

    @Test
    void createShouldDelegateToAdminCouponService() {
        AdminCouponCreateRequest request = new AdminCouponCreateRequest();
        request.setName("夏日券");
        when(adminCouponService.create(request)).thenReturn(9001L);

        Result<Long> result = controller.create(request, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals(9001L, result.getData());
        verify(adminCouponService).create(request);
    }

    @Test
    void issueShouldDelegateToAdminCouponService() {
        AdminCouponIssueRequest request = new AdminCouponIssueRequest();
        request.setUserIds(List.of(1001L, 1002L));
        request.setIssueMode("manual");

        Result<Void> result = controller.issue(77L, request, httpRequest);

        assertTrue(result.isSuccess());
        verify(adminCouponService).issue(77L, List.of(1001L, 1002L), "manual");
    }

    @Test
    void listShouldReturnPagedCoupons() {
        AdminCouponListItemResponse item = new AdminCouponListItemResponse();
        item.setId(77L);
        item.setName("夏日券");
        when(adminCouponService.list("夏", 1, 1, 10))
                .thenReturn(PageResult.of(List.of(item), 1L, 1, 10));

        Result<PageResult<AdminCouponListItemResponse>> result = controller.list("夏", 1, 1, 10, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getList().size());
        assertEquals("夏日券", result.getData().getList().get(0).getName());
    }

    @Test
    void detailShouldReturnCouponFormData() {
        AdminCouponCreateRequest detail = new AdminCouponCreateRequest();
        detail.setName("学生票优惠券");
        when(adminCouponService.detail(88L)).thenReturn(detail);

        Result<AdminCouponCreateRequest> result = controller.detail(88L, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals("学生票优惠券", result.getData().getName());
        verify(adminCouponService).detail(88L);
    }
}
