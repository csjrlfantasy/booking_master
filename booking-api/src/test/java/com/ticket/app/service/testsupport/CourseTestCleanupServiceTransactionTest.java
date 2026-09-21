package com.ticket.app.service.testsupport;

import com.ticket.app.controller.testsupport.dto.CourseCleanupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseTestCleanupServiceTransactionTest {
    private JdbcTemplate jdbc;
    private CourseTestCleanupService cleanup;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:cleanup;MODE=MySQL;NON_KEYWORDS=USER,ORDER;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE user (id BIGINT PRIMARY KEY, username VARCHAR(255))");
        jdbc.execute("CREATE TABLE `order` (id BIGINT PRIMARY KEY, user_id BIGINT, user_remark VARCHAR(255), order_status INT, ticket_id BIGINT, session_id BIGINT, inventory_restored TINYINT DEFAULT 0)");
        jdbc.execute("CREATE TABLE order_item (id BIGINT PRIMARY KEY, order_id BIGINT, price_level_id BIGINT, quantity INT)");
        jdbc.execute("CREATE TABLE ticket (id BIGINT PRIMARY KEY, total_stock INT, available_stock INT, sale_count INT)");
        jdbc.execute("CREATE TABLE ticket_price_level (id BIGINT PRIMARY KEY, total_stock INT, available_stock INT, sold_count INT)");
        jdbc.execute("CREATE TABLE ticket_session (id BIGINT PRIMARY KEY, total_capacity INT, available_capacity INT)");
        jdbc.execute("CREATE TABLE coupon (id BIGINT PRIMARY KEY, total_count INT, remain_count INT)");
        jdbc.execute("CREATE TABLE user_coupon (id BIGINT PRIMARY KEY, user_id BIGINT, coupon_id BIGINT, source VARCHAR(20))");
        for (String table : List.of("refund", "payment", "user_identity", "user_auth", "user_address", "cart", "user_role_relation")) {
            jdbc.execute("CREATE TABLE " + table + " (id BIGINT PRIMARY KEY, user_id BIGINT, order_id BIGINT)");
        }
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);
        RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
        attribute.setRollbackRules(List.of(new org.springframework.transaction.interceptor.RollbackRuleAttribute(Exception.class)));
        MatchAlwaysTransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();
        source.setTransactionAttribute(attribute);
        ProxyFactory proxy = new ProxyFactory(new CourseTestCleanupService(jdbc));
        proxy.addAdvice(new TransactionInterceptor(manager, source));
        cleanup = (CourseTestCleanupService) proxy.getProxy();
    }

    @Test
    void rollsBackEarlierInventoryRestoreWhenLaterCapWouldBeExceeded() {
        seedOrder(8L, 7L, 1, 11L, 21L);
        jdbc.update("INSERT INTO ticket VALUES (11, 10, 8, 2)");
        jdbc.update("INSERT INTO ticket_session VALUES (21, 10, 8)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (41, 10, 8, 2)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (42, 10, 9, 1)");
        jdbc.update("INSERT INTO order_item VALUES (1, 8, 41, 1)");
        jdbc.update("INSERT INTO order_item VALUES (2, 8, 42, 2)");

        assertThrows(IllegalStateException.class, () -> cleanup.cleanup(request(List.of(8L), List.of())));

        assertEquals(8, scalar("SELECT available_stock FROM ticket_price_level WHERE id=41"));
        assertEquals(9, scalar("SELECT available_stock FROM ticket_price_level WHERE id=42"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM `order` WHERE id=8"));
    }

    @Test
    void orderThenUserAndRetryRestoreAggregatedInventoryAndAllCouponRowsExactlyOnce() {
        jdbc.update("INSERT INTO user VALUES (7, 'pw-course-a1b2c3d4-user')");
        seedOrder(8L, 7L, 1, 11L, 21L);
        jdbc.update("INSERT INTO ticket VALUES (11, 10, 7, 3)");
        jdbc.update("INSERT INTO ticket_session VALUES (21, 10, 7)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (41, 10, 7, 3)");
        jdbc.update("INSERT INTO order_item VALUES (1, 8, 41, 1)");
        jdbc.update("INSERT INTO order_item VALUES (2, 8, 41, 2)");
        jdbc.update("INSERT INTO coupon VALUES (91, 10, 8)");
        jdbc.update("INSERT INTO user_coupon VALUES (1, 7, 91, 'manual')");
        jdbc.update("INSERT INTO user_coupon VALUES (2, 7, 91, 'activity')");

        cleanup.cleanup(request(List.of(8L), List.of()));
        cleanup.cleanupUser("a1b2c3d4", 7L);
        cleanup.cleanup(request(List.of(8L), List.of(7L)));

        assertEquals(10, scalar("SELECT available_stock FROM ticket WHERE id=11"));
        assertEquals(0, scalar("SELECT sale_count FROM ticket WHERE id=11"));
        assertEquals(10, scalar("SELECT available_stock FROM ticket_price_level WHERE id=41"));
        assertEquals(0, scalar("SELECT sold_count FROM ticket_price_level WHERE id=41"));
        assertEquals(10, scalar("SELECT available_capacity FROM ticket_session WHERE id=21"));
        assertEquals(10, scalar("SELECT remain_count FROM coupon WHERE id=91"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM `order`"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM user_coupon"));
    }

    @Test
    void cancelledThenRefundedFactCannotTriggerSecondRestoration() {
        seedOrder(8L, 7L, 5, 11L, null);
        jdbc.update("UPDATE `order` SET inventory_restored=1 WHERE id=8");
        jdbc.update("INSERT INTO ticket VALUES (11,10,8,2)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (41,10,8,2)");
        jdbc.update("INSERT INTO order_item VALUES (1,8,41,1)");
        cleanup.cleanup(request(List.of(8L), List.of()));
        assertEquals(8, scalar("SELECT available_stock FROM ticket WHERE id=11"));
        assertEquals(2, scalar("SELECT sale_count FROM ticket WHERE id=11"));
    }

    @Test
    void statusAloneNeverSuppressesUnrestoredInventory() {
        for (int status : List.of(1,2,3,4,5,6)) {
            seedOrder((long)status, 7L, status, 11L, null);
            jdbc.update("INSERT INTO order_item VALUES (?,?,41,1)", status, status);
        }
        jdbc.update("INSERT INTO ticket VALUES (11,10,4,6)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (41,10,4,6)");
        cleanup.cleanup(request(List.of(6L,5L,4L,3L,2L,1L), List.of()));
        assertEquals(10, scalar("SELECT available_stock FROM ticket WHERE id=11"));
        assertEquals(0, scalar("SELECT sale_count FROM ticket WHERE id=11"));
    }

    @Test
    void couponCapFailureRollsBackAlreadyRestoredAndDeletedOrder() {
        jdbc.update("INSERT INTO user VALUES (7, 'pw-course-a1b2c3d4-user')");
        seedOrder(8L, 7L, 1, 11L, null);
        jdbc.update("INSERT INTO ticket VALUES (11,10,9,1)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (41,10,9,1)");
        jdbc.update("INSERT INTO order_item VALUES (1,8,41,1)");
        jdbc.update("INSERT INTO coupon VALUES (91,10,10)");
        jdbc.update("INSERT INTO user_coupon VALUES (1,7,91,'order')");
        assertThrows(IllegalStateException.class, () -> cleanup.cleanupUser("a1b2c3d4", 7L));
        assertEquals(9, scalar("SELECT available_stock FROM ticket WHERE id=11"));
        assertEquals(1, scalar("SELECT sale_count FROM ticket WHERE id=11"));
        assertEquals(0, scalar("SELECT inventory_restored FROM `order` WHERE id=8"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM order_item"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM user"));
    }

    @Test
    void unknownLegacyAndRejectedOwnerNeverMutateInventory() {
        seedOrder(8L, 7L, 4, 11L, null);
        jdbc.update("UPDATE `order` SET inventory_restored=NULL WHERE id=8");
        assertThrows(IllegalStateException.class, () -> cleanup.cleanup(request(List.of(8L), List.of())));
        jdbc.update("UPDATE `order` SET user_remark='another-run' WHERE id=8");
        var result = cleanup.cleanup(request(List.of(8L), List.of()));
        assertEquals(1, result.rejected().get("order"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM `order`"));
    }

    @Test
    void restoresAllCurrentSourcesIncludingBoundAndUsedCouponsOnce() {
        jdbc.execute("ALTER TABLE user_coupon ADD status INT DEFAULT 0");
        jdbc.execute("ALTER TABLE user_coupon ADD order_id BIGINT");
        jdbc.update("INSERT INTO user VALUES (7, 'pw-course-a1b2c3d4-user')");
        seedOrder(8L,7L,5,11L,null);
        jdbc.update("UPDATE `order` SET inventory_restored=1 WHERE id=8");
        jdbc.update("INSERT INTO coupon VALUES (91,10,4)");
        int id=0;
        for (String source : List.of("manual","admin","register","order","activity","custom")) {
            jdbc.update("INSERT INTO user_coupon VALUES (?,7,91,?,?,8)", ++id, source, id % 3);
        }
        cleanup.cleanupUser("a1b2c3d4",7L);
        var retry = cleanup.cleanupUser("a1b2c3d4",7L);
        assertEquals(10, scalar("SELECT remain_count FROM coupon WHERE id=91"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM user_coupon"));
        assertEquals(1, retry.notFound().get("user"));
    }

    @Test
    void overlappingOrderAndUserCleanupRestoresExactlyOnce() throws Exception {
        jdbc.update("INSERT INTO user VALUES (7, 'pw-course-a1b2c3d4-user')");
        seedOrder(8L, 7L, 1, 11L, null);
        jdbc.update("INSERT INTO ticket VALUES (11,10,9,1)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (41,10,9,1)");
        jdbc.update("INSERT INTO order_item VALUES (1,8,41,1)");
        jdbc.update("INSERT INTO coupon VALUES (91,10,9)");
        jdbc.update("INSERT INTO user_coupon VALUES (1,7,91,'custom-admin')");
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var order = pool.submit(() -> { start.await(); return cleanup.cleanup(request(List.of(8L), List.of())); });
            var user = pool.submit(() -> { start.await(); return cleanup.cleanupUser("a1b2c3d4", 7L); });
            start.countDown();
            order.get(5, java.util.concurrent.TimeUnit.SECONDS);
            user.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(10, scalar("SELECT available_stock FROM ticket WHERE id=11"));
            assertEquals(0, scalar("SELECT sale_count FROM ticket WHERE id=11"));
            assertEquals(10, scalar("SELECT remain_count FROM coupon WHERE id=91"));
            assertEquals(0, scalar("SELECT COUNT(*) FROM user"));
            assertEquals(0, scalar("SELECT COUNT(*) FROM `order`"));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void overlappingBatchAndStandaloneOrderWithSharedStockCannotInvertLocks() throws Exception {
        seedOrder(8L,7L,1,11L,null);
        seedOrder(9L,7L,1,11L,null);
        jdbc.update("INSERT INTO ticket VALUES (11,10,8,2)");
        jdbc.update("INSERT INTO ticket_price_level VALUES (41,10,8,2)");
        jdbc.update("INSERT INTO order_item VALUES (1,8,41,1),(2,9,41,1)");
        var standaloneHasOrder9 = new java.util.concurrent.CountDownLatch(1);
        var batchAboutToLockOrder9 = new java.util.concurrent.CountDownLatch(1);
        var hookedJdbc = new JdbcTemplate(jdbc.getDataSource()) {
            @Override
            public <T> T queryForObject(String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
                if (sql.equals("SELECT user_remark FROM `order` WHERE id = ? FOR UPDATE") && args[0].equals(9L)) {
                    batchAboutToLockOrder9.countDown();
                }
                return super.queryForObject(sql, mapper, args);
            }
        };
        var manager = new DataSourceTransactionManager(jdbc.getDataSource());
        var proxy = new ProxyFactory(new CourseTestCleanupService(hookedJdbc));
        proxy.addAdvice(new TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var batchCleanup = (CourseTestCleanupService)proxy.getProxy();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var standalone = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(manager).execute(status -> {
                jdbc.queryForList("SELECT id FROM `order` WHERE id=9 FOR UPDATE");
                standaloneHasOrder9.countDown();
                await(batchAboutToLockOrder9);
                return cleanup.cleanup(request(List.of(9L),List.of()));
            }));
            var batch = pool.submit(() -> {
                await(standaloneHasOrder9);
                return batchCleanup.cleanup(request(List.of(9L,8L),List.of()));
            });
            standalone.get(10,java.util.concurrent.TimeUnit.SECONDS);
            var batchResult = batch.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1,batchResult.notFound().get("order"));
            assertEquals(10,scalar("SELECT available_stock FROM ticket WHERE id=11"));
            assertEquals(10,scalar("SELECT available_stock FROM ticket_price_level WHERE id=41"));
            assertEquals(0,scalar("SELECT sale_count FROM ticket WHERE id=11"));
            assertEquals(0,scalar("SELECT sold_count FROM ticket_price_level WHERE id=41"));
            assertEquals(0,scalar("SELECT COUNT(*) FROM `order`"));
        } finally {
            standaloneHasOrder9.countDown();
            batchAboutToLockOrder9.countDown();
            pool.shutdownNow();
        }
    }

    private static void await(java.util.concurrent.CountDownLatch latch) {
        try {
            org.junit.jupiter.api.Assertions.assertTrue(latch.await(5,java.util.concurrent.TimeUnit.SECONDS),
                    "expected cleanup lock checkpoint was not reached");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private void seedOrder(Long id, Long userId, int status, Long ticketId, Long sessionId) {
        jdbc.update("INSERT INTO `order` (id,user_id,user_remark,order_status,ticket_id,session_id) VALUES (?, ?, ?, ?, ?, ?)", id, userId,
                "pw-course-a1b2c3d4-order", status, ticketId, sessionId);
    }

    private CourseCleanupRequest request(List<Long> orders, List<Long> users) {
        return new CourseCleanupRequest("a1b2c3d4", users, orders, List.of(), List.of());
    }

    private int scalar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
