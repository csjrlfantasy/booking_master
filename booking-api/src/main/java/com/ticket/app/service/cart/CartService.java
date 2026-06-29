package com.ticket.app.service.cart;

import com.ticket.app.controller.cart.dto.AddCartRequest;
import com.ticket.app.controller.cart.dto.CartItemResponse;
import com.ticket.app.controller.cart.dto.UpdateCartRequest;
import com.ticket.app.common.PageResult;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService {

    /**
     * 添加商品到购物车
     *
     * @param userId 用户ID
     * @param request 添加请求
     * @return 购物车项ID
     */
    Long addToCart(Long userId, AddCartRequest request);

    /**
     * 获取购物车列表
     *
     * @param userId 用户ID
     * @return 购物车列表
     */
    List<CartItemResponse> getCartList(Long userId);

    /**
     * 获取购物车数量
     *
     * @param userId 用户ID
     * @return 购物车商品数量
     */
    Integer getCartCount(Long userId);

    /**
     * 更新购物车商品数量
     *
     * @param userId 用户ID
     * @param request 更新请求
     */
    void updateCartQuantity(Long userId, UpdateCartRequest request);

    /**
     * 删除购物车商品
     *
     * @param userId 用户ID
     * @param cartId 购物车ID
     */
    void deleteCartItem(Long userId, Long cartId);

    /**
     * 批量删除购物车商品
     *
     * @param userId 用户ID
     * @param cartIds 购物车ID列表
     */
    void batchDeleteCartItems(Long userId, List<Long> cartIds);

    /**
     * 清空购物车
     *
     * @param userId 用户ID
     */
    void clearCart(Long userId);
}
