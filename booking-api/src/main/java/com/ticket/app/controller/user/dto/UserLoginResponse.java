package com.ticket.app.controller.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户登录响应
 */
@Schema(description = "用户登录响应")
@Data
public class UserLoginResponse {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "用户类型: 1-普通用户, 2-VIP用户, 3-企业用户", example = "1")
    private Integer userType;

    @Schema(description = "用户等级", example = "1")
    private Integer userLevel;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "微信余额", example = "10000.00")
    private BigDecimal wechatBalance;

    @Schema(description = "支付宝余额", example = "10000.00")
    private BigDecimal alipayBalance;
}
