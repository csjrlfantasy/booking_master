package com.ticket.app.controller.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员用户列表响应
 */
@Data
@Schema(description = "管理员用户信息")
public class AdminUserResponse {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "用户类型")
    private Integer userType;

    @Schema(description = "用户类型文本")
    private String userTypeText;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "累计消费金额")
    private BigDecimal totalOrderAmount;

    @Schema(description = "微信余额")
    private BigDecimal wechatBalance;

    @Schema(description = "支付宝余额")
    private BigDecimal alipayBalance;

    @Schema(description = "订单数量")
    private Integer orderCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
