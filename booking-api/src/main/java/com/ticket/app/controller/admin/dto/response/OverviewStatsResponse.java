package com.ticket.app.controller.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 概览统计响应
 */
@Data
@Schema(description = "概览统计数据")
public class OverviewStatsResponse {

    @Schema(description = "今日销售额")
    private java.math.BigDecimal todaySales;

    @Schema(description = "今日订单数")
    private Integer todayOrders;

    @Schema(description = "今日新增用户数")
    private Integer todayUsers;

    @Schema(description = "今日退款数")
    private Integer todayRefunds;

    @Schema(description = "昨日销售额")
    private java.math.BigDecimal yesterdaySales;

    @Schema(description = "销售额增长率(%)")
    private java.math.BigDecimal salesGrowth;

    @Schema(description = "待退款数量")
    private Integer pendingRefundCount;

    @Schema(description = "待支付订单数量")
    private Integer pendingOrderCount;
}