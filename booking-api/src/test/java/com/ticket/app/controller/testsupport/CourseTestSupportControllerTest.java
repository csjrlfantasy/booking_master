package com.ticket.app.controller.testsupport;

import com.ticket.app.common.Result;
import com.ticket.app.controller.testsupport.dto.CourseCleanupRequest;
import com.ticket.app.controller.testsupport.dto.CourseCleanupResponse;
import com.ticket.app.service.testsupport.CourseTestCleanupService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseTestSupportControllerTest {
    private final CourseTestCleanupService service = mock(CourseTestCleanupService.class);
    private final CourseTestSupportController controller =
            new CourseTestSupportController(service, "playwright-course-local");

    @Test
    void cleanupRejectsWrongCourseKey() {
        CourseCleanupRequest request = request();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.cleanup("wrong-key", request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void cleanupReturnsServiceCountsForMatchingKey() {
        CourseCleanupRequest request = request();
        CourseCleanupResponse response = new CourseCleanupResponse(
                "a1b2c3d4", Map.of("user", 1, "order", 2), Map.of(), Map.of());
        when(service.cleanup(request)).thenReturn(response);

        Result<CourseCleanupResponse> result =
                controller.cleanup("playwright-course-local", request);

        assertEquals(response, result.getData());
        verify(service).cleanup(request);
    }

    @Test
    void deleteUserRejectsWrongCourseKey() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.deleteUser("wrong-key", 7L, "a1b2c3d4"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void deleteUserDelegatesForMatchingKey() {
        CourseCleanupResponse response = new CourseCleanupResponse(
                "a1b2c3d4", Map.of("user", 1), Map.of(), Map.of());
        when(service.cleanupUser("a1b2c3d4", 7L)).thenReturn(response);

        Result<CourseCleanupResponse> result =
                controller.deleteUser("playwright-course-local", 7L, "a1b2c3d4");

        assertEquals(response, result.getData());
        verify(service).cleanupUser("a1b2c3d4", 7L);
    }

    private static CourseCleanupRequest request() {
        return new CourseCleanupRequest("a1b2c3d4", List.of(7L), List.of(8L), List.of(9L), List.of());
    }
}
