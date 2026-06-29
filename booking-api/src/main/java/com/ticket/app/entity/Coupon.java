package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 优惠券实体类
 */
@Data
@TableName("coupon")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 优惠券ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠券类型: 1-满减券, 2-折扣券, 3-立减券
     */
    private Integer couponType;

    /**
     * 优惠券面值/折扣
     */
    private BigDecimal couponValue;

    /**
     * 最低订单金额
     */
    private BigDecimal minOrderAmount;

    /**
     * 最高优惠金额(折扣券用)
     */
    private BigDecimal maxDiscountAmount;

    /**
     * 发放总量
     */
    private Integer totalCount;

    /**
     * 剩余数量
     */
    private Integer remainCount;

    /**
     * 每人限领数量
     */
    private Integer perLimit;

    /**
     * 领取方式: 1-主动领取, 2-自动发放, 3-活动发放
     */
    private Integer receiveType;

    /**
     * 有效期类型: 1-固定日期, 2-领取后N天生效
     */
    private Integer validityType;

    /**
     * 有效期开始日期
     */
    private LocalDate validStartDate;

    /**
     * 有效期结束日期
     */
    private LocalDate validEndDate;

    /**
     * 领取后有效天数
     */
    private Integer validDays;

    /**
     * 适用范围: 1-全场通用, 2-指定票务, 3-指定分类
     */
    private Integer applicableScope;

    /**
     * 适用的票务ID列表
     */
    private String applicableTickets;

    /**
     * 适用的分类ID列表
     */
    private String applicableCategories;

    /**
     * 使用说明
     */
    private String description;

    /**
     * 使用须知
     */
    private String notice;

    /**
     * 领取开始时间
     */
    private LocalDateTime receiveStartTime;

    /**
     * 领取结束时间
     */
    private LocalDateTime receiveEndTime;

    /**
     * 状态: 0-未激活, 1-进行中, 2-已结束
     */
    private Integer status;

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

    // ==================== 优惠券类型常量 ====================
    public static final Integer TYPE_FULL_REDUCE = 1;    // 满减券
    public static final Integer TYPE_DISCOUNT = 2;       // 折扣券
    public static final Integer TYPE_CASH = 3;            // 立减券

    // ==================== 领取方式常量 ====================
    public static final Integer RECEIVE_TYPE_MANUAL = 1;      // 主动领取
    public static final Integer RECEIVE_TYPE_AUTO = 2;        // 自动发放
    public static final Integer RECEIVE_TYPE_ACTIVITY = 3;    // 活动发放

    // ==================== 有效期类型常量 ====================
    public static final Integer VALID_TYPE_FIXED = 1;     // 固定日期
    public static final Integer VALID_TYPE_DYNAMIC = 2;   // 领取后N天生效

    // ==================== 适用范围常量 ====================
    public static final Integer SCOPE_ALL = 1;            // 全场通用
    public static final Integer SCOPE_TICKET = 2;         // 指定票务
    public static final Integer SCOPE_CATEGORY = 3;       // 指定分类

    // ==================== 状态常量 ====================
    public static final Integer STATUS_INACTIVE = 0;      // 未激活
    public static final Integer STATUS_ACTIVE = 1;        // 进行中
    public static final Integer STATUS_ENDED = 2;         // 已结束
}
