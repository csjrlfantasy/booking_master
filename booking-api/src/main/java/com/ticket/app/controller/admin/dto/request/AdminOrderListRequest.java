package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 管理员订单列表请求参数
 */
@Data
public class AdminOrderListRequest {

    @Parameter(description = "页码", example = "1")
    private Integer page = 1;

    @Parameter(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Parameter(description = "订单号")
    private String orderNo;

    @Parameter(description = "用户ID")
    private Long userId;

    @Parameter(description = "订单状态: 1-待支付, 2-已支付, 3-已完成, 4-已取消, 5-已退款, 6-退款中")
    private Integer status;

    @Parameter(description = "支付状态: 0-未支付, 1-已支付, 2-已退款")
    private Integer payStatus;

    @Parameter(description = "订单开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Parameter(description = "订单结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}