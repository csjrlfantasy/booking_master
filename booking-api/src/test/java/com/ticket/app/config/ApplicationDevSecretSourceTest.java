package com.ticket.app.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationDevSecretSourceTest {
    @Test
    void devConfigurationRequiresSecretsFromEnvironment() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application-dev.yml"));
        assertTrue(yaml.contains("${DB_PASSWORD}"));
        assertTrue(yaml.contains("${JWT_SECRET}"));
        assertTrue(yaml.contains("${PAYMENT_CALLBACK_SECRET_KEY}"));
        assertTrue(yaml.contains("${COURSE_TEST_KEY}"));
        assertFalse(yaml.contains("COURSE_TEST_KEY:playwright-course-local"));
        assertFalse(yaml.matches("(?s).*password:\\s+[^$\\s].*"));
    }
}
