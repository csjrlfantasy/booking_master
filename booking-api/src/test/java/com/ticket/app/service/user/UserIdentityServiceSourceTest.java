package com.ticket.app.service.user;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserIdentityServiceSourceTest {

    @Test
    void serviceShouldValidateIdCardAndMaskResponse() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/ticket/app/service/user/UserIdentityServiceImpl.java"));
        String response = Files.readString(Path.of("src/main/java/com/ticket/app/controller/user/dto/UserIdentityResponse.java"));
        String controller = Files.readString(Path.of("src/main/java/com/ticket/app/controller/user/UserController.java"));

        assertTrue(service.contains("IdCardUtils.isValid"));
        assertTrue(service.contains("IdCardUtils.mask"));
        assertTrue(response.contains("maskedIdCardNo"));
        assertTrue(controller.contains("@GetMapping(\"/identities\")"));
        assertTrue(controller.contains("@PostMapping(\"/identities\")"));
    }
}
