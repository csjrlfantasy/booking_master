package com.ticket.app.service.order;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderSchemaInitializerSourceTest {

    @Test
    void orderSchemaInitializerShouldBackfillCouponAndContactColumns() throws Exception {
        String schema = Files.readString(Path.of("src/main/java/com/ticket/app/config/OrderSchemaInitializer.java"));

        assertTrue(schema.contains("user_coupon_id"));
        assertTrue(schema.contains("coupon_name"));
        assertTrue(schema.contains("coupon_amount"));
        assertTrue(schema.contains("contact_name"));
        assertTrue(schema.contains("contact_phone"));
        assertTrue(schema.contains("contact_id_card"));
        assertTrue(schema.contains("user_remark"));
        assertTrue(schema.contains("expire_time"));
    }
}
