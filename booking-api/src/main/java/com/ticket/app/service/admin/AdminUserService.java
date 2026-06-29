package com.ticket.app.service.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.controller.admin.dto.request.AdminAdjustBalanceRequest;
import com.ticket.app.controller.admin.dto.request.AdminUserListRequest;
import com.ticket.app.controller.admin.dto.request.AssignUserRoleRequest;
import com.ticket.app.controller.admin.dto.request.UpdateUserStatusRequest;
import com.ticket.app.controller.admin.dto.response.AdminUserResponse;
import com.ticket.app.controller.admin.dto.response.UserRoleResponse;

/**
 * 管理员用户服务接口
 */
public interface AdminUserService {

    /**
     * 获取用户列表（管理员）
     *
     * @param request 查询参数
     * @return 分页用户列表
     */
    PageResult<AdminUserResponse> getUserList(AdminUserListRequest request);

    /**
     * 获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    AdminUserResponse getUserDetail(Long userId);

    /**
     * 更新用户状态（启用/禁用）
     *
     * @param userId 用户ID
     * @param request 更新请求
     */
    void updateUserStatus(Long userId, UpdateUserStatusRequest request);

    /**
     * 分配用户角色
     *
     * @param userId 用户ID
     * @param request 角色分配请求
     */
    void assignUserRole(Long userId, AssignUserRoleRequest request);

    void addBalance(Long userId, AdminAdjustBalanceRequest request);

    /**
     * 获取角色列表
     *
     * @return 角色列表
     */
    java.util.List<UserRoleResponse> getRoleList();
}
