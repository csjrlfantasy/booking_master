package com.ticket.app.controller.order;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.common.util.PaymentSignatureUtils;
import com.ticket.app.controller.order.dto.*;
import com.ticket.app.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Slf4j
@Tag(name = "订单管理", description = "订单相关接口")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtUtils jwtUtils;

    /**
     * 创建订单
     * POST /api/orders
     */
    @PostMapping
    @Operation(summary = "创建订单", description = "创建新的订单")
    public Result<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/orders - 创建订单: userId={}, ticketId={}, sessionId={}, itemCount={}",
                userId, request.getTicketId(), request.getSessionId(),
                request.getItems() != null ? request.getItems().size() : 0);
        CreateOrderResponse response = orderService.createOrder(userId, request);
        return Result.success("订单创建成功", response);
    }

    /**
     * 获取订单列表
     * GET /api/orders
     */
    @GetMapping
    @Operation(summary = "获取订单列表", description = "获取当前用户的订单列表，支持分页和状态筛选")
    public Result<PageResult<OrderListItemResponse>> getOrderList(
            @Parameter(description = "订单状态: 1-待支付, 2-已支付, 3-已完成, 4-已取消, 5-已退款, 6-退款中")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码", example = "1") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/orders - 获取订单列表: userId={}, status={}, page={}, pageSize={}",
                userId, status, page, pageSize);
        PageResult<OrderListItemResponse> result = orderService.getOrderList(userId, status, page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取订单详情
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取订单详情", description = "根据订单ID或订单号获取订单详情")
    public Result<OrderDetailResponse> getOrderDetail(
            @Parameter(description = "订单ID或订单号", required = true) @PathVariable("id") String id,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/orders/{id} - 获取订单详情: userId={}, id={}", userId, id);

        OrderDetailResponse response;
        try {
            // 尝试作为数字ID解析
            Long orderId = Long.parseLong(id);
            response = orderService.getOrderDetail(userId, orderId);
        } catch (NumberFormatException e) {
            // 作为订单号解析
            response = orderService.getOrderDetailByNo(userId, id);
        }
        return Result.success(response);
    }

    /**
     * 更新订单状态
     * PUT /api/orders/{id}/status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新订单状态", description = "更新订单状态")
    public Result<Void> updateOrderStatus(
            @Parameter(description = "订单ID", required = true) @PathVariable("id") Long orderId,
            @Parameter(description = "订单状态", required = true) @RequestParam Integer status,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("PUT /api/orders/{id}/status - 更新订单状态: userId={}, orderId={}, status={}", userId, orderId, status);
        orderService.updateOrderStatus(userId, orderId, status);
        return Result.success("订单状态更新成功", null);
    }

    /**
     * 取消订单
     * PUT /api/orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "取消待支付订单")
    public Result<Void> cancelOrder(
            @Parameter(description = "订单ID") @PathVariable("id") Long orderId,
            @RequestBody(required = false) CancelOrderRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("PUT /api/orders/{id}/cancel - 取消订单: userId={}, orderId={}", userId, orderId);

        if (request == null) {
            request = new CancelOrderRequest();
        }
        request.setOrderId(orderId);
        orderService.cancelOrder(userId, request);
        return Result.success("订单取消成功", null);
    }

    /**
     * 确认完成订单
     * PUT /api/orders/{id}/complete
     */
    @PutMapping("/{id}/complete")
    @Operation(summary = "确认完成订单", description = "确认收货/完成订单")
    public Result<Void> completeOrder(
            @Parameter(description = "订单ID", required = true) @PathVariable("id") Long orderId,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("PUT /api/orders/{id}/complete - 确认完成订单: userId={}, orderId={}", userId, orderId);
        orderService.completeOrder(userId, orderId);
        return Result.success("订单已完成", null);
    }

    /**
     * 支付成功回调(供支付系统调用)
     * 添加签名验证，防止伪造支付回调
     */
    @PostMapping("/pay/callback")
    @Operation(summary = "支付成功回调", description = "支付系统回调接口")
    public Result<Void> payCallback(
            @Parameter(description = "订单号", required = true) @RequestParam String orderNo,
            @Parameter(description = "支付金额", required = true) @RequestParam String amount,
            @Parameter(description = "时间戳", required = true) @RequestParam String timestamp,
            @Parameter(description = "签名", required = true) @RequestParam String sign,
            @Parameter(description = "支付渠道", required = true) @RequestParam String payChannel,
            @Parameter(description = "第三方交易号") @RequestParam(required = false) String tradeNo) {
        
        // 验证签名
        if (!PaymentSignatureUtils.verifyPaymentCallback(orderNo, amount, timestamp, sign)) {
            log.warn("支付回调签名验证失败: orderNo={}, amount={}, timestamp={}", orderNo, amount, timestamp);
            throw new BusinessException(ResultCode.PARAM_ERROR, "签名验证失败");
        }
        
        log.info("支付回调签名验证通过: orderNo={}, payChannel={}, tradeNo={}", orderNo, payChannel, tradeNo);
        orderService.paySuccess(orderNo, payChannel, tradeNo);
        return Result.success("处理成功", null);
    }

    /**
     * 从请求中获取用户ID（需要登录的接口使用）
     * 如果Token无效，抛出异常而不是返回默认用户ID（安全修复）
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        
        // 检查是否提供了Token
        if (token == null || token.isEmpty()) {
            log.warn("未提供Authentication头");
            throw new com.ticket.app.common.exception.BusinessException(
                com.ticket.app.common.ResultCode.TOKEN_INVALID, "请先登录");
        }
        
        // 移除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证Token格式
        if (token.isEmpty()) {
            log.warn("Token为空");
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
            // 直接重新抛出业务异常
            log.warn("Token验证失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Token解析异常: {}", e.getMessage());
            throw new com.ticket.app.common.exception.BusinessException(
                com.ticket.app.common.ResultCode.TOKEN_INVALID, "Token无效");
        }
    }
}
