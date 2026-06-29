package com.ticket.app.controller.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券响应
 */
@Schema(description = "用户优惠券响应")
@Data
public class UserCouponResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户优惠券ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "优惠券ID", example = "1")
    private Long couponId;

    @Schema(description = "优惠券名称", example = "满100减50券")
    private String couponName;

    @Schema(description = "优惠券类型: 1-满减券, 2-折扣券, 3-立减券", example = "1")
    private Integer couponType;

    @Schema(description = "优惠券类型描述", example = "满减券")
    private String couponTypeText;

    @Schema(description = "优惠券面值/折扣", example = "50.00")
    private BigDecimal couponValue;

    @Schema(description = "最低订单金额", example = "100.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "使用说明", example = "限购买演唱会门票使用")
    private String description;

    @Schema(description = "有效期开始时间")
    private LocalDateTime validStartTime;

    @Schema(description = "有效期结束时间")
    private LocalDateTime validEndTime;

    @Schema(description = "剩余有效期天数", example = "7")
    private Integer remainingDays;

    @Schema(description = "状态: 0-未使用, 1-已使用, 2-已过期, 3-已失效", example = "0")
    private Integer status;

    @Schema(description = "状态描述", example = "未使用")
    private String statusText;

    @Schema(description = "领取时间")
    private LocalDateTime receiveTime;

    @Schema(description = "使用时间")
    private LocalDateTime useTime;

    @Schema(description = "来源", example = "register")
    private String source;
}
