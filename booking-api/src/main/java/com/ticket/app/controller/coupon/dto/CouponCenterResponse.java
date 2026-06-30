package com.ticket.app.controller.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领券中心响应
 */
@Data
@Schema(description = "领券中心响应")
public class CouponCenterResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠券ID", example = "101")
    private Long couponId;

    @Schema(description = "优惠券名称", example = "满500减30")
    private String couponName;

    @Schema(description = "优惠券类型", example = "1")
    private Integer couponType;

    @Schema(description = "优惠券面值", example = "30.00")
    private BigDecimal couponValue;

    @Schema(description = "最低订单金额", example = "500.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "剩余库存", example = "88")
    private Integer remainCount;

    @Schema(description = "每人限领数量", example = "1")
    private Integer perLimit;

    @Schema(description = "领取开始时间")
    private LocalDateTime receiveStartTime;

    @Schema(description = "领取结束时间")
    private LocalDateTime receiveEndTime;

    @Schema(description = "是否已领取", example = "false")
    private Boolean received;
}
