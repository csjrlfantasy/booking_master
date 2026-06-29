package com.ticket.app.controller.payment;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.payment.dto.CreatePaymentRequest;
import com.ticket.app.controller.payment.dto.PaymentResultResponse;
import com.ticket.app.controller.payment.dto.RefundRequest;
import com.ticket.app.controller.payment.dto.RefundResultResponse;
import com.ticket.app.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Payment", description = "Payment and refund APIs")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtUtils jwtUtils;

    @PostMapping("/create")
    @Operation(summary = "Create payment", description = "Create a mock payment request for an order")
    public Result<PaymentResultResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/payment/create - create payment: userId={}, orderNo={}, payChannel={}",
                userId, request.getOrderNo(), request.getPayChannel());

        if (request.getClientIp() == null || request.getClientIp().isEmpty()) {
            request.setClientIp(getClientIp(httpRequest));
        }

        PaymentResultResponse response = paymentService.createPayment(userId, request);
        return Result.success("Payment created", response);
    }

    @PostMapping("/mock-pay")
    @Operation(summary = "Mock payment", description = "Demo endpoint for mock WeChat or Alipay payment success")
    public Result<PaymentResultResponse> mockPay(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/payment/mock-pay - mock payment: userId={}, orderNo={}, payChannel={}",
                userId, request.getOrderNo(), request.getPayChannel());

        if (request.getClientIp() == null || request.getClientIp().isEmpty()) {
            request.setClientIp(getClientIp(httpRequest));
        }

        PaymentResultResponse response = paymentService.mockPay(userId, request);
        return Result.success("Mock payment success", response);
    }

    @GetMapping("/status/{orderNo}")
    @Operation(summary = "Get payment status", description = "Get payment status by order number")
    public Result<PaymentResultResponse> getPaymentStatus(
            @Parameter(description = "Order number", required = true) @PathVariable("orderNo") String orderNo,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/payment/status/{} - userId={}", orderNo, userId);

        PaymentResultResponse response = paymentService.getPaymentStatus(userId, orderNo);
        return Result.success(response);
    }

    @PostMapping("/refund")
    @Operation(summary = "Apply refund", description = "Apply refund for a paid order")
    public Result<RefundResultResponse> applyRefund(
            @Valid @RequestBody RefundRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/payment/refund - userId={}, orderNo={}", userId, request.getOrderNo());

        RefundResultResponse response = paymentService.applyRefund(userId, request);
        return Result.success("Refund submitted", response);
    }

    @GetMapping("/refund/{orderNo}")
    @Operation(summary = "Get refund status", description = "Get refund status by order number")
    public Result<RefundResultResponse> getRefundStatus(
            @Parameter(description = "Order number", required = true) @PathVariable("orderNo") String orderNo,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/payment/refund/{} - userId={}", orderNo, userId);

        RefundResultResponse response = paymentService.getRefundStatus(userId, orderNo);
        return Result.success(response);
    }

    @DeleteMapping("/{orderNo}")
    @Operation(summary = "Cancel payment", description = "Close a pending payment")
    public Result<Void> cancelPayment(
            @Parameter(description = "Order number", required = true) @PathVariable("orderNo") String orderNo,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("DELETE /api/payment/{} - userId={}", orderNo, userId);

        paymentService.cancelPayment(userId, orderNo);
        return Result.success("Payment cancelled", null);
    }

    @PostMapping("/callback")
    @Operation(summary = "Payment callback", description = "Payment provider callback with signature verification")
    public Result<Void> payCallback(
            @Parameter(description = "Order number", required = true) @RequestParam String orderNo,
            @Parameter(description = "Payment amount", required = true) @RequestParam String amount,
            @Parameter(description = "Timestamp", required = true) @RequestParam String timestamp,
            @Parameter(description = "Signature", required = true) @RequestParam String sign,
            @Parameter(description = "Payment channel", required = true) @RequestParam String payChannel,
            @Parameter(description = "Third-party trade number") @RequestParam(required = false) String tradeNo) {
        if (!paymentService.verifyCallbackSign(orderNo, amount, timestamp, sign)) {
            log.warn("Payment callback signature verification failed: orderNo={}, amount={}, timestamp={}",
                    orderNo, amount, timestamp);
            throw new BusinessException(ResultCode.PAYMENT_NOTIFY_SIGN_ERROR, "Invalid signature");
        }

        log.info("Payment callback verified: orderNo={}, payChannel={}, tradeNo={}",
                orderNo, payChannel, tradeNo);

        paymentService.handlePayCallback(orderNo, payChannel, tradeNo);
        return Result.success("Processed", null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Please login first");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Invalid token");
        }

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new BusinessException(ResultCode.TOKEN_INVALID, "Token parse failed");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Token parse exception: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Invalid token");
        }
    }
}
