package com.ticket.app.controller.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付请求
 */
@Data
@Schema(description = "创建支付请求")
public class CreatePaymentRequest {

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号", required = true)
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    @Schema(description = "支付金额", required = true)
    private BigDecimal amount;

    @NotBlank(message = "支付渠道不能为空")
    @Schema(description = "支付渠道: alipay, wechat, bank, unionpay", required = true)
    private String payChannel;

    @Schema(description = "支付客户端IP")
    private String clientIp;

    @Schema(description = "支付完成后跳转URL")
    private String returnUrl;

    @Schema(description = "支付场景: APP, H5, WEB, NATIVE")
    private String paymentScene = "H5";
}
