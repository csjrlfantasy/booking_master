package com.ticket.app.service.order;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderIdentityValidationSourceTest {

    @Test
    void orderCreationShouldValidateAttendeesAndPersistMaskedIdentityFields() throws Exception {
        String request = Files.readString(Path.of("src/main/java/com/ticket/app/controller/order/dto/CreateOrderRequest.java"));
        String service = Files.readString(Path.of("src/main/java/com/ticket/app/service/order/OrderServiceImpl.java"));
        String item = Files.readString(Path.of("src/main/java/com/ticket/app/entity/OrderItem.java"));
        String schema = Files.readString(Path.of("src/main/java/com/ticket/app/config/OrderItemAttendeeSchemaInitializer.java"));

        assertTrue(request.contains("List<AttendeeRequest> attendees"));
        assertTrue(request.contains("identityId"));
        assertTrue(request.contains("idCardNo"));
        assertTrue(service.contains("validateAttendees"));
        assertTrue(service.contains("IdCardUtils.calculateAge"));
        assertTrue(service.contains("观演人数量必须和购票数量一致"));
        assertTrue(service.contains("同一订单不能重复使用身份证"));
        assertTrue(item.contains("attendeeName"));
        assertTrue(item.contains("attendeeIdCard"));
        assertTrue(schema.contains("attendee_name"));
        assertTrue(schema.contains("attendee_id_card"));
    }
}
