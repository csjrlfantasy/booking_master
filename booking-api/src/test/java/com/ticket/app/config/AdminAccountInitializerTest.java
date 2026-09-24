package com.ticket.app.config;

import com.ticket.app.common.util.PasswordUtils;
import com.ticket.app.entity.User;
import com.ticket.app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAccountInitializerTest {
    private final UserRepository repository = mock(UserRepository.class);

    private AnnotationConfigApplicationContext context(String password) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // Do not let developer-machine credentials affect these isolated tests.
        context.getEnvironment().getPropertySources().remove("systemEnvironment");
        context.getEnvironment().getPropertySources().remove("systemProperties");
        if (password != null) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of("BOOKING_ADMIN_PASSWORD", password)));
        }
        context.registerBean(PropertySourcesPlaceholderConfigurer.class);
        context.registerBean(UserRepository.class, () -> repository);
        context.register(AdminAccountInitializer.class);
        context.refresh();
        return context;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void firstCreationRequiresNonBlankPassword(String password) {
        try (var context = context(password)) {
            var exception = assertThrows(IllegalStateException.class,
                    () -> context.getBean(AdminAccountInitializer.class).run());
            assertEquals("BOOKING_ADMIN_PASSWORD is required to create the administrator account", exception.getMessage());
            verify(repository, never()).insert(any(User.class));
        }
    }

    @Test
    void createsAdministratorWithConfiguredPasswordHash() {
        String password = UUID.randomUUID().toString();
        try (var context = context(password)) {
            context.getBean(AdminAccountInitializer.class).run();
            var captor = ArgumentCaptor.forClass(User.class);
            verify(repository).insert(captor.capture());
            User administrator = captor.getValue();
            assertEquals("admin", administrator.getUsername());
            assertTrue(PasswordUtils.matches(password, administrator.getPassword()));
            assertNotEquals(password, administrator.getPassword());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "unused-test-input"})
    void existingAdministratorIsNeverReset(String password) {
        User existing = new User();
        existing.setPassword("existing-hash");
        when(repository.selectOne(any())).thenReturn(existing);
        try (var context = context(password)) {
            assertDoesNotThrow(() -> context.getBean(AdminAccountInitializer.class).run());
            verify(repository).selectOne(any());
            verifyNoMoreInteractions(repository);
            assertEquals("existing-hash", existing.getPassword());
        }
    }
}
