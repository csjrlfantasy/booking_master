package com.ticket.app.controller.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取消订单请求
 */
@Schema(description = "取消订单请求")
@Data
public class CancelOrderRequest {

    /**
     * 订单ID (从路径参数获取，可选)
     */
    private Long orderId;

    /**
     * 取消原因
     */
    @Schema(description = "取消原因", example = "不想要了")
    private String cancelReason;
}
