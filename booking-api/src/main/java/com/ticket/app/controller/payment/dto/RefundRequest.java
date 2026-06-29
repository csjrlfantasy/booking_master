package com.ticket.app.controller.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 退款请求
 */
@Data
@Schema(description = "退款请求")
public class RefundRequest {

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号", required = true)
    private String orderNo;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "退款金额(不传则全额退款)")
    private String refundAmount;

    @Schema(description = "退款凭证图片URL(可选)")
    private String proofImages;
}
