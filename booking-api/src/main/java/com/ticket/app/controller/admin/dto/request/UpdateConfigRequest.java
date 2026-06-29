package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * 更新单个系统配置请求参数
 */
@Data
public class UpdateConfigRequest {

    @Parameter(description = "配置值")
    private String configValue;

    @Parameter(description = "配置描述")
    private String configDesc;
}