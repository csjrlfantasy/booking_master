package com.ticket.app.controller.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 订单列表项响应
 */
@Schema(description = "订单列表项响应")
@Data
public class OrderListItemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", example = "1")
    private Long id;

    @Schema(description = "订单号", example = "ORD2026042800001")
    private String orderNo;

    @Schema(description = "票务ID", example = "1")
    private Long ticketId;

    @Schema(description = "票务名称", example = "周杰伦演唱会")
    private String ticketName;

    @Schema(description = "票务海报", example = "https://example.com/poster.jpg")
    private String ticketPoster;

    @Schema(description = "场馆名称", example = "北京鸟巢")
    private String venueName;

    @Schema(description = "演出日期", example = "2026-05-01")
    private LocalDate showDate;

    @Schema(description = "演出时间", example = "19:30:00")
    private LocalTime showTime;

    @Schema(description = "总数量", example = "2")
    private Integer totalQuantity;

    @Schema(description = "实付金额", example = "548.00")
    private BigDecimal payAmount;

    @Schema(description = "订单状态: 1-待支付, 2-已支付, 3-已完成, 4-已取消, 5-已退款, 6-退款中", example = "1")
    private Integer orderStatus;

    @Schema(description = "订单状态描述", example = "待支付")
    private String orderStatusText;

    @Schema(description = "支付状态: 0-未支付, 1-已支付, 2-已退款", example = "0")
    private Integer payStatus;

    @Schema(description = "订单过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
