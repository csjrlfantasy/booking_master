package com.ticket.app.controller.cart;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.cart.dto.AddCartRequest;
import com.ticket.app.controller.cart.dto.CartItemResponse;
import com.ticket.app.controller.cart.dto.UpdateCartRequest;
import com.ticket.app.service.cart.CartService;
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
 * 购物车控制器
 */
@Slf4j
@Tag(name = "购物车管理", description = "购物车相关接口")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtUtils jwtUtils;

    /**
     * 添加购物车
     * POST /api/cart/add
     */
    @PostMapping("/add")
    @Operation(summary = "添加购物车", description = "将票品添加到购物车")
    public Result<Long> addToCart(
            @Valid @RequestBody AddCartRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("POST /api/cart/add - 添加购物车: userId={}, ticketId={}, sessionId={}, quantity={}",
                userId, request.getTicketId(), request.getSessionId(), request.getQuantity());
        Long cartItemId = cartService.addToCart(userId, request);
        return Result.success("添加成功", cartItemId);
    }

    /**
     * 获取购物车列表
     * GET /api/cart
     */
    @GetMapping
    @Operation(summary = "获取购物车列表", description = "获取当前用户的购物车列表")
    public Result<List<CartItemResponse>> getCartList(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("GET /api/cart - 获取购物车列表: userId={}", userId);
        List<CartItemResponse> cartList = cartService.getCartList(userId);
        return Result.success(cartList);
    }

    /**
     * 获取购物车数量
     * GET /api/cart/count
     */
    @GetMapping("/count")
    @Operation(summary = "获取购物车数量", description = "获取当前用户购物车商品数量")
    public Result<Integer> getCartCount(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        Integer count = cartService.getCartCount(userId);
        return Result.success(count);
    }

    /**
     * 更新购物车商品数量
     * PUT /api/cart/quantity
     */
    @PutMapping("/quantity")
    @Operation(summary = "更新购物车数量", description = "更新购物车商品数量")
    public Result<Void> updateCartQuantity(
            @Valid @RequestBody UpdateCartRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("PUT /api/cart/quantity - 更新购物车数量: userId={}, cartId={}, quantity={}",
                userId, request.getCartId(), request.getQuantity());
        cartService.updateCartQuantity(userId, request);
        return Result.success("更新成功", null);
    }

    /**
     * 删除购物车项
     * DELETE /api/cart/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除购物车项", description = "删除购物车中的单个商品")
    public Result<Void> deleteCartItem(
            @Parameter(description = "购物车ID", required = true) @PathVariable("id") Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("DELETE /api/cart/{id} - 删除购物车项: userId={}, cartId={}", userId, id);
        cartService.deleteCartItem(userId, id);
        return Result.success("删除成功", null);
    }

    /**
     * 批量删除购物车项
     * DELETE /api/cart/batch
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除购物车项", description = "批量删除购物车中的商品")
    public Result<Void> batchDeleteCartItems(
            @Parameter(description = "购物车ID列表", required = true) @RequestBody List<Long> cartIds,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("DELETE /api/cart/batch - 批量删除购物车项: userId={}, count={}", userId, cartIds.size());
        cartService.batchDeleteCartItems(userId, cartIds);
        return Result.success("删除成功", null);
    }

    /**
     * 清空购物车
     * DELETE /api/cart
     */
    @DeleteMapping
    @Operation(summary = "清空购物车", description = "清空当前用户的所有购物车商品")
    public Result<Void> clearCart(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        log.info("DELETE /api/cart - 清空购物车: userId={}", userId);
        cartService.clearCart(userId);
        return Result.success("清空成功", null);
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
