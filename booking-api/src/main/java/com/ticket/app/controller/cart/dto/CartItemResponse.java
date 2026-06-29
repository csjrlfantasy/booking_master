package com.ticket.app.controller.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项响应
 */
@Schema(description = "购物车项响应")
@Data
public class CartItemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "票务ID", example = "1")
    private Long ticketId;

    @Schema(description = "票务名称", example = "周杰伦演唱会")
    private String ticketName;

    @Schema(description = "票务海报", example = "https://example.com/poster.jpg")
    private String ticketPoster;

    @Schema(description = "场次ID", example = "1")
    private Long sessionId;

    @Schema(description = "场次时间", example = "2026-05-01 19:30:00")
    private LocalDateTime sessionTime;

    @Schema(description = "票档ID", example = "1")
    private Long priceLevelId;

    @Schema(description = "票档名称", example = "VIP区")
    private String priceLevelName;

    @Schema(description = "购买数量", example = "2")
    private Integer quantity;

    @Schema(description = "单价", example = "299.00")
    private BigDecimal unitPrice;

    @Schema(description = "小计金额", example = "598.00")
    private BigDecimal subtotal;

    @Schema(description = "状态: 1-有效, 0-已失效", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
