package com.ticket.app.controller.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券校验请求
 */
@Schema(description = "优惠券校验请求")
@Data
public class ValidateCouponRequest {

    @Schema(description = "优惠券码/优惠券ID", required = true, example = "COUPON123456")
    @NotNull(message = "优惠券码不能为空")
    private String couponCode;

    @Schema(description = "订单总金额", required = true, example = "299.00")
    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    @Schema(description = "票务ID", example = "1")
    private Long ticketId;

    @Schema(description = "票务分类ID", example = "1")
    private Integer categoryId;
}
