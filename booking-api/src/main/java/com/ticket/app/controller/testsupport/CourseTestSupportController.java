package com.ticket.app.controller.testsupport;

import com.ticket.app.common.Result;
import com.ticket.app.controller.testsupport.dto.CourseCleanupRequest;
import com.ticket.app.controller.testsupport.dto.CourseCleanupResponse;
import com.ticket.app.service.testsupport.CourseTestCleanupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Profile("dev")
@RestController
@RequestMapping("/test-support")
@ConditionalOnProperty(prefix = "course.test-support", name = "enabled", havingValue = "true")
public class CourseTestSupportController {
    private final CourseTestCleanupService cleanupService;
    private final String expectedKey;

    public CourseTestSupportController(
            CourseTestCleanupService cleanupService,
            @Value("${course.test-support.key}") String expectedKey) {
        this.cleanupService = cleanupService;
        this.expectedKey = expectedKey;
    }

    @PostMapping("/cleanup")
    public Result<CourseCleanupResponse> cleanup(
            @RequestHeader("X-Course-Test-Key") String suppliedKey,
            @Valid @RequestBody CourseCleanupRequest request) {
        requireValidKey(suppliedKey);
        return Result.success(cleanupService.cleanup(request));
    }

    @DeleteMapping("/users/{userId}")
    public Result<CourseCleanupResponse> deleteUser(
            @RequestHeader("X-Course-Test-Key") String suppliedKey,
            @PathVariable Long userId,
            @RequestParam String runId) {
        requireValidKey(suppliedKey);
        return Result.success(cleanupService.cleanupUser(runId, userId));
    }

    private void requireValidKey(String suppliedKey) {
        if (!MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid course test key");
        }
    }
}
