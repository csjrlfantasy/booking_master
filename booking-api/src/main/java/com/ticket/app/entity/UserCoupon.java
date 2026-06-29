package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体类
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户优惠券ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 优惠券名称
     */
    private String couponName;

    /**
     * 优惠券类型
     */
    private Integer couponType;

    /**
     * 优惠券面值
     */
    private BigDecimal couponValue;

    /**
     * 最低订单金额
     */
    private BigDecimal minOrderAmount;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 领取时间
     */
    private LocalDateTime receiveTime;

    /**
     * 有效开始时间
     */
    private LocalDateTime validStartTime;

    /**
     * 有效结束时间
     */
    private LocalDateTime validEndTime;

    /**
     * 使用时间
     */
    private LocalDateTime useTime;

    /**
     * 状态: 0-未使用, 1-已使用, 2-已过期, 3-已失效
     */
    private Integer status;

    /**
     * 来源: register, order, activity, admin
     */
    private String source;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 状态常量 ====================
    public static final Integer STATUS_UNUSED = 0;        // 未使用
    public static final Integer STATUS_USED = 1;          // 已使用
    public static final Integer STATUS_EXPIRED = 2;       // 已过期
    public static final Integer STATUS_DISABLED = 3;      // 已失效

    // ==================== 来源常量 ====================
    public static final String SOURCE_REGISTER = "register";
    public static final String SOURCE_ORDER = "order";
    public static final String SOURCE_ACTIVITY = "activity";
    public static final String SOURCE_ADMIN = "admin";
    public static final String SOURCE_MANUAL = "manual";
}
