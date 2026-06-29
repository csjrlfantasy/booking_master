package com.ticket.app.controller.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 管理员订单列表响应
 */
@Data
@Schema(description = "管理员订单信息")
public class AdminOrderResponse {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "购票联系人")
    private String contactName;

    @Schema(description = "购票联系电话")
    private String contactPhone;

    @Schema(description = "票务名称")
    private String ticketName;

    @Schema(description = "场馆名称")
    private String venueName;

    @Schema(description = "演出日期")
    private LocalDate showDate;

    @Schema(description = "演出时间")
    private LocalTime showTime;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "订单总金额")
    private java.math.BigDecimal totalAmount;

    @Schema(description = "实付金额")
    private java.math.BigDecimal payAmount;

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "订单状态文本")
    private String orderStatusText;

    @Schema(description = "支付状态")
    private Integer payStatus;

    @Schema(description = "支付渠道")
    private String payChannel;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
