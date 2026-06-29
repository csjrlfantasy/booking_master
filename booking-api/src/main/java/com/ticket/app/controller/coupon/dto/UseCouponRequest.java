package com.ticket.app.controller.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 使用优惠券请求
 */
@Schema(description = "使用优惠券请求")
@Data
public class UseCouponRequest {

    @Schema(description = "用户优惠券ID", required = true, example = "1")
    @NotNull(message = "用户优惠券ID不能为空")
    private Long userCouponId;

    @Schema(description = "订单ID", example = "1")
    private Long orderId;
}
