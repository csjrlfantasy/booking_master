package com.ticket.app.service.order;

import com.ticket.app.config.OrderSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventorySchemaMigrationTest {
    @Test
    void initializerPreservesUnknownHistoricalFactsAndChangesOnlyFutureDefault() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("inventory_restored"))).thenReturn(0,1);
        var initializer = new OrderSchemaInitializer(jdbc);
        initializer.run();
        initializer.run();
        verify(jdbc,times(1)).execute("ALTER TABLE `order` ADD COLUMN `inventory_restored` TINYINT(1) NULL DEFAULT NULL COMMENT '库存是否已回补，NULL待审计'");
        verify(jdbc,times(2)).execute("ALTER TABLE `order` ALTER COLUMN `inventory_restored` SET DEFAULT 0");
        verify(jdbc,never()).update(anyString());
    }
}
