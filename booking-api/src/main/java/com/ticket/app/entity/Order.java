package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 订单实体类
 */
@Data
@TableName("`order`")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 票务ID
     */
    private Long ticketId;

    /**
     * 场次ID
     */
    private Long sessionId;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 票务名称
     */
    private String ticketName;

    /**
     * 演出日期
     */
    private LocalDate showDate;

    /**
     * 演出时间
     */
    private LocalTime showTime;

    /**
     * 总数量
     */
    private Integer totalQuantity;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 用户优惠券ID
     */
    private Long userCouponId;

    /**
     * 优惠券名称
     */
    private String couponName;

    /**
     * 优惠券优惠金额
     */
    private BigDecimal couponAmount;

    /**
     * 积分抵扣金额
     */
    private BigDecimal pointDiscount;

    /**
     * 订单状态: 1-待支付, 2-已支付, 3-已完成, 4-已取消, 5-已退款, 6-退款中
     */
    private Integer orderStatus;

    /**
     * 支付状态: 0-未支付, 1-已支付, 2-已退款
     */
    private Integer payStatus;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 支付渠道: alipay, wechat, bank
     */
    private String payChannel;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人手机
     */
    private String contactPhone;

    /**
     * 联系人身份证
     */
    private String contactIdCard;

    /**
     * 用户备注
     */
    private String userRemark;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 下单IP
     */
    private String ipAddress;

    /**
     * 订单来源
     */
    private String source;

    /** Persistent fact: shared inventory has been returned for this order. */
    @com.baomidou.mybatisplus.annotation.TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER,
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private Boolean inventoryRestored;

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

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    // ==================== 订单状态常量 ====================
    public static final Integer STATUS_PENDING_PAY = 1;      // 待支付
    public static final Integer STATUS_PAID = 2;             // 已支付
    public static final Integer STATUS_COMPLETED = 3;       // 已完成
    public static final Integer STATUS_CANCELLED = 4;       // 已取消
    public static final Integer STATUS_REFUNDED = 5;         // 已退款
    public static final Integer STATUS_REFUNDING = 6;        // 退款中

    // ==================== 支付状态常量 ====================
    public static final Integer PAY_STATUS_UNPAID = 0;       // 未支付
    public static final Integer PAY_STATUS_PAID = 1;        // 已支付
    public static final Integer PAY_STATUS_REFUNDED = 2;    // 已退款
}
