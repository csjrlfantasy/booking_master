package com.ticket.app.controller.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 订单统计响应
 */
@Data
@Schema(description = "订单统计数据")
public class OrderStatsResponse {

    @Schema(description = "总订单数")
    private Integer totalOrders;

    @Schema(description = "待支付订单数")
    private Integer pendingPayOrders;

    @Schema(description = "已支付订单数")
    private Integer paidOrders;

    @Schema(description = "已完成订单数")
    private Integer completedOrders;

    @Schema(description = "已取消订单数")
    private Integer cancelledOrders;

    @Schema(description = "退款中订单数")
    private Integer refundingOrders;

    @Schema(description = "订单状态分布")
    private List<StatusItem> orderStatusDistribution;

    @Data
    @Schema(description = "状态分布项")
    public static class StatusItem {
        private Integer status;
        private Integer count;
    }
}