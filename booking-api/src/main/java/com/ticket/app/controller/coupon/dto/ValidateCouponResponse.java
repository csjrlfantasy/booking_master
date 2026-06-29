package com.ticket.app.controller.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券校验响应
 */
@Schema(description = "优惠券校验响应")
@Data
public class ValidateCouponResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否可用", example = "true")
    private Boolean usable;

    @Schema(description = "用户优惠券ID", example = "1")
    private Long userCouponId;

    @Schema(description = "优惠券ID", example = "1")
    private Long couponId;

    @Schema(description = "优惠券名称", example = "满100减50券")
    private String couponName;

    @Schema(description = "优惠券类型: 1-满减券, 2-折扣券, 3-立减券", example = "1")
    private Integer couponType;

    @Schema(description = "优惠券面值/折扣", example = "50.00")
    private BigDecimal couponValue;

    @Schema(description = "优惠金额", example = "50.00")
    private BigDecimal discountAmount;

    @Schema(description = "最低订单金额", example = "100.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "不可用原因(当usable=false时返回)", example = "订单金额不满足最低消费")
    private String reason;
}
