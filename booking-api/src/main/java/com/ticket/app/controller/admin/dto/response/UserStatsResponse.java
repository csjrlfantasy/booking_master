package com.ticket.app.controller.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户统计响应
 */
@Data
@Schema(description = "用户统计数据")
public class UserStatsResponse {

    @Schema(description = "总用户数")
    private Integer totalUsers;

    @Schema(description = "活跃用户数")
    private Integer activeUsers;

    @Schema(description = "今日新增用户数")
    private Integer newUsersToday;

    @Schema(description = "本周新增用户数")
    private Integer newUsersThisWeek;

    @Schema(description = "本月新增用户数")
    private Integer newUsersThisMonth;

    @Schema(description = "VIP用户数")
    private Integer vipUsers;

    @Schema(description = "用户增长趋势")
    private List<TrendItem> userGrowthTrend;

    @Data
    @Schema(description = "增长趋势项")
    public static class TrendItem {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "用户数")
        private Integer count;
    }
}