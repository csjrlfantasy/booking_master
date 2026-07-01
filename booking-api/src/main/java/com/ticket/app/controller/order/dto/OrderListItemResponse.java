package com.ticket.app.controller.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Order list item response")
@Data
public class OrderListItemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Order ID", example = "1")
    private Long id;

    @Schema(description = "Order number", example = "ORD2026042800001")
    private String orderNo;

    @Schema(description = "Ticket ID", example = "1")
    private Long ticketId;

    @Schema(description = "Ticket name", example = "Concert")
    private String ticketName;

    @Schema(description = "Ticket poster", example = "https://example.com/poster.jpg")
    private String ticketPoster;

    @Schema(description = "Venue name", example = "Arena")
    private String venueName;

    @Schema(description = "Show date", example = "2026-05-01")
    private LocalDate showDate;

    @Schema(description = "Show time", example = "19:30:00")
    private LocalTime showTime;

    @Schema(description = "Total quantity", example = "2")
    private Integer totalQuantity;

    @Schema(description = "Pay amount", example = "548.00")
    private BigDecimal payAmount;

    @Schema(description = "Coupon template ID", example = "101")
    private Long couponId;

    @Schema(description = "User coupon ID", example = "301")
    private Long userCouponId;

    @Schema(description = "Coupon name", example = "minus-30")
    private String couponName;

    @Schema(description = "Coupon amount", example = "30.00")
    private BigDecimal couponAmount;

    @Schema(description = "Order status", example = "1")
    private Integer orderStatus;

    @Schema(description = "Order status text", example = "pending_pay")
    private String orderStatusText;

    @Schema(description = "Pay status", example = "0")
    private Integer payStatus;

    @Schema(description = "Expire time")
    private LocalDateTime expireTime;

    @Schema(description = "Created time")
    private LocalDateTime createdAt;
}
