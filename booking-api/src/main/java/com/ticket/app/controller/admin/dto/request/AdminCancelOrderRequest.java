package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * 管理员取消订单请求参数
 */
@Data
public class AdminCancelOrderRequest {

    @Parameter(description = "取消原因")
    private String cancelReason;
}