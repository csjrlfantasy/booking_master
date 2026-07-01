package com.ticket.app.service.coupon;

import com.ticket.app.common.PageResult;
import com.ticket.app.controller.coupon.dto.CouponCenterResponse;
import com.ticket.app.controller.coupon.dto.UserCouponResponse;
import com.ticket.app.controller.coupon.dto.ValidateCouponRequest;
import com.ticket.app.controller.coupon.dto.ValidateCouponResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    PageResult<CouponCenterResponse> getCouponCenter(Long userId, Integer page, Integer pageSize);

    Long receiveCoupon(Long userId, Long couponId);

    PageResult<UserCouponResponse> getUserCouponList(Long userId, Integer status, Integer page, Integer pageSize);

    List<UserCouponResponse> getAvailableCoupons(Long userId, BigDecimal orderAmount, Long ticketId, Integer categoryId);

    ValidateCouponResponse validateUserCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long ticketId, Integer categoryId);

    void bindCouponToOrder(Long userId, Long userCouponId, Long orderId);

    ValidateCouponResponse validateCoupon(Long userId, ValidateCouponRequest request);

    void useCoupon(Long userId, Long userCouponId, Long orderId);

    void markCouponUsed(Long userId, Long userCouponId, Long orderId);

    void returnCoupon(Long userId, Long orderId);

    void returnCoupon(Long userId, Long orderId, boolean forceExpireIfOutOfWindow);

    UserCouponResponse getUserCouponDetail(Long userId, Long userCouponId);
}
