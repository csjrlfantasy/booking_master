package com.ticket.app.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.admin.dto.request.AdminAdjustBalanceRequest;
import com.ticket.app.controller.admin.dto.request.AdminUserListRequest;
import com.ticket.app.controller.admin.dto.request.AssignUserRoleRequest;
import com.ticket.app.controller.admin.dto.request.UpdateUserStatusRequest;
import com.ticket.app.controller.admin.dto.response.AdminUserResponse;
import com.ticket.app.controller.admin.dto.response.UserRoleResponse;
import com.ticket.app.entity.User;
import com.ticket.app.repository.admin.AdminUserRepository;
import com.ticket.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;

    @Override
    public PageResult<AdminUserResponse> getUserList(AdminUserListRequest request) {
        Integer page = request.getPage() != null ? request.getPage() : 1;
        Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        // 构建查询条件
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (request.getStartDate() != null) {
            startDate = request.getStartDate().atStartOfDay();
        }
        if (request.getEndDate() != null) {
            endDate = request.getEndDate().atTime(LocalTime.MAX);
        }

        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(User::getDeletedAt);
        if (org.springframework.util.StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(User::getStatus, request.getStatus());
        }
        if (request.getUserType() != null) {
            queryWrapper.eq(User::getUserType, request.getUserType());
        }
        if (startDate != null) {
            queryWrapper.ge(User::getCreatedAt, startDate);
        }
        if (endDate != null) {
            queryWrapper.le(User::getCreatedAt, endDate);
        }
        queryWrapper.orderByDesc(User::getCreatedAt);

        IPage<User> pageResult = adminUserRepository.selectPage(pageParam, queryWrapper);

        // 转换为响应对象
        List<AdminUserResponse> responseList = new ArrayList<>();
        for (User user : pageResult.getRecords()) {
            AdminUserResponse response = convertToResponse(user);
            response.setOrderCount(nullToZero(adminUserRepository.selectUserOrderCount(user.getId())));
            response.setTotalOrderAmount(nullToZero(adminUserRepository.selectUserPaidOrderAmount(user.getId())));
            responseList.add(response);
        }

        return PageResult.of(
                responseList,
                pageResult.getTotal(),
                page.intValue(),
                pageSize
        );
    }

    @Override
    public AdminUserResponse getUserDetail(Long userId) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return convertToResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);

        log.info("更新用户状态: userId={}, status={}", userId, request.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRole(Long userId, AssignUserRoleRequest request) {
        // TODO: 实现角色分配逻辑
        log.info("分配用户角色: userId={}, roleIds={}", userId, request.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBalance(Long userId, AdminAdjustBalanceRequest request) {
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "充值金额必须大于0");
        }

        String channel = request.getChannel() == null ? "" : request.getChannel().trim().toLowerCase();
        if (!"wechat".equals(channel) && !"alipay".equals(channel)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "充值渠道只支持wechat或alipay");
        }

        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        if ("wechat".equals(channel)) {
            user.setWechatBalance(nullToZero(user.getWechatBalance()).add(request.getAmount()));
        } else {
            user.setAlipayBalance(nullToZero(user.getAlipayBalance()).add(request.getAmount()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);

        log.info("后台给用户加余额: userId={}, channel={}, amount={}", userId, channel, request.getAmount());
    }

    @Override
    public List<UserRoleResponse> getRoleList() {
        // TODO: 从角色表查询
        List<UserRoleResponse> responses = new ArrayList<>();

        // 添加默认角色
        UserRoleResponse adminRole = new UserRoleResponse();
        adminRole.setId(1L);
        adminRole.setRoleName("超级管理员");
        adminRole.setRoleCode("SUPER_ADMIN");
        adminRole.setRoleDesc("拥有所有权限");
        adminRole.setRoleType(1);
        adminRole.setStatus(1);
        responses.add(adminRole);

        UserRoleResponse operatorRole = new UserRoleResponse();
        operatorRole.setId(2L);
        operatorRole.setRoleName("运营管理员");
        operatorRole.setRoleCode("OPERATOR");
        operatorRole.setRoleDesc("用户管理、订单管理、数据统计");
        operatorRole.setRoleType(1);
        operatorRole.setStatus(1);
        responses.add(operatorRole);

        UserRoleResponse csRole = new UserRoleResponse();
        csRole.setId(3L);
        csRole.setRoleName("客服");
        csRole.setRoleCode("CUSTOMER_SERVICE");
        csRole.setRoleDesc("订单管理、查看统计数据");
        csRole.setRoleType(1);
        csRole.setStatus(1);
        responses.add(csRole);

        return responses;
    }

    /**
     * 转换为响应对象
     */
    private AdminUserResponse convertToResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setUserType(user.getUserType());
        response.setUserTypeText(getUserTypeText(user.getUserType()));
        response.setStatus(user.getStatus());
        response.setStatusText(getStatusText(user.getStatus()));
        response.setTotalOrderAmount(user.getTotalOrderAmount());
        response.setWechatBalance(user.getWechatBalance());
        response.setAlipayBalance(user.getAlipayBalance());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Integer nullToZero(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * 获取用户类型文本
     */
    private String getUserTypeText(Integer userType) {
        if (userType == null) return "未知";
        return switch (userType) {
            case 1 -> "普通用户";
            case 2 -> "VIP用户";
            case 3 -> "企业用户";
            default -> "未知";
        };
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "禁用";
            case 1 -> "正常";
            case 2 -> "待激活";
            default -> "未知";
        };
    }
}
