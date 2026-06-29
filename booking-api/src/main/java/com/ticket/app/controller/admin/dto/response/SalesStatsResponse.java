package com.ticket.app.controller.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 销售统计响应
 */
@Data
@Schema(description = "销售统计数据")
public class SalesStatsResponse {

    @Schema(description = "总销售额")
    private java.math.BigDecimal totalSales;

    @Schema(description = "总订单数")
    private Integer totalOrders;

    @Schema(description = "平均订单金额")
    private java.math.BigDecimal averageOrderAmount;

    @Schema(description = "趋势数据列表")
    private List<TrendItem> trendList;

    @Data
    @Schema(description = "趋势数据项")
    public static class TrendItem {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "销售额")
        private java.math.BigDecimal sales;

        @Schema(description = "订单数")
        private Integer orderCount;

        @Schema(description = "退款数")
        private Integer refundCount;

        @Schema(description = "退款金额")
        private java.math.BigDecimal refundAmount;
    }
}