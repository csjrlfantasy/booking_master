package com.ticket.app.controller.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统配置响应
 */
@Data
@Schema(description = "系统配置信息")
public class SystemConfigResponse {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置类型")
    private String configType;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置描述")
    private String configDesc;

    @Schema(description = "配置分组")
    private String configGroup;

    @Schema(description = "是否系统配置")
    private Integer isSystem;
}