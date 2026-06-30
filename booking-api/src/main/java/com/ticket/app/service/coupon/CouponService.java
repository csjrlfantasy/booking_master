package com.ticket.app.service.coupon;

import com.ticket.app.controller.coupon.dto.CouponCenterResponse;
import com.ticket.app.controller.coupon.dto.ValidateCouponRequest;
import com.ticket.app.controller.coupon.dto.ValidateCouponResponse;
import com.ticket.app.controller.coupon.dto.UserCouponResponse;
import com.ticket.app.common.PageResult;

import java.util.List;

/**
 * 优惠券服务接口
 */
public interface CouponService {

    /**
     * 获取领券中心列表
     */
    PageResult<CouponCenterResponse> getCouponCenter(Long userId, Integer page, Integer pageSize);

    /**
     * 领取优惠券
     *
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 用户优惠券ID
     */
    Long receiveCoupon(Long userId, Long couponId);

    /**
     * 获取用户的优惠券列表
     *
     * @param userId 用户ID
     * @param status 优惠券状态筛选 (0-未使用, 1-已使用, 2-已过期, null-全部)
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页优惠券列表
     */
    PageResult<UserCouponResponse> getUserCouponList(Long userId, Integer status, Integer page, Integer pageSize);

    /**
     * 获取用户可用优惠券列表(用于下单)
     *
     * @param userId 用户ID
     * @param orderAmount 订单金额
     * @param ticketId 票务ID
     * @param categoryId 票务分类ID
     * @return 可用优惠券列表
     */
    List<UserCouponResponse> getAvailableCoupons(Long userId, java.math.BigDecimal orderAmount, Long ticketId, Integer categoryId);

    /**
     * 校验用户优惠券是否可用于当前订单
     */
    ValidateCouponResponse validateUserCoupon(Long userId, Long userCouponId, java.math.BigDecimal orderAmount, Long ticketId, Integer categoryId);

    /**
     * 校验优惠券是否可用
     *
     * @param userId 用户ID
     * @param request 校验请求
     * @return 校验结果
     */
    ValidateCouponResponse validateCoupon(Long userId, ValidateCouponRequest request);

    /**
     * 使用优惠券(标记为已使用)
     *
     * @param userId 用户ID
     * @param userCouponId 用户优惠券ID
     * @param orderId 订单ID
     */
    void useCoupon(Long userId, Long userCouponId, Long orderId);

    /**
     * 标记用户优惠券已使用
     */
    void markCouponUsed(Long userId, Long userCouponId, Long orderId);

    /**
     * 退还优惠券(订单取消时)
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     */
    void returnCoupon(Long userId, Long orderId);

    /**
     * 退还优惠券
     */
    void returnCoupon(Long userId, Long orderId, boolean forceExpireIfOutOfWindow);

    /**
     * 获取优惠券详情
     *
     * @param userId 用户ID
     * @param userCouponId 用户优惠券ID
     * @return 优惠券详情
     */
    UserCouponResponse getUserCouponDetail(Long userId, Long userCouponId);
}
