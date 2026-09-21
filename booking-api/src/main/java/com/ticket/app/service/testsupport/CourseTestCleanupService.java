package com.ticket.app.service.testsupport;

import com.ticket.app.controller.testsupport.dto.CourseCleanupRequest;
import com.ticket.app.controller.testsupport.dto.CourseCleanupResponse;
import com.ticket.app.service.order.InventoryCompensationService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.TreeMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.List;

@Profile("dev")
@Service
@ConditionalOnProperty(prefix = "course.test-support", name = "enabled", havingValue = "true")
public class CourseTestCleanupService {
    private static final Pattern RUN_ID = Pattern.compile("[a-z0-9]{8,32}");
    private final JdbcTemplate jdbcTemplate;
    private final InventoryCompensationService inventory;

    @Autowired
    public CourseTestCleanupService(JdbcTemplate jdbcTemplate, InventoryCompensationService inventory) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventory = inventory;
    }

    public CourseTestCleanupService(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new InventoryCompensationService(jdbcTemplate));
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseCleanupResponse cleanupUser(String runId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        return cleanup(new CourseCleanupRequest(
                runId, List.of(userId), List.of(), List.of(), List.of()));
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseCleanupResponse cleanup(CourseCleanupRequest request) {
        if (request == null || request.runId() == null
                || !RUN_ID.matcher(request.runId()).matches()) {
            throw new IllegalArgumentException("runId must match [a-z0-9]{8,32}");
        }
        Map<String, Integer> deleted = new LinkedHashMap<>();
        Map<String, Integer> notFound = new LinkedHashMap<>();
        Map<String, Integer> rejected = new LinkedHashMap<>();
        // Lock ALL validated users first; standalone order cleanup never takes a user lock.
        List<Long> users = new ArrayList<>();
        distinct(request.userIds()).forEach(id -> validate("user", id,
                "SELECT username FROM user WHERE id = ?", request.runId(),
                deleted, notFound, rejected, () -> users.add(id)));
        TreeSet<Long> orders = new TreeSet<>(distinct(request.orderIds()));
        users.forEach(id -> orders.addAll(runOwnedOrderIds(id, request.runId())));
        // Acquire every order lock before touching shared inventory. Interleaving
        // restoration with locking could invert order/price-level locks against a
        // standalone cleanup of a later order sharing the same stock.
        List<Long> authorizedOrders = new ArrayList<>();
        orders.forEach(id -> validate("order", id,
                "SELECT user_remark FROM `order` WHERE id = ?", request.runId(),
                deleted, notFound, rejected, () -> authorizedOrders.add(id)));
        authorizedOrders.forEach(id -> deleteOrder(id, deleted));
        users.forEach(id -> deleteUser(id, deleted));
        distinct(request.ticketIds()).forEach(id -> validate("ticket", id,
                "SELECT name FROM ticket WHERE id = ?", request.runId(),
                deleted, notFound, rejected, () -> deleteTicket(id, deleted)));
        distinct(request.couponIds()).forEach(id -> validate("coupon", id,
                "SELECT name FROM coupon WHERE id = ?", request.runId(),
                deleted, notFound, rejected, () -> deleteCoupon(id, deleted)));
        return new CourseCleanupResponse(request.runId(), immutable(deleted),
                immutable(notFound), immutable(rejected));
    }

    private void validate(String kind, Long id, String query, String runId,
                          Map<String, Integer> d, Map<String, Integer> n,
                          Map<String, Integer> r, Runnable delete) {
        String owner;
        try {
            owner = jdbcTemplate.queryForObject(query + " FOR UPDATE", (rs,row) -> rs.getString(1), id);
        } catch (EmptyResultDataAccessException missing) {
            owner = null;
        }
        if (owner == null) {
            n.merge(kind, 1, Integer::sum);
            return;
        }
        if (!owner.startsWith("pw-course-" + runId + "-")) {
            r.merge(kind, 1, Integer::sum);
            return;
        }
        delete.run();
    }

    private static List<Long> distinct(List<Long> ids) {
        return ids.stream().distinct().sorted().toList();
    }

    private void deleteUser(Long id, Map<String,Integer> d) {
        restoreCouponStock(id);
        delete("user_coupon", "DELETE FROM user_coupon WHERE user_id = ?", id, d);
        delete("user_identity", "DELETE FROM user_identity WHERE user_id = ?", id, d);
        delete("user_auth", "DELETE FROM user_auth WHERE user_id = ?", id, d);
        delete("user_address", "DELETE FROM user_address WHERE user_id = ?", id, d);
        delete("cart", "DELETE FROM cart WHERE user_id = ?", id, d);
        delete("user_role_relation", "DELETE FROM user_role_relation WHERE user_id = ?", id, d);
        delete("user", "DELETE FROM user WHERE id = ?", id, d);
    }

    private void deleteOrder(Long id, Map<String,Integer> d) {
        inventory.restoreOnce(id);
        delete("order_item", "DELETE FROM order_item WHERE order_id = ?", id, d);
        delete("refund", "DELETE FROM refund WHERE order_id = ?", id, d);
        delete("payment", "DELETE FROM payment WHERE order_id = ?", id, d);
        delete("order", "DELETE FROM `order` WHERE id = ?", id, d);
    }

    private List<Long> runOwnedOrderIds(Long userId, String runId) {
        String prefix = "pw-course-" + runId + "-%";
        return jdbcTemplate.queryForList(
                        "SELECT id FROM `order` WHERE user_id = ? AND user_remark LIKE ? ORDER BY id", userId, prefix)
                .stream()
                .map(row -> longValue(row.get("id")))
                .toList();
    }

    private void restoreCouponStock(Long userId) {
        Map<Long, Integer> manualOrAdminCounts = new TreeMap<>();
        for (Map<String, Object> userCoupon : jdbcTemplate.queryForList(
                "SELECT coupon_id, source FROM user_coupon WHERE user_id = ? ORDER BY id FOR UPDATE", userId)) {
            Long couponId = longValue(userCoupon.get("coupon_id"));
            if (couponId != null) {
                manualOrAdminCounts.merge(couponId, 1, Integer::sum);
            }
        }
        manualOrAdminCounts.forEach((couponId, count) -> requireSingleUpdate(
                "UPDATE coupon SET remain_count = remain_count + ? WHERE id = ? AND remain_count >= 0 AND remain_count + ? <= total_count",
                count, couponId, count));
    }

    private void requireSingleUpdate(String sql, Object... args) {
        if (jdbcTemplate.update(sql, args) != 1) {
            throw new IllegalStateException("course cleanup would violate an inventory invariant");
        }
    }

    private static Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private void deleteTicket(Long id, Map<String,Integer> d) {
        delete("ticket_comment", "DELETE FROM ticket_comment WHERE ticket_id = ?", id, d);
        delete("ticket_reminder", "DELETE FROM ticket_reminder WHERE ticket_id = ?", id, d);
        delete("ticket_flash_sale", "DELETE FROM ticket_flash_sale WHERE ticket_id = ?", id, d);
        delete("inventory_log", "DELETE FROM inventory_log WHERE ticket_id = ?", id, d);
        delete("ticket_price_level", "DELETE FROM ticket_price_level WHERE ticket_id = ?", id, d);
        delete("ticket_session", "DELETE FROM ticket_session WHERE ticket_id = ?", id, d);
        delete("ticket", "DELETE FROM ticket WHERE id = ?", id, d);
    }
    private void deleteCoupon(Long id, Map<String,Integer> d) {
        delete("user_coupon", "DELETE FROM user_coupon WHERE coupon_id = ?", id, d);
        delete("coupon", "DELETE FROM coupon WHERE id = ?", id, d);
    }
    private void delete(String table, String sql, Long id, Map<String,Integer> d) {
        d.merge(table, jdbcTemplate.update(sql, id), Integer::sum);
    }
    private static Map<String,Integer> immutable(Map<String,Integer> m) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(m));
    }
}
