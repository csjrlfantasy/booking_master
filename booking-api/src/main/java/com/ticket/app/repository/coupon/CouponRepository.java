package com.ticket.app.repository.coupon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 优惠券Repository
 */
@Mapper
public interface CouponRepository extends BaseMapper<Coupon> {

    /**
     * 查询可领取的优惠券列表
     *
     * @return 优惠券列表
     */
    List<Coupon> selectAvailableCoupons();

    /**
     * 根据ID查询优惠券
     *
     * @param id 优惠券ID
     * @return 优惠券
     */
    Coupon selectById(@Param("id") Long id);

    /**
     * 减少优惠券剩余数量
     *
     * @param couponId 优惠券ID
     * @return 影响行数
     */
    int decrementRemainCount(@Param("couponId") Long couponId);

    /**
     * 增加优惠券剩余数量
     *
     * @param couponId 优惠券ID
     * @return 影响行数
     */
    int incrementRemainCount(@Param("couponId") Long couponId);
}
