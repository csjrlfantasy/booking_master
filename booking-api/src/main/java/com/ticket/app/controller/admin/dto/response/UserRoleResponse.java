package com.ticket.app.controller.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户角色响应
 */
@Data
@Schema(description = "用户角色信息")
public class UserRoleResponse {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色描述")
    private String roleDesc;

    @Schema(description = "角色类型")
    private Integer roleType;

    @Schema(description = "状态")
    private Integer status;
}