package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 分配用户角色请求参数
 */
@Data
public class AssignUserRoleRequest {

    @Parameter(description = "角色ID列表", required = true)
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}