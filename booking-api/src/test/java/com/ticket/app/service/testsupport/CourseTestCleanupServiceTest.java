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
        int userOrder = lastIndexContaining(statements, "DELETE FROM `order` WHERE user_id");
        assertTrue(item >= 0, "item=" + item);
        assertTrue(refund > item, "refund=" + refund + ", item=" + item);
        assertTrue(payment > refund, "payment=" + payment + ", refund=" + refund);
        assertTrue(order > payment, "order=" + order + ", payment=" + payment);
        assertTrue(userCoupon > payment, "userCoupon=" + userCoupon + ", payment=" + payment);
        assertTrue(userOrder > userCoupon, "userOrder=" + userOrder + ", userCoupon=" + userCoupon);
        assertTrue(user > userOrder, "user=" + user + ", userOrder=" + userOrder);
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
