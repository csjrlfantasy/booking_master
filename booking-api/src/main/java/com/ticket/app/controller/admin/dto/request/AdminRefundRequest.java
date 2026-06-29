package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员强制退款请求参数
 */
@Data
public class AdminRefundRequest {

    @Parameter(description = "退款金额")
    private BigDecimal refundAmount;

    @Parameter(description = "退款原因")
    private String reason;

    @Parameter(description = "凭证图片URL列表(JSON数组)")
    private String proofImages;
}