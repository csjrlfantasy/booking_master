package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.SalesStatsRequest;
import com.ticket.app.controller.admin.dto.response.*;
import com.ticket.app.service.admin.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员数据统计控制器
 */
@Slf4j
@Tag(name = "管理后台 - 数据统计", description = "数据统计接口")
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final JwtUtils jwtUtils;

    /**
     * 获取概览统计
     * GET /api/admin/stats/overview
     */
    @GetMapping("/overview")
    @Operation(summary = "获取概览统计", description = "获取数据面板概览统计")
    public Result<OverviewStatsResponse> getOverviewStats(HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/stats/overview - 获取概览统计: adminId={}", adminId);

        OverviewStatsResponse response = adminStatsService.getOverviewStats();
        return Result.success(response);
    }

    /**
     * 获取销售统计
     * GET /api/admin/stats/sales
     */
    @GetMapping("/sales")
    @Operation(summary = "获取销售统计", description = "获取销售统计（按天/周/月）")
    public Result<SalesStatsResponse> getSalesStats(
            @Parameter(description = "统计类型: day-日报, week-周报, month-月报")
            @RequestParam(required = false, defaultValue = "day") String type,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/stats/sales - 获取销售统计: adminId={}, type={}", adminId, type);

        SalesStatsRequest queryRequest = new SalesStatsRequest();
        queryRequest.setType(type);

        SalesStatsResponse response = adminStatsService.getSalesStats(queryRequest);
        return Result.success(response);
    }

    /**
     * 获取订单统计
     * GET /api/admin/stats/orders
     */
    @GetMapping("/orders")
    @Operation(summary = "获取订单统计", description = "获取订单统计")
    public Result<OrderStatsResponse> getOrderStats(HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/stats/orders - 获取订单统计: adminId={}", adminId);

        OrderStatsResponse response = adminStatsService.getOrderStats();
        return Result.success(response);
    }

    /**
     * 获取用户统计
     * GET /api/admin/stats/users
     */
    @GetMapping("/users")
    @Operation(summary = "获取用户统计", description = "获取用户增长统计")
    public Result<UserStatsResponse> getUserStats(HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/stats/users - 获取用户统计: adminId={}", adminId);

        UserStatsResponse response = adminStatsService.getUserStats();
        return Result.success(response);
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
