package com.ticket.app.controller.coupon;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券控制器
 */
@Slf4j
@Tag(name = "优惠券管理", description = "优惠券相关接口")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final JwtUtils jwtUtils;

    /**
     * 获取可用优惠券列表
     * GET /api/coupons
     */
    @GetMapping
    @Operation(summary = "获取可用优惠券", description = "获取当前订单可用的优惠券列表")
    public Result<List<UserCouponResponse>> getAvailableCoupons(
            @Parameter(description = "订单总金额", required = false) @RequestParam(required = false) BigDecimal orderAmount,
            @Parameter(description = "票务ID") @RequestParam(required = false) Long ticketId,
            @Parameter(description = "票务分类ID") @RequestParam(required = false) Integer categoryId,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/coupons - 获取可用优惠券: userId={}, orderAmount={}, ticketId={}, categoryId={}",
                userId, orderAmount, ticketId, categoryId);
        // 如果订单金额为空，设置默认值
        if (orderAmount == null) {
            orderAmount = BigDecimal.ZERO;
        }
        List<UserCouponResponse> coupons = couponService.getAvailableCoupons(userId, orderAmount, ticketId, categoryId);
        return Result.success(coupons);
    }

    /**
     * 获取用户优惠券列表
     * GET /api/coupons/list
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户优惠券列表", description = "获取用户的优惠券列表，支持状态筛选")
    public Result<PageResult<UserCouponResponse>> getUserCouponList(
            @Parameter(description = "优惠券状态: 0-未使用, 1-已使用, 2-已过期, 3-已失效")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码", example = "1") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/coupons/list - 获取优惠券列表: userId={}, status={}, page={}, pageSize={}",
                userId, status, page, pageSize);
        PageResult<UserCouponResponse> result = couponService.getUserCouponList(userId, status, page, pageSize);
        return Result.success(result);
    }

    /**
     * 领取优惠券
     * POST /api/coupons/receive
     */
    @PostMapping("/receive")
    @Operation(summary = "领取优惠券", description = "用户领取指定优惠券")
    public Result<Long> receiveCoupon(
            @Parameter(description = "优惠券ID", required = true) @RequestParam Long couponId,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/coupons/receive - 领取优惠券: userId={}, couponId={}", userId, couponId);
        Long userCouponId = couponService.receiveCoupon(userId, couponId);
        return Result.success("领取成功", userCouponId);
    }

    /**
     * 使用优惠券
     * POST /api/coupons/use
     */
    @PostMapping("/use")
    @Operation(summary = "使用优惠券", description = "在订单中使用优惠券")
    public Result<Void> useCoupon(
            @Valid @RequestBody UseCouponRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/coupons/use - 使用优惠券: userId={}, userCouponId={}, orderId={}",
                userId, request.getUserCouponId(), request.getOrderId());
        couponService.useCoupon(userId, request.getUserCouponId(), request.getOrderId());
        return Result.success("优惠券使用成功", null);
    }

    /**
     * 校验优惠券
     * POST /api/coupons/validate
     */
    @PostMapping("/validate")
    @Operation(summary = "校验优惠券", description = "校验优惠券是否可用，返回优惠金额")
    public Result<ValidateCouponResponse> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/coupons/validate - 校验优惠券: userId={}, couponCode={}, orderAmount={}",
                userId, request.getCouponCode(), request.getOrderAmount());
        ValidateCouponResponse response = couponService.validateCoupon(userId, request);
        return Result.success(response);
    }

    /**
     * 获取优惠券详情
     * GET /api/coupons/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取优惠券详情", description = "获取用户优惠券的详细信息")
    public Result<UserCouponResponse> getUserCouponDetail(
            @Parameter(description = "用户优惠券ID", required = true) @PathVariable("id") Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/coupons/{id} - 获取优惠券详情: userId={}, userCouponId={}", userId, id);
        UserCouponResponse response = couponService.getUserCouponDetail(userId, id);
        return Result.success(response);
    }

    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
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
