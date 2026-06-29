package com.ticket.app.controller.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新购物车请求
 */
@Schema(description = "更新购物车请求")
@Data
public class UpdateCartRequest {

    @Schema(description = "购物车ID", required = true, example = "1")
    @NotNull(message = "购物车ID不能为空")
    private Long cartId;

    @Schema(description = "购买数量", required = true, example = "3")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少为1")
    private Integer quantity;
}
