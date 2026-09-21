package com.ticket.app.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.OrderItem;
import com.ticket.app.repository.order.OrderItemRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import com.ticket.app.service.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpirationService {

    private final OrderRepository orderRepository;
    private final InventoryCompensationService inventory;
    private final CouponService couponService;

    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderStatus, Order.STATUS_PENDING_PAY)
                .eq(Order::getPayStatus, Order.PAY_STATUS_UNPAID)
                .isNotNull(Order::getExpireTime)
                .le(Order::getExpireTime, now)
                .isNull(Order::getDeletedAt)
                .orderByAsc(Order::getId)
                .last("FOR UPDATE"));

        int cancelled = 0;
        for (Order order : expiredOrders) {
            order.setOrderStatus(Order.STATUS_CANCELLED);
            order.setCancelTime(now);
            order.setCancelReason("支付超时自动取消");
            order.setUpdatedAt(now);
            orderRepository.updateById(order);

            if (order.getUserCouponId() != null) {
                couponService.returnCoupon(order.getUserId(), order.getId());
            }

            inventory.restoreOnce(order.getId());
            cancelled++;
        }

        if (cancelled > 0) {
            log.info("自动取消超时待支付订单: count={}", cancelled);
        }
        return cancelled;
    }

}
