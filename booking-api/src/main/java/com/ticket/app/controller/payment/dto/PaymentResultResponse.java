package com.ticket.app.controller.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付结果响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "支付结果响应")
public class PaymentResultResponse {

    @Schema(description = "支付记录ID")
    private Long paymentId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "支付状态: PENDING-待支付, SUCCESS-支付成功, FAILED-支付失败, CLOSED-已关闭")
    private String status;

    @Schema(description = "支付状态文本")
    private String statusText;

    @Schema(description = "支付渠道")
    private String payChannel;

    @Schema(description = "第三方交易号")
    private String tradeNo;

    @Schema(description = "支付二维码URL(扫码支付时返回)")
    private String qrCodeUrl;

    @Schema(description = "支付跳转链接(H5支付时返回)")
    private String payUrl;

    @Schema(description = "支付过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "支付成功时间")
    private LocalDateTime payTime;

    @Schema(description = "错误码(支付失败时)")
    private String errorCode;

    @Schema(description = "错误信息(支付失败时)")
    private String errorMessage;
}
