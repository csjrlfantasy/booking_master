package com.ticket.app.controller.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户登录响应(含Token)
 */
@Schema(description = "用户登录响应(含Token)")
@Data
public class UserTokenResponse extends UserLoginResponse {

    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "token过期时间(秒)", example = "86400")
    private Long expiresIn;
}