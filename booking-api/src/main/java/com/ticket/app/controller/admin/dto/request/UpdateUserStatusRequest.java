package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户状态请求参数
 */
@Data
public class UpdateUserStatusRequest {

    @Parameter(description = "用户状态: 0-禁用, 1-正常", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;
}