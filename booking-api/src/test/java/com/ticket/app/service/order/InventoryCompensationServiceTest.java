package com.ticket.app.service.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InventoryCompensationServiceTest {
    private JdbcTemplate jdbc;
    private InventoryCompensationService inventory;
    private DataSourceTransactionManager manager;

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE `order` (id BIGINT PRIMARY KEY, ticket_id BIGINT, session_id BIGINT, inventory_restored TINYINT DEFAULT 0, order_status INT)");
        jdbc.execute("CREATE TABLE order_item (id BIGINT PRIMARY KEY, order_id BIGINT, price_level_id BIGINT, quantity INT)");
        jdbc.execute("CREATE TABLE ticket_price_level (id BIGINT PRIMARY KEY, available_stock INT, total_stock INT, sold_count INT)");
        jdbc.execute("CREATE TABLE ticket (id BIGINT PRIMARY KEY, available_stock INT, total_stock INT, sale_count INT)");
        jdbc.execute("CREATE TABLE ticket_session (id BIGINT PRIMARY KEY, available_capacity INT, total_capacity INT)");
        jdbc.update("INSERT INTO `order` VALUES (8,11,21,0,1)");
        jdbc.update("INSERT INTO order_item VALUES (1,8,41,1),(2,8,41,2),(3,8,42,1)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (41,7,10,3),(42,9,10,1)");
        jdbc.update("INSERT INTO ticket VALUES (11,6,10,4)");
        jdbc.update("INSERT INTO ticket_session VALUES (21,6,10)");
        var source = new MatchAlwaysTransactionAttributeSource();
        source.setTransactionAttribute(new RuleBasedTransactionAttribute());
        manager = new DataSourceTransactionManager(ds);
        var proxy = new ProxyFactory(new InventoryCompensationService(jdbc));
        proxy.addAdvice(new TransactionInterceptor(manager, source));
        inventory = (InventoryCompensationService) proxy.getProxy();
    }

    @Test
    void aggregatesAndPersistsFactAcrossLaterRefundStatus() {
        assertTrue(inventory.restoreOnce(8L));
        jdbc.update("UPDATE `order` SET order_status=5 WHERE id=8");
        assertFalse(inventory.restoreOnce(8L));
        assertEquals(10, value("ticket", "available_stock", 11));
        assertEquals(0, value("ticket", "sale_count", 11));
        assertEquals(10, value("ticket_price_level", "available_stock", 41));
        assertEquals(0, value("ticket_price_level", "sold_count", 41));
        assertEquals(10, value("ticket_session", "available_capacity", 21));
        assertEquals(1, value("`order`", "inventory_restored", 8));
    }

    @Test
    void rejectsSoldCounterUnderflowAndRollsBackEarlierPriceLevel() {
        jdbc.update("UPDATE ticket_price_level SET sold_count=0 WHERE id=42");
        assertThrows(IllegalStateException.class, () -> inventory.restoreOnce(8L));
        assertEquals(7, value("ticket_price_level", "available_stock", 41));
        assertEquals(3, value("ticket_price_level", "sold_count", 41));
        assertEquals(0, value("`order`", "inventory_restored", 8));
    }

    @Test
    void rejectsTicketSalesUnderflowAndRollsBackBothPriceLevels() {
        jdbc.update("UPDATE ticket SET sale_count=3 WHERE id=11");
        assertThrows(IllegalStateException.class, () -> inventory.restoreOnce(8L));
        assertEquals(7, value("ticket_price_level", "available_stock", 41));
        assertEquals(9, value("ticket_price_level", "available_stock", 42));
    }

    @Test
    void rejectsLateSessionCapAndRollsBackEverything() {
        jdbc.update("UPDATE ticket_session SET available_capacity=7 WHERE id=21");
        assertThrows(IllegalStateException.class, () -> inventory.restoreOnce(8L));
        assertEquals(6, value("ticket", "available_stock", 11));
        assertEquals(4, value("ticket", "sale_count", 11));
        assertEquals(7, value("ticket_price_level", "available_stock", 41));
        assertEquals(0, value("`order`", "inventory_restored", 8));
    }

    @Test
    void handlesNullableSessionAndAbsentOrder() {
        jdbc.update("UPDATE `order` SET session_id=NULL WHERE id=8");
        assertTrue(inventory.restoreOnce(8L));
        assertFalse(inventory.restoreOnce(999L));
        assertEquals(6, value("ticket_session", "available_capacity", 21));
    }

    @Test
    void rejectsUnknownLegacyFactAndInvalidItems() {
        jdbc.update("UPDATE `order` SET inventory_restored=NULL WHERE id=8");
        assertThrows(IllegalStateException.class, () -> inventory.restoreOnce(8L));
        jdbc.update("UPDATE `order` SET inventory_restored=0 WHERE id=8");
        jdbc.update("UPDATE order_item SET quantity=-1 WHERE id=1");
        assertThrows(IllegalStateException.class, () -> inventory.restoreOnce(8L));
        assertEquals(7, value("ticket_price_level", "available_stock", 41));
    }

    @Test
    void concurrentRestorationMutatesExactlyOnce() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> inventory.restoreOnce(8L));
            var b = pool.submit(() -> inventory.restoreOnce(8L));
            assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
            assertEquals(10, value("ticket", "available_stock", 11));
            assertEquals(0, value("ticket", "sale_count", 11));
        } finally { pool.shutdownNow(); }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"customer", "expiry", "admin", "payment", "direct"})
    void everyLifecycleUsesTransactionalPersistentCompensation(String path) {
        invokeCancellation(path);
        assertEquals(4, value("`order`", "order_status", 8));
        assertEquals(1, value("`order`", "inventory_restored", 8));
        assertEquals(0, value("ticket", "sale_count", 11));
        jdbc.update("UPDATE `order` SET order_status=5 WHERE id=8");
        assertFalse(inventory.restoreOnce(8L));
        assertEquals(10, value("ticket", "available_stock", 11));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"customer", "expiry", "admin", "payment", "direct"})
    void everyLifecycleRollsBackStatusAndEarlierInventoryWhenLaterInvariantFails(String path) {
        jdbc.update("UPDATE ticket_session SET available_capacity=7 WHERE id=21");
        assertThrows(IllegalStateException.class, () -> invokeCancellation(path));
        assertEquals(1, value("`order`", "order_status", 8));
        assertEquals(0, value("`order`", "inventory_restored", 8));
        assertEquals(6, value("ticket", "available_stock", 11));
        assertEquals(7, value("ticket_price_level", "available_stock", 41));
    }

    @Test
    void historicalUnknownRowsStayUnknownWhenFutureInsertDefaultChanges() {
        jdbc.execute("CREATE TABLE legacy_order (id INT PRIMARY KEY)");
        jdbc.update("INSERT INTO legacy_order VALUES (1)");
        jdbc.execute("ALTER TABLE legacy_order ADD inventory_restored TINYINT NULL DEFAULT NULL");
        jdbc.execute("ALTER TABLE legacy_order ALTER COLUMN inventory_restored SET DEFAULT 0");
        jdbc.update("INSERT INTO legacy_order (id) VALUES (2)");
        assertNull(jdbc.queryForObject("SELECT inventory_restored FROM legacy_order WHERE id=1", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT inventory_restored FROM legacy_order WHERE id=2", Integer.class));
    }

    @Test
    void supportsBooleanJdbcFactsWithoutNumericCasting() {
        jdbc.execute("ALTER TABLE `order` ALTER COLUMN inventory_restored BOOLEAN");
        assertTrue(inventory.restoreOnce(8L));
        assertFalse(inventory.restoreOnce(8L));
    }

    @Test
    void staleMybatisEntitiesCannotOverwritePersistentFact() throws Exception {
        var field = com.ticket.app.entity.Order.class.getDeclaredField("inventoryRestored");
        var mapping = field.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);
        assertEquals(com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER, mapping.updateStrategy());
        assertEquals(com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER, mapping.insertStrategy());
    }

    private void invokeCancellation(String path) {
        // Repository seam executes real status SQL; the entry service, compensation
        // SQL, row locks and transaction rollback run against the same H2 database.
        var orders = org.mockito.Mockito.mock(com.ticket.app.repository.order.OrderRepository.class);
        var order = new com.ticket.app.entity.Order();
        order.setId(8L); order.setUserId(7L); order.setOrderNo("ORDER-8");
        order.setTicketId(11L); order.setSessionId(21L);
        order.setOrderStatus(1); order.setPayStatus(0);
        order.setExpireTime(java.time.LocalDateTime.now().minusMinutes(1));
        org.mockito.Mockito.when(orders.selectById(8L)).thenReturn(order);
        org.mockito.Mockito.when(orders.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(order);
        org.mockito.Mockito.when(orders.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(order));
        org.mockito.Mockito.when(orders.updateById(org.mockito.ArgumentMatchers.any(com.ticket.app.entity.Order.class)))
                .thenAnswer(call -> jdbc.update("UPDATE `order` SET order_status=? WHERE id=?",
                        ((com.ticket.app.entity.Order)call.getArgument(0)).getOrderStatus(), 8L));
        switch (path) {
            case "customer", "direct" -> {
                var service = transactional(new OrderServiceImpl(orders,null,null,null,null,null,null,inventory));
                if (path.equals("direct")) service.updateOrderStatus(7L,8L,4);
                else {
                    var request = new com.ticket.app.controller.order.dto.CancelOrderRequest();
                    request.setOrderId(8L);
                    service.cancelOrder(7L,request);
                }
            }
            case "expiry" -> transactional(new OrderExpirationService(orders,inventory,null)).cancelExpiredPendingOrders();
            case "admin" -> transactional(new com.ticket.app.service.admin.AdminOrderServiceImpl(null,orders,null,null,inventory))
                    .cancelOrder(8L,new com.ticket.app.controller.admin.dto.request.AdminCancelOrderRequest());
            case "payment" -> transactional(new com.ticket.app.service.payment.PaymentServiceImpl(orders,null,null,inventory))
                    .cancelPayment(7L,"ORDER-8");
            default -> throw new IllegalArgumentException(path);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T transactional(T target) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        // Read the real production annotations, including the newly transactional
        // direct status path; do not add transactions that production lacks.
        proxy.addAdvice(new TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        return (T) proxy.getProxy();
    }

    private int value(String table, String column, long id) {
        return jdbc.queryForObject("SELECT " + column + " FROM " + table + " WHERE id=?", Integer.class, id);
    }
}
