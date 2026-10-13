package com.ticket.app.service.admin;

import com.ticket.app.controller.admin.dto.request.AdminCouponCreateRequest;
import com.ticket.app.entity.Coupon;
import com.ticket.app.entity.UserCoupon;
import com.ticket.app.repository.coupon.CouponRepository;
import com.ticket.app.repository.coupon.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    private AdminCouponService adminCouponService;

    @BeforeEach
    void setUp() {
        adminCouponService = new AdminCouponServiceImpl(couponRepository, userCouponRepository);
    }

    @Test
    void should_create_coupon_and_issue_to_specific_users() {
        AtomicLong couponId = new AtomicLong(6001L);
        when(couponRepository.insert(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon coupon = invocation.getArgument(0);
            coupon.setId(couponId.get());
            return 1;
        });
        when(couponRepository.selectById(6001L)).thenReturn(createdCoupon());
        when(userCouponRepository.selectCount(any())).thenReturn(0L);

        Long createdId = adminCouponService.create(buildCreateRequest());
        adminCouponService.issue(createdId, List.of(1001L, 1002L), "manual");

        assertEquals(6001L, createdId);

        ArgumentCaptor<Coupon> couponCaptor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).insert(couponCaptor.capture());
        assertEquals("暑期满减券", couponCaptor.getValue().getName());
        assertEquals(Coupon.STATUS_ACTIVE, couponCaptor.getValue().getStatus());
        assertEquals(20, couponCaptor.getValue().getRemainCount());

        ArgumentCaptor<UserCoupon> issueCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponRepository, times(2)).insert(issueCaptor.capture());
        List<UserCoupon> issued = issueCaptor.getAllValues();
        assertEquals(List.of(1001L, 1002L), issued.stream().map(UserCoupon::getUserId).toList());
        assertEquals(List.of(6001L, 6001L), issued.stream().map(UserCoupon::getCouponId).toList());

        ArgumentCaptor<Coupon> updateCaptor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).updateById(updateCaptor.capture());
        assertEquals(18, updateCaptor.getValue().getRemainCount());
    }

    private AdminCouponCreateRequest buildCreateRequest() {
        AdminCouponCreateRequest request = new AdminCouponCreateRequest();
        request.setName("暑期满减券");
        request.setCouponType(Coupon.TYPE_FULL_REDUCE);
        request.setCouponValue(new BigDecimal("30.00"));
        request.setMinOrderAmount(new BigDecimal("100.00"));
        request.setTotalCount(20);
        request.setRemainCount(20);
        request.setPerLimit(1);
        request.setReceiveType(Coupon.RECEIVE_TYPE_MANUAL);
        request.setValidityType(Coupon.VALID_TYPE_FIXED);
        request.setValidStartDate(LocalDate.of(2026, 7, 1));
        request.setValidEndDate(LocalDate.of(2026, 7, 31));
        request.setApplicableScope(Coupon.SCOPE_ALL);
        request.setDescription("暑期活动满100减30");
        request.setNotice("每人限领1张");
        request.setReceiveStartTime(LocalDateTime.of(2026, 7, 1, 0, 0));
        request.setReceiveEndTime(LocalDateTime.of(2026, 7, 31, 23, 59));
        request.setStatus(Coupon.STATUS_ACTIVE);
        return request;
    }

    private Coupon createdCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId(6001L);
        coupon.setName("暑期满减券");
        coupon.setCouponType(Coupon.TYPE_FULL_REDUCE);
        coupon.setCouponValue(new BigDecimal("30.00"));
        coupon.setMinOrderAmount(new BigDecimal("100.00"));
        coupon.setTotalCount(20);
        coupon.setRemainCount(20);
        coupon.setPerLimit(1);
        coupon.setReceiveType(Coupon.RECEIVE_TYPE_MANUAL);
        coupon.setValidityType(Coupon.VALID_TYPE_FIXED);
        coupon.setValidStartDate(LocalDate.of(2026, 7, 1));
        coupon.setValidEndDate(LocalDate.of(2026, 7, 31));
        coupon.setApplicableScope(Coupon.SCOPE_ALL);
        coupon.setDescription("暑期活动满100减30");
        coupon.setNotice("每人限领1张");
        coupon.setReceiveStartTime(LocalDateTime.of(2026, 7, 1, 0, 0));
        coupon.setReceiveEndTime(LocalDateTime.of(2026, 7, 31, 23, 59));
        coupon.setStatus(Coupon.STATUS_ACTIVE);
        return coupon;
    }
}
