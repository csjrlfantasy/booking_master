package com.ticket.app.job;

import com.ticket.app.service.order.OrderExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderExpirationService orderExpirationService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelExpiredPendingOrders() {
        orderExpirationService.cancelExpiredPendingOrders();
    }
}
