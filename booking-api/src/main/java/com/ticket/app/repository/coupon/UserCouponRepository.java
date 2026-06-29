package com.ticket.app.repository.coupon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户优惠券Repository
 */
@Mapper
public interface UserCouponRepository extends BaseMapper<UserCoupon> {

    /**
     * 根据用户ID查询可用优惠券列表
     *
     * @param userId 用户ID
     * @param ticketId 票务ID
     * @param orderAmount 订单金额
     * @return 可用优惠券列表
     */
    List<UserCoupon> selectAvailableCoupons(@Param("userId") Long userId,
                                           @Param("ticketId") Long ticketId,
                                           @Param("orderAmount") BigDecimal orderAmount);

    /**
     * 根据用户ID查询所有优惠券
     *
     * @param userId 用户ID
     * @param status 状态(可选)
     * @return 优惠券列表
     */
    List<UserCoupon> selectByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 根据用户ID和优惠券ID查询用户优惠券
     *
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 用户优惠券
     */
    UserCoupon selectByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 统计用户已领取某优惠券的数量
     *
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 已领取数量
     */
    int countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);
}
