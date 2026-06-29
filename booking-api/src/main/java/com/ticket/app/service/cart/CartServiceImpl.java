package com.ticket.app.service.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.cart.dto.AddCartRequest;
import com.ticket.app.controller.cart.dto.CartItemResponse;
import com.ticket.app.controller.cart.dto.UpdateCartRequest;
import com.ticket.app.entity.Cart;
import com.ticket.app.entity.Ticket;
import com.ticket.app.repository.cart.CartRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addToCart(Long userId, AddCartRequest request) {
        // 检查是否已存在相同的购物车项
        LambdaQueryWrapper<Cart> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getTicketId, request.getTicketId())
                .eq(Cart::getSessionId, request.getSessionId())
                .eq(Cart::getPriceLevelId, request.getPriceLevelId())
                .eq(Cart::getStatus, Cart.STATUS_ACTIVE);
        
        Cart existCart = cartRepository.selectOne(existWrapper);
        
        if (existCart != null) {
            // 存在则累加数量
            existCart.setQuantity(existCart.getQuantity() + request.getQuantity());
            existCart.setSubtotal(existCart.getUnitPrice().multiply(BigDecimal.valueOf(existCart.getQuantity())));
            cartRepository.updateById(existCart);
            log.info("购物车数量累加: userId={}, cartId={}, quantity={}", userId, existCart.getId(), existCart.getQuantity());
            return existCart.getId();
        }
        
        // 不存在则新增
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setTicketId(request.getTicketId());
        cart.setSessionId(request.getSessionId());
        cart.setPriceLevelId(request.getPriceLevelId());
        cart.setPriceLevelName(request.getPriceLevelName());
        cart.setQuantity(request.getQuantity());
        cart.setUnitPrice(request.getUnitPrice());
        cart.setSubtotal(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        cart.setStatus(Cart.STATUS_ACTIVE);
        
        cartRepository.insert(cart);
        log.info("添加购物车成功: userId={}, cartId={}", userId, cart.getId());
        
        return cart.getId();
    }

    @Override
    public List<CartItemResponse> getCartList(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getStatus, Cart.STATUS_ACTIVE)
                .orderByDesc(Cart::getCreatedAt);
        
        List<Cart> cartList = cartRepository.selectList(wrapper);
        
        if (cartList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取票务信息
        Set<Long> ticketIds = cartList.stream()
                .map(Cart::getTicketId)
                .collect(Collectors.toSet());
        Map<Long, Ticket> ticketMap = ticketRepository.selectBatchIds(ticketIds).stream()
                .collect(Collectors.toMap(Ticket::getId, t -> t));
        
        return cartList.stream().map(cart -> {
            CartItemResponse response = new CartItemResponse();
            response.setId(cart.getId());
            response.setUserId(cart.getUserId());
            response.setTicketId(cart.getTicketId());
            response.setSessionId(cart.getSessionId());
            response.setPriceLevelId(cart.getPriceLevelId());
            response.setPriceLevelName(cart.getPriceLevelName());
            response.setQuantity(cart.getQuantity());
            response.setUnitPrice(cart.getUnitPrice());
            response.setSubtotal(cart.getSubtotal());
            response.setStatus(cart.getStatus());
            response.setCreatedAt(cart.getCreatedAt());
            response.setUpdatedAt(cart.getUpdatedAt());
            
            // 填充票务信息
            Ticket ticket = ticketMap.get(cart.getTicketId());
            if (ticket != null) {
                response.setTicketName(ticket.getName());
                response.setTicketPoster(ticket.getPoster());
            }
            
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    public Integer getCartCount(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getStatus, Cart.STATUS_ACTIVE);
        
        Long count = cartRepository.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCartQuantity(Long userId, UpdateCartRequest request) {
        Cart cart = cartRepository.selectById(request.getCartId());
        if (cart == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "购物车项不存在");
        }
        
        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权操作此购物车项");
        }
        
        if (!Cart.STATUS_ACTIVE.equals(cart.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "购物车项已失效");
        }
        
        cart.setQuantity(request.getQuantity());
        cart.setSubtotal(cart.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        cartRepository.updateById(cart);
        
        log.info("更新购物车数量: userId={}, cartId={}, quantity={}", userId, request.getCartId(), request.getQuantity());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCartItem(Long userId, Long cartId) {
        Cart cart = cartRepository.selectById(cartId);
        if (cart == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "购物车项不存在");
        }
        
        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权操作此购物车项");
        }
        
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getId, cartId)
                .set(Cart::getStatus, Cart.STATUS_INACTIVE);
        cartRepository.update(wrapper);
        
        log.info("删除购物车项: userId={}, cartId={}", userId, cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteCartItems(Long userId, List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return;
        }
        
        // 检查是否属于该用户
        LambdaQueryWrapper<Cart> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Cart::getUserId, userId)
                .in(Cart::getId, cartIds);
        Long count = cartRepository.selectCount(checkWrapper);
        
        if (count == null || count != cartIds.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "存在无权操作的购物车项");
        }
        
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Cart::getId, cartIds)
                .set(Cart::getStatus, Cart.STATUS_INACTIVE);
        cartRepository.update(wrapper);
        
        log.info("批量删除购物车项: userId={}, count={}", userId, cartIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getStatus, Cart.STATUS_ACTIVE)
                .set(Cart::getStatus, Cart.STATUS_INACTIVE);
        cartRepository.update(wrapper);
        
        log.info("清空购物车: userId={}", userId);
    }
}
