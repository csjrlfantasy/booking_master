package com.ticket.app.controller.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款结果响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "退款结果响应")
public class RefundResultResponse {

    @Schema(description = "退款记录ID")
    private Long refundId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "原支付金额")
    private BigDecimal payAmount;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "退款状态: APPLYING-申请中, PROCESSING-处理中, SUCCESS-退款成功, FAILED-退款失败, REJECTED-退款被拒绝")
    private String status;

    @Schema(description = "退款状态文本")
    private String statusText;

    @Schema(description = "退款类型: USER-用户申请, SYSTEM-系统退款")
    private String refundType;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "处理时间")
    private LocalDateTime processTime;

    @Schema(description = "退款完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "第三方退款单号")
    private String thirdRefundNo;

    @Schema(description = "拒绝原因")
    private String rejectReason;
}
