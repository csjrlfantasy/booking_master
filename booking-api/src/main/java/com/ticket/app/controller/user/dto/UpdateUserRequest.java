package com.ticket.app.controller.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求
 */
@Schema(description = "更新用户信息请求")
@Data
public class UpdateUserRequest {

    @Size(min = 1, max = 50, message = "昵称长度不能超过50个字符")
    @Schema(description = "昵称", example = "新昵称")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13900139000")
    private String phone;

    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "newemail@example.com")
    private String email;

    @Schema(description = "性别: 0-未知, 1-男, 2-女", example = "1")
    private Integer gender;

    @Schema(description = "生日", example = "1990-01-01")
    private String birthday;

    @Schema(description = "头像URL", example = "https://example.com/new-avatar.jpg")
    private String avatar;
}
