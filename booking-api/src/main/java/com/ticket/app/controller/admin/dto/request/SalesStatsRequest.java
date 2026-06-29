package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 销售统计请求参数
 */
@Data
public class SalesStatsRequest {

    @Parameter(description = "统计类型: day-日报, week-周报, month-月报")
    private String type = "day";

    @Parameter(description = "开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Parameter(description = "结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}