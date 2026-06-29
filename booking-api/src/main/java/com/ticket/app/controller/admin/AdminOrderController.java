package com.ticket.app.controller.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminCancelOrderRequest;
import com.ticket.app.controller.admin.dto.request.AdminOrderListRequest;
import com.ticket.app.controller.admin.dto.request.AdminRefundRequest;
import com.ticket.app.controller.admin.dto.response.AdminOrderResponse;
import com.ticket.app.controller.admin.dto.response.OrderStatsResponse;
import com.ticket.app.service.admin.AdminOrderService;
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
 * 管理员订单控制器
 */
@Slf4j
@Tag(name = "管理后台 - 订单管理", description = "管理员操作订单的接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final JwtUtils jwtUtils;

    /**
     * 获取订单列表（管理员）
     * GET /api/admin/orders
     */
    @GetMapping("/orders")
    @Operation(summary = "获取订单列表", description = "管理员获取订单列表，支持分页、搜索、状态筛选")
    public Result<PageResult<AdminOrderResponse>> getOrderList(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "支付状态") @RequestParam(required = false) Integer payStatus,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/orders - 获取订单列表: adminId={}, page={}, pageSize={}, orderNo={}",
                adminId, page, pageSize, orderNo);

        AdminOrderListRequest queryRequest = new AdminOrderListRequest();
        queryRequest.setPage(page);
        queryRequest.setPageSize(pageSize);
        queryRequest.setOrderNo(orderNo);
        queryRequest.setUserId(userId);
        queryRequest.setStatus(status);
        queryRequest.setPayStatus(payStatus);

        PageResult<AdminOrderResponse> result = adminOrderService.getOrderList(queryRequest);
        return Result.success(result);
    }

    /**
     * 获取订单详情
     * GET /api/admin/orders/{id}
     */
    @GetMapping("/orders/{id}")
    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详情")
    public Result<AdminOrderResponse> getOrderDetail(
            @Parameter(description = "订单ID", required = true) @PathVariable("id") Long orderId,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/orders/{id} - 获取订单详情: adminId={}, orderId={}", adminId, orderId);

        AdminOrderResponse response = adminOrderService.getOrderDetail(orderId);
        return Result.success(response);
    }

    /**
     * 更新订单状态
     * PUT /api/admin/orders/{id}/status
     */
    @PutMapping("/orders/{id}/status")
    @Operation(summary = "更新订单状态", description = "管理员更新订单状态")
    public Result<Void> updateOrderStatus(
            @Parameter(description = "订单ID", required = true) @PathVariable("id") Long orderId,
            @Parameter(description = "订单状态", required = true) @RequestParam Integer status,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("PUT /api/admin/orders/{id}/status - 更新订单状态: adminId={}, orderId={}, status={}",
                adminId, orderId, status);

        // TODO: 实现更新订单状态逻辑
        return Result.success("订单状态更新成功", null);
    }

    /**
     * 管理员取消订单
     * PUT /api/admin/orders/{id}/cancel
     */
    @PutMapping("/orders/{id}/cancel")
    @Operation(summary = "取消订单", description = "管理员取消订单")
    public Result<Void> cancelOrder(
            @Parameter(description = "订单ID", required = true) @PathVariable("id") Long orderId,
            @Valid @RequestBody(required = false) AdminCancelOrderRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/orders/{id}/cancel - 取消订单: adminId={}, orderId={}", adminId, orderId);

        if (request == null) {
            request = new AdminCancelOrderRequest();
        }
        adminOrderService.cancelOrder(orderId, request);
        return Result.success("订单取消成功", null);
    }

    /**
     * 管理员强制退款
     * PUT /api/admin/orders/{id}/refund
     */
    @PutMapping("/orders/{id}/refund")
    @Operation(summary = "强制退款", description = "管理员强制退款")
    public Result<Void> refundOrder(
            @Parameter(description = "订单ID", required = true) @PathVariable("id") Long orderId,
            @Valid @RequestBody AdminRefundRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/orders/{id}/refund - 强制退款: adminId={}, orderId={}, refundAmount={}",
                adminId, orderId, request.getRefundAmount());

        adminOrderService.refundOrder(orderId, request);
        return Result.success("退款成功", null);
    }

    /**
     * 获取订单关联的票务
     * GET /api/admin/orders/{id}/tickets
     */
    @GetMapping("/orders/{id}/tickets")
    @Operation(summary = "获取订单关联的票务", description = "获取订单关联的票务列表")
    public Result<List<?>> getOrderTickets(
            @Parameter(description = "订单ID", required = true) @PathVariable("id") Long orderId,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/orders/{id}/tickets - 获取订单票务: adminId={}, orderId={}", adminId, orderId);

        List<?> tickets = adminOrderService.getOrderTickets(orderId);
        return Result.success(tickets);
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
