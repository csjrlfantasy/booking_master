package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量更新系统配置请求参数
 */
@Data
public class BatchUpdateConfigRequest {

    @Parameter(description = "配置列表", required = true)
    @NotEmpty(message = "配置列表不能为空")
    private List<ConfigItem> configs;

    @Data
    public static class ConfigItem {
        private Long id;
        private String configValue;
    }
}