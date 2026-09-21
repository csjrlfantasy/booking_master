package com.ticket.app.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

/** Idempotent inventory release backed by the order-row fact, not order status. */
@Service
@RequiredArgsConstructor
public class InventoryCompensationService {
    private final JdbcTemplate jdbc;

    @Transactional(rollbackFor = Exception.class)
    public boolean restoreOnce(Long orderId) {
        var rows = jdbc.queryForList("SELECT ticket_id, session_id, inventory_restored FROM `order` WHERE id = ? FOR UPDATE", orderId);
        if (rows.isEmpty()) return false;
        var order = rows.get(0);
        Object fact = order.get("inventory_restored");
        if (fact == null) throw new IllegalStateException("Order " + orderId + " has unknown legacy inventory_restored; reconcile inventory before retrying");
        if (Boolean.TRUE.equals(fact) || (fact instanceof Number number && number.intValue() == 1)) return false;
        if (!Boolean.FALSE.equals(fact) && !(fact instanceof Number number && number.intValue() == 0)) {
            throw new IllegalStateException("Invalid inventory_restored fact for order " + orderId);
        }
        Map<Long, Integer> quantities = new TreeMap<>();
        int total = 0;
        for (var item : jdbc.queryForList("SELECT price_level_id, quantity FROM order_item WHERE order_id = ? ORDER BY id FOR UPDATE", orderId)) {
            if (!(item.get("quantity") instanceof Number quantity) || quantity.intValue() <= 0
                    || !(item.get("price_level_id") instanceof Number level)) {
                throw new IllegalStateException("Invalid inventory item for order " + orderId);
            }
            quantities.merge(level.longValue(), quantity.intValue(), Math::addExact);
            total = Math.addExact(total, quantity.intValue());
        }
        if (total == 0 || !(order.get("ticket_id") instanceof Number ticketId)) {
            throw new IllegalStateException("Missing inventory items or ticket for order " + orderId);
        }
        // Stable resource order, matching reservation: price levels, ticket, session.
        for (var entry : quantities.entrySet()) require(jdbc.update(
                "UPDATE ticket_price_level SET available_stock=available_stock+?, sold_count=sold_count-? "
                        + "WHERE id=? AND available_stock>=0 AND available_stock+?<=total_stock AND sold_count>=? AND sold_count<=total_stock",
                entry.getValue(), entry.getValue(), entry.getKey(), entry.getValue(), entry.getValue()));
        require(jdbc.update("UPDATE ticket SET available_stock=available_stock+?, sale_count=sale_count-? "
                        + "WHERE id=? AND available_stock>=0 AND available_stock+?<=total_stock AND sale_count>=? AND sale_count<=total_stock",
                total, total, ticketId.longValue(), total, total));
        if (order.get("session_id") != null) {
            Long sessionId = ((Number) order.get("session_id")).longValue();
            require(jdbc.update("UPDATE ticket_session SET available_capacity=available_capacity+? WHERE id=? AND available_capacity>=0 AND available_capacity+?<=total_capacity", total, sessionId, total));
        }
        require(jdbc.update("UPDATE `order` SET inventory_restored=1 WHERE id=? AND inventory_restored=0", orderId));
        return true;
    }
    private static void require(int rows) { if (rows != 1) throw new IllegalStateException("inventory compensation invariant failed"); }
}
