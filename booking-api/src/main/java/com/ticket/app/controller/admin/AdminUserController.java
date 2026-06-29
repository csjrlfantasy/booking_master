package com.ticket.app.controller.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminAdjustBalanceRequest;
import com.ticket.app.controller.admin.dto.request.AdminUserListRequest;
import com.ticket.app.controller.admin.dto.request.AssignUserRoleRequest;
import com.ticket.app.controller.admin.dto.request.UpdateUserStatusRequest;
import com.ticket.app.controller.admin.dto.response.AdminUserResponse;
import com.ticket.app.controller.admin.dto.response.UserRoleResponse;
import com.ticket.app.controller.user.dto.UserRegisterRequest;
import com.ticket.app.service.admin.AdminUserService;
import com.ticket.app.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户控制器
 */
@Slf4j
@Tag(name = "管理后台 - 用户管理", description = "管理员操作用户的接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    /**
     * 获取用户列表（管理员）
     * GET /api/admin/users
     */
    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "管理员获取用户列表，支持分页、搜索、筛选")
    public Result<PageResult<AdminUserResponse>> getUserList(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "用户类型") @RequestParam(required = false) Integer userType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/users - 获取用户列表: adminId={}, page={}, pageSize={}, keyword={}",
                adminId, page, pageSize, keyword);

        AdminUserListRequest queryRequest = new AdminUserListRequest();
        queryRequest.setPage(page);
        queryRequest.setPageSize(pageSize);
        queryRequest.setKeyword(keyword);
        queryRequest.setStatus(status);
        queryRequest.setUserType(userType);

        PageResult<AdminUserResponse> result = adminUserService.getUserList(queryRequest);
        return Result.success(result);
    }

    /**
     * 新增用户（管理员）
     * POST /api/admin/users
     */
    @PostMapping("/users")
    @Operation(summary = "新增用户", description = "管理员在后台新增普通用户")
    public Result<Long> createUser(
            @Valid @RequestBody UserRegisterRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("POST /api/admin/users - 新增用户: adminId={}, username={}", adminId, request.getUsername());

        request.setSource("admin");
        Long userId = userService.register(request);
        return Result.success("用户创建成功", userId);
    }

    /**
     * 获取用户详情
     * GET /api/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详情")
    public Result<AdminUserResponse> getUserDetail(
            @Parameter(description = "用户ID", required = true) @PathVariable("id") Long userId,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/users/{id} - 获取用户详情: adminId={}, userId={}", adminId, userId);

        AdminUserResponse response = adminUserService.getUserDetail(userId);
        return Result.success(response);
    }

    /**
     * 更新用户信息
     * PUT /api/admin/users/{id}
     */
    @PutMapping("/users/{id}")
    @Operation(summary = "更新用户信息", description = "更新用户信息（管理员）")
    public Result<Void> updateUser(
            @Parameter(description = "用户ID", required = true) @PathVariable("id") Long userId,
            @RequestBody com.ticket.app.controller.user.dto.UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/users/{id} - 更新用户信息: adminId={}, userId={}", adminId, userId);

        // TODO: 实现更新用户信息逻辑
        return Result.success("用户信息更新成功", null);
    }

    /**
     * 启用/禁用用户
     * PUT /api/admin/users/{id}/status
     */
    @PutMapping("/users/{id}/status")
    @Operation(summary = "启用/禁用用户", description = "启用或禁用用户账户")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID", required = true) @PathVariable("id") Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/users/{id}/status - 更新用户状态: adminId={}, userId={}, status={}",
                adminId, userId, request.getStatus());

        adminUserService.updateUserStatus(userId, request);
        return Result.success("用户状态更新成功", null);
    }

    /**
     * 分配用户角色
     * PUT /api/admin/users/{id}/role
     */
    @PutMapping("/users/{id}/role")
    @Operation(summary = "分配用户角色", description = "为用户分配角色")
    public Result<Void> assignUserRole(
            @Parameter(description = "用户ID", required = true) @PathVariable("id") Long userId,
            @Valid @RequestBody AssignUserRoleRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/users/{id}/role - 分配用户角色: adminId={}, userId={}, roleIds={}",
                adminId, userId, request.getRoleIds());

        adminUserService.assignUserRole(userId, request);
        return Result.success("角色分配成功", null);
    }

    @PostMapping("/users/{id}/balance")
    @Operation(summary = "给用户添加余额", description = "管理员给用户微信或支付宝余额充值")
    public Result<Void> addBalance(
            @Parameter(description = "用户ID", required = true) @PathVariable("id") Long userId,
            @RequestBody AdminAdjustBalanceRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("POST /api/admin/users/{id}/balance - 添加余额: adminId={}, userId={}, channel={}, amount={}",
                adminId, userId, request != null ? request.getChannel() : null, request != null ? request.getAmount() : null);

        adminUserService.addBalance(userId, request);
        return Result.success("余额添加成功", null);
    }

    /**
     * 获取角色列表
     * GET /api/admin/roles
     */
    @GetMapping("/roles")
    @Operation(summary = "获取角色列表", description = "获取所有可用角色列表")
    public Result<List<UserRoleResponse>> getRoleList(HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/roles - 获取角色列表: adminId={}", adminId);

        List<UserRoleResponse> roles = adminUserService.getRoleList();
        return Result.success(roles);
    }

    /**
     * 从请求中获取管理员ID
     */
    private Long getAdminIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            throw new com.ticket.app.common.exception.BusinessException(
                    com.ticket.app.common.ResultCode.TOKEN_INVALID, "请先登录");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token.isEmpty()) {
            throw new com.ticket.app.common.exception.BusinessException(
                    com.ticket.app.common.ResultCode.TOKEN_INVALID, "Token无效");
        }

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new com.ticket.app.common.exception.BusinessException(
                        com.ticket.app.common.ResultCode.TOKEN_INVALID, "Token解析失败");
            }
            return userId;
        } catch (com.ticket.app.common.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Token解析异常: {}", e.getMessage());
            throw new com.ticket.app.common.exception.BusinessException(
                    com.ticket.app.common.ResultCode.TOKEN_INVALID, "Token无效");
        }
    }
}
