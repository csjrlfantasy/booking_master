package com.ticket.app.controller.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "Coupon validation request")
@Data
public class ValidateCouponRequest {

    @Schema(description = "Claimed user coupon ID", required = true, example = "301")
    @NotNull(message = "userCouponId is required")
    private Long userCouponId;

    @Schema(description = "Legacy coupon code or coupon ID field", example = "COUPON123456")
    private String couponCode;

    @Schema(description = "Order amount", required = true, example = "299.00")
    @NotNull(message = "orderAmount is required")
    private BigDecimal orderAmount;

    @Schema(description = "Ticket ID", example = "1")
    private Long ticketId;

    @Schema(description = "Ticket category ID", example = "1")
    private Integer categoryId;
}