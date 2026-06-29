package com.ticket.app.controller.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建订单响应
 */
@Schema(description = "创建订单响应")
@Data
public class CreateOrderResponse {

    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @Schema(description = "订单号", example = "ORD2026042800001")
    private String orderNo;

    @Schema(description = "订单总金额", example = "598.00")
    private java.math.BigDecimal totalAmount;

    @Schema(description = "优惠金额", example = "50.00")
    private java.math.BigDecimal discountAmount;

    @Schema(description = "实付金额", example = "548.00")
    private java.math.BigDecimal payAmount;

    @Schema(description = "订单过期时间")
    private java.time.LocalDateTime expireTime;
}
