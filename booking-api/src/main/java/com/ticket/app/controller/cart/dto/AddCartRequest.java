package com.ticket.app.controller.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 添加购物车请求
 */
@Schema(description = "添加购物车请求")
@Data
public class AddCartRequest {

    @Schema(description = "票务ID", required = true, example = "1")
    @NotNull(message = "票务ID不能为空")
    private Long ticketId;

    @Schema(description = "场次ID", required = true, example = "1")
    @NotNull(message = "场次ID不能为空")
    private Long sessionId;

    @Schema(description = "票档ID", required = true, example = "1")
    @NotNull(message = "票档ID不能为空")
    private Long priceLevelId;

    @Schema(description = "票档名称", required = true, example = "VIP区")
    private String priceLevelName;

    @Schema(description = "购买数量", required = true, example = "2")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少为1")
    private Integer quantity;

    @Schema(description = "单价", required = true, example = "299.00")
    @NotNull(message = "单价不能为空")
    private BigDecimal unitPrice;
}
