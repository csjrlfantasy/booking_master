package com.ticket.app.controller.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.BatchUpdateConfigRequest;
import com.ticket.app.controller.admin.dto.request.NotificationConfigRequest;
import com.ticket.app.controller.admin.dto.request.UpdateConfigRequest;
import com.ticket.app.controller.admin.dto.response.SystemConfigResponse;
import com.ticket.app.service.admin.AdminConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员系统配置控制器
 */
@Slf4j
@Tag(name = "管理后台 - 系统配置", description = "系统配置管理接口")
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigService adminConfigService;
    private final JwtUtils jwtUtils;

    /**
     * 获取配置列表
     * GET /api/admin/config
     */
    @GetMapping
    @Operation(summary = "获取配置列表", description = "获取系统配置列表")
    public Result<PageResult<SystemConfigResponse>> getConfigList(
            @Parameter(description = "配置分组") @RequestParam(required = false) String configGroup,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/config - 获取配置列表: adminId={}, configGroup={}", adminId, configGroup);

        PageResult<SystemConfigResponse> result = adminConfigService.getConfigList(configGroup, keyword);
        return Result.success(result);
    }

    /**
     * 获取单个配置
     * GET /api/admin/config/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取配置详情", description = "根据ID获取配置详情")
    public Result<SystemConfigResponse> getConfig(
            @Parameter(description = "配置ID", required = true) @PathVariable("id") Long configId,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/config/{id} - 获取配置详情: adminId={}, configId={}", adminId, configId);

        SystemConfigResponse response = adminConfigService.getConfig(configId);
        return Result.success(response);
    }

    /**
     * 更新系统配置
     * PUT /api/admin/config/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新配置", description = "更新系统配置")
    public Result<Void> updateConfig(
            @Parameter(description = "配置ID", required = true) @PathVariable("id") Long configId,
            @Valid @RequestBody UpdateConfigRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/config/{id} - 更新配置: adminId={}, configId={}", adminId, configId);

        adminConfigService.updateConfig(configId, request);
        return Result.success("配置更新成功", null);
    }

    /**
     * 批量更新配置
     * PUT /api/admin/config/batch
     */
    @PutMapping("/batch")
    @Operation(summary = "批量更新配置", description = "批量更新系统配置")
    public Result<Void> batchUpdateConfig(
            @Valid @RequestBody BatchUpdateConfigRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/config/batch - 批量更新配置: adminId={}, count={}",
                adminId, request.getConfigs().size());

        adminConfigService.batchUpdateConfig(request);
        return Result.success("批量更新成功", null);
    }

    /**
     * 获取通知配置
     * GET /api/admin/config/notification
     */
    @GetMapping("/notification")
    @Operation(summary = "获取通知配置", description = "获取通知配置")
    public Result<NotificationConfigRequest> getNotificationConfig(HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/config/notification - 获取通知配置: adminId={}", adminId);

        NotificationConfigRequest response = adminConfigService.getNotificationConfig();
        return Result.success(response);
    }

    /**
     * 更新通知配置
     * PUT /api/admin/config/notification
     */
    @PutMapping("/notification")
    @Operation(summary = "更新通知配置", description = "更新通知配置")
    public Result<Void> updateNotificationConfig(
            @Valid @RequestBody NotificationConfigRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/config/notification - 更新通知配置: adminId={}", adminId);

        adminConfigService.updateNotificationConfig(request);
        return Result.success("通知配置更新成功", null);
    }

    /**
     * 从请求中获取管理员ID
     */
    private Long getAdminIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "请先登录");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new BusinessException(ResultCode.TOKEN_INVALID, "Token解析失败");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Token解析异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
    }
}
