package com.ticket.app.service.testsupport;

import com.ticket.app.controller.testsupport.dto.CourseCleanupResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.ticket.app.controller.testsupport.dto.CourseCleanupRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseTestCleanupServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Test
    void rejectsUnsafeRunIdBeforeSql() {
        CourseTestCleanupService service = new CourseTestCleanupService(jdbcTemplate);

        CourseCleanupRequest request = new CourseCleanupRequest("../unsafe", List.of(), List.of(), List.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> service.cleanup(request));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void usesRunScopedPrefixAndIsIdempotent() {
        CourseTestCleanupService service = new CourseTestCleanupService(jdbcTemplate);

        CourseCleanupRequest request = new CourseCleanupRequest("a1b2c3d4", List.of(), List.of(), List.of(), List.of());
        CourseCleanupResponse first = service.cleanup(request);
        CourseCleanupResponse second = service.cleanup(request);

        assertTrue(first.deleted().values().stream().allMatch(count -> count == 0));
        assertEquals(first.deleted(), second.deleted());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void deletesDependenciesBeforeOwnedUsers() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any())).thenReturn("pw-course-a1b2c3d4-owner");
        CourseTestCleanupService service = new CourseTestCleanupService(jdbcTemplate);

        CourseCleanupRequest request = new CourseCleanupRequest("a1b2c3d4", List.of(7L), List.of(8L), List.of(), List.of());
        service.cleanup(request);

        ArgumentCaptor<String> ownershipQueries = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).queryForObject(ownershipQueries.capture(), any(org.springframework.jdbc.core.RowMapper.class), any());
        assertTrue(ownershipQueries.getAllValues().get(0).contains("user_remark"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(Object[].class));
        List<String> statements = new ArrayList<>(sql.getAllValues());
        int item = indexContaining(statements, "order_item");
        int refund = indexContaining(statements, "refund");
        int payment = indexContaining(statements, "payment");
        int order = indexContaining(statements, "DELETE FROM `order`");
        int user = indexContaining(statements, "DELETE FROM user WHERE");
        int userCoupon = lastIndexContaining(statements, "user_coupon");
        assertTrue(item >= 0, "item=" + item);
        assertTrue(refund > item, "refund=" + refund + ", item=" + item);
        assertTrue(payment > refund, "payment=" + payment + ", refund=" + refund);
        assertTrue(order > payment, "order=" + order + ", payment=" + payment);
        assertTrue(userCoupon > payment, "userCoupon=" + userCoupon + ", payment=" + payment);
        assertTrue(user > userCoupon, "user=" + user + ", userCoupon=" + userCoupon);
    }

    @Test
    void validatesExactIdsAndReportsMissingAndRejectedWithoutDeletingThem() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    Long id = invocation.getArgument(2);
                    if (id == 1L) return "pw-course-a1b2c3d4-user";
                    if (id == 2L) return null;
                    return "someone-elses-resource";
                });
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        CourseCleanupRequest request = new CourseCleanupRequest("a1b2c3d4", List.of(1L, 2L, 3L), List.of(), List.of(), List.of());

        CourseCleanupResponse response = new CourseTestCleanupService(jdbcTemplate).cleanup(request);

        assertEquals(1, response.deleted().get("user"));
        assertEquals(1, response.notFound().get("user"));
        assertEquals(1, response.rejected().get("user"));
        verify(jdbcTemplate, atLeastOnce()).update(anyString(), any(Object[].class));
        ArgumentCaptor<Object[]> ids = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, atLeastOnce()).update(anyString(), ids.capture());
        assertTrue(ids.getAllValues().stream().noneMatch(values -> values.length == 1 && Long.valueOf(3L).equals(values[0])));
    }

    @Test
    void rejectsNearPrefixAndDeduplicatesResourceIds() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any()))
                .thenAnswer(invocation -> "pw-course-a1b2c3d4evil");
        CourseCleanupRequest request = new CourseCleanupRequest(
                "a1b2c3d4", List.of(4L, 4L), List.of(), List.of(), List.of());
        CourseCleanupResponse response = new CourseTestCleanupService(jdbcTemplate).cleanup(request);

        assertEquals(1, response.rejected().get("user"));
        assertEquals(0, response.deleted().getOrDefault("user", 0));
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(4L));
    }

    @Test
    void cleanupUserDeletesOnlyRunOwnedUser() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(7L)))
                .thenReturn("pw-course-a1b2c3d4-user");
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        CourseCleanupResponse response =
                new CourseTestCleanupService(jdbcTemplate).cleanupUser("a1b2c3d4", 7L);

        assertEquals(1, response.deleted().get("user"));
        assertEquals(0, response.rejected().getOrDefault("user", 0));
    }

    @Test
    void cleanupUserRejectsUserOwnedByAnotherRun() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(7L)))
                .thenReturn("pw-course-deadbeef-user");

        CourseCleanupResponse response =
                new CourseTestCleanupService(jdbcTemplate).cleanupUser("a1b2c3d4", 7L);

        assertEquals(1, response.rejected().get("user"));
        verify(jdbcTemplate, times(0)).update(anyString(), any(Object[].class));
    }

    @Test
    void compensatesPendingOrderInventoryBeforeDeletingItsRows() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(8L)))
                .thenReturn("pw-course-a1b2c3d4-order");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("order_status")) {
                return List.of(Map.of("order_status", 1, "ticket_id", 11L, "session_id", 21L));
            }
            if (sql.contains("price_level_id")) {
                return List.of(
                        Map.of("price_level_id", 41L, "quantity", 2),
                        Map.of("price_level_id", 42L, "quantity", 1));
            }
            return List.of();
        });
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        new CourseTestCleanupService(jdbcTemplate).cleanup(new CourseCleanupRequest(
                "a1b2c3d4", List.of(), List.of(8L), List.of(), List.of()));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), args.capture());
        assertUpdatePresent(sql.getAllValues(), args.getAllValues(), "ticket_price_level", 2, 41L, 2);
        assertUpdatePresent(sql.getAllValues(), args.getAllValues(), "ticket_price_level", 1, 42L, 1);
        assertUpdatePresent(sql.getAllValues(), args.getAllValues(), "UPDATE ticket SET available_stock", 3, 11L, 3);
        assertUpdatePresent(sql.getAllValues(), args.getAllValues(), "UPDATE ticket_session SET available_capacity", 3, 21L, 3);
    }

    @Test
    void doesNotCompensateCancelledOrderBecauseItsNormalLifecycleAlreadyRestoredInventory() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(8L)))
                .thenReturn("pw-course-a1b2c3d4-order");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("order_status")) {
                return List.of(Map.of("order_status", 4, "ticket_id", 11L, "session_id", 21L));
            }
            return List.of();
        });
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        new CourseTestCleanupService(jdbcTemplate).cleanup(new CourseCleanupRequest(
                "a1b2c3d4", List.of(), List.of(8L), List.of(), List.of()));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(Object[].class));
        assertTrue(sql.getAllValues().stream().noneMatch(statement -> statement.contains("available_stock = available_stock +")
                || statement.contains("available_capacity = available_capacity +")));
    }

    @Test
    void restoresManualAndAdminIssuedCouponStockButLeavesNonDecrementingGrantStockUnchanged() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(7L)))
                .thenReturn("pw-course-a1b2c3d4-user");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM `order`")) {
                return List.of();
            }
            if (sql.contains("FROM user_coupon")) {
                return List.of(
                        Map.of("coupon_id", 91L, "source", "manual"),
                        Map.of("coupon_id", 93L, "source", "admin"),
                        Map.of("coupon_id", 92L, "source", "register"));
            }
            return List.of();
        });
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        new CourseTestCleanupService(jdbcTemplate).cleanupUser("a1b2c3d4", 7L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), args.capture());
        assertUpdatePresent(sql.getAllValues(), args.getAllValues(), "UPDATE coupon SET remain_count", 1, 91L, 1);
        assertUpdatePresent(sql.getAllValues(), args.getAllValues(), "UPDATE coupon SET remain_count", 1, 93L, 1);
        assertTrue(args.getAllValues().stream().noneMatch(values -> values.length >= 2 && Long.valueOf(92L).equals(values[1])));
    }

    @Test
    void failsBeforeDeletingOrderWhenInventoryRestoreWouldExceedItsTotal() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(8L)))
                .thenReturn("pw-course-a1b2c3d4-order");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("order_status")) {
                return List.of(Map.of("order_status", 1, "ticket_id", 11L, "session_id", 21L));
            }
            if (sql.contains("price_level_id")) {
                return List.of(Map.of("price_level_id", 41L, "quantity", 2));
            }
            return List.of();
        });
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            return sql.contains("ticket_price_level") ? 0 : 1;
        });

        assertThrows(IllegalStateException.class, () -> new CourseTestCleanupService(jdbcTemplate).cleanup(
                new CourseCleanupRequest("a1b2c3d4", List.of(), List.of(8L), List.of(), List.of())));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(Object[].class));
        assertTrue(sql.getAllValues().stream().noneMatch(statement -> statement.startsWith("DELETE FROM `order`")));
    }

    @Test
    void compensatesPaidAndRefundStatesBecauseTheirCurrentLifecycleDoesNotRestoreInventory() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any()))
                .thenReturn("pw-course-a1b2c3d4-order");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("order_status")) {
                Long orderId = invocation.getArgument(1);
                int status = Long.valueOf(8L).equals(orderId) ? 2 : Long.valueOf(9L).equals(orderId) ? 5 : 6;
                return List.of(Map.of("order_status", status, "ticket_id", 11L, "session_id", 21L));
            }
            if (sql.contains("price_level_id")) {
                return List.of(Map.of("price_level_id", 41L, "quantity", 1));
            }
            return List.of();
        });
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        new CourseTestCleanupService(jdbcTemplate).cleanup(new CourseCleanupRequest(
                "a1b2c3d4", List.of(), List.of(8L, 9L, 10L), List.of(), List.of()));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(Object[].class));
        assertEquals(3, sql.getAllValues().stream()
                .filter(statement -> statement.contains("UPDATE ticket SET available_stock")).count());
    }

    @Test
    void handlesOrderWithoutSessionWithoutAttemptingSessionCapacityRestore() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(8L)))
                .thenReturn("pw-course-a1b2c3d4-order");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("order_status")) {
                Map<String, Object> order = new java.util.HashMap<>();
                order.put("order_status", 1);
                order.put("ticket_id", 11L);
                order.put("session_id", null);
                return List.of(order);
            }
            if (sql.contains("price_level_id")) {
                return List.of(Map.of("price_level_id", 41L, "quantity", 1));
            }
            return List.of();
        });
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        new CourseTestCleanupService(jdbcTemplate).cleanup(new CourseCleanupRequest(
                "a1b2c3d4", List.of(), List.of(8L), List.of(), List.of()));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(Object[].class));
        assertTrue(sql.getAllValues().stream().noneMatch(statement -> statement.contains("ticket_session")));
    }

    @Test
    void orderThenUserCleanupDoesNotRepeatCompensationAfterTheOrderWasDeleted() {
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any()))
                .thenAnswer(invocation -> Long.valueOf(7L).equals(invocation.getArgument(2))
                        ? "pw-course-a1b2c3d4-user" : "pw-course-a1b2c3d4-order");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("SELECT id FROM `order`")) {
                return List.of();
            }
            if (sql.contains("order_status")) {
                return List.of(Map.of("order_status", 1, "ticket_id", 11L, "session_id", 21L));
            }
            if (sql.contains("price_level_id")) {
                return List.of(Map.of("price_level_id", 41L, "quantity", 1));
            }
            return List.of();
        });
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        CourseTestCleanupService service = new CourseTestCleanupService(jdbcTemplate);
        service.cleanup(new CourseCleanupRequest("a1b2c3d4", List.of(), List.of(8L), List.of(), List.of()));
        service.cleanupUser("a1b2c3d4", 7L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), any(Object[].class));
        assertEquals(1, sql.getAllValues().stream()
                .filter(statement -> statement.contains("UPDATE ticket SET available_stock")).count());
        verify(jdbcTemplate).queryForList("SELECT id FROM `order` WHERE user_id = ? AND user_remark LIKE ?", 7L,
                "pw-course-a1b2c3d4-%");
    }

    private static void assertUpdatePresent(List<String> statements, List<Object[]> values,
                                            String sqlFragment, int quantity, Long id, int cappedQuantity) {
        for (int i = 0; i < statements.size(); i++) {
            Object[] args = values.get(i);
            if (statements.get(i).contains(sqlFragment)
                    && args.length >= 3
                    && Integer.valueOf(quantity).equals(args[0])
                    && id.equals(args[args.length - 2])
                    && Integer.valueOf(cappedQuantity).equals(args[args.length - 1])) {
                return;
            }
        }
        throw new AssertionError("missing update " + sqlFragment + " for id=" + id);
    }

    private static int indexContaining(List<String> values, String text) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).contains(text)) {
                return i;
            }
        }
        return -1;
    }

    private static int lastIndexContaining(List<String> values, String text) {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i).contains(text)) {
                return i;
            }
        }
        return -1;
    }
}
