package com.ticket.app.controller.coupon;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.coupon.dto.CouponCenterResponse;
import com.ticket.app.controller.coupon.dto.UserCouponResponse;
import com.ticket.app.controller.coupon.dto.UseCouponRequest;
import com.ticket.app.controller.coupon.dto.ValidateCouponRequest;
import com.ticket.app.controller.coupon.dto.ValidateCouponResponse;
import com.ticket.app.service.coupon.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Tag(name = "Coupon", description = "Coupon APIs")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final JwtUtils jwtUtils;

    @GetMapping
    @Operation(summary = "Get available coupons", description = "List coupons available for the current order")
    public Result<List<UserCouponResponse>> getAvailableCoupons(
            @Parameter(description = "Order amount", required = false) @RequestParam(required = false) BigDecimal orderAmount,
            @Parameter(description = "Ticket ID") @RequestParam(required = false) Long ticketId,
            @Parameter(description = "Ticket category ID") @RequestParam(required = false) Integer categoryId,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/coupons - list available coupons: userId={}, orderAmount={}, ticketId={}, categoryId={}",
                userId, orderAmount, ticketId, categoryId);
        if (orderAmount == null) {
            orderAmount = BigDecimal.ZERO;
        }
        List<UserCouponResponse> coupons = couponService.getAvailableCoupons(userId, orderAmount, ticketId, categoryId);
        return Result.success(coupons);
    }

    @GetMapping("/center")
    @Operation(summary = "Get coupon center", description = "List claimable coupon templates for the current user")
    public Result<PageResult<CouponCenterResponse>> getCouponCenter(
            @Parameter(description = "Page", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/coupons/center - list coupon center: userId={}, page={}, pageSize={}", userId, page, pageSize);
        return Result.success(couponService.getCouponCenter(userId, page, pageSize));
    }

    @GetMapping("/list")
    @Operation(summary = "Get user coupons", description = "List the current user's coupons")
    public Result<PageResult<UserCouponResponse>> getUserCouponList(
            @Parameter(description = "Coupon status: 0-unused, 1-used, 2-expired, 3-disabled") @RequestParam(required = false) Integer status,
            @Parameter(description = "Page", example = "1") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Page size", example = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/coupons/list - list user coupons: userId={}, status={}, page={}, pageSize={}",
                userId, status, page, pageSize);
        PageResult<UserCouponResponse> result = couponService.getUserCouponList(userId, status, page, pageSize);
        return Result.success(result);
    }

    @PostMapping("/receive")
    @Operation(summary = "Receive coupon", description = "Claim a coupon template")
    public Result<Long> receiveCoupon(
            @Parameter(description = "Coupon template ID", required = true) @RequestParam Long couponId,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/coupons/receive - receive coupon: userId={}, couponId={}", userId, couponId);
        Long userCouponId = couponService.receiveCoupon(userId, couponId);
        return Result.success("success", userCouponId);
    }

    @PostMapping("/use")
    @Operation(summary = "Use coupon", description = "Mark a user coupon as used")
    public Result<Void> useCoupon(
            @Valid @RequestBody UseCouponRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/coupons/use - use coupon: userId={}, userCouponId={}, orderId={}",
                userId, request.getUserCouponId(), request.getOrderId());
        couponService.useCoupon(userId, request.getUserCouponId(), request.getOrderId());
        return Result.success("success", null);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate coupon", description = "Validate a claimed user coupon for the current order")
    public Result<ValidateCouponResponse> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/coupons/validate - validate coupon: userId={}, userCouponId={}, orderAmount={}",
                userId, request.getUserCouponId(), request.getOrderAmount());
        ValidateCouponResponse response = couponService.validateUserCoupon(
                userId,
                request.getUserCouponId(),
                request.getOrderAmount(),
                request.getTicketId(),
                request.getCategoryId()
        );
        return Result.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon detail", description = "Get detail for a claimed user coupon")
    public Result<UserCouponResponse> getUserCouponDetail(
            @Parameter(description = "User coupon ID", required = true) @PathVariable("id") Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/coupons/{id} - get coupon detail: userId={}, userCouponId={}", userId, id);
        UserCouponResponse response = couponService.getUserCouponDetail(userId, id);
        return Result.success(response);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "login required");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "invalid token");
        }
        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new BusinessException(ResultCode.TOKEN_INVALID, "token parse failed");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("token parse error: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID, "invalid token");
        }
    }
}