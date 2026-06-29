package com.ticket.app.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebMvcConfigTest {

    @Test
    void jwtExcludePathPatternsShouldAllowUploadedImages() {
        assertTrue(WebMvcConfig.jwtExcludePathPatterns().contains("/api/uploads/**"));
        assertTrue(WebMvcConfig.jwtExcludePathPatterns().contains("/uploads/**"));
    }
}
