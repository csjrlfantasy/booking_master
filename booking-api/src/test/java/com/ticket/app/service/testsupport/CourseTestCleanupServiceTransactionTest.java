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
        jdbc.execute("CREATE TABLE `order` (id BIGINT PRIMARY KEY, user_id BIGINT, user_remark VARCHAR(255), order_status INT, ticket_id BIGINT, session_id BIGINT)");
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
        assertEquals(10, scalar("SELECT available_stock FROM ticket_price_level WHERE id=41"));
        assertEquals(10, scalar("SELECT available_capacity FROM ticket_session WHERE id=21"));
        assertEquals(10, scalar("SELECT remain_count FROM coupon WHERE id=91"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM `order`"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM user_coupon"));
    }

    private void seedOrder(Long id, Long userId, int status, Long ticketId, Long sessionId) {
        jdbc.update("INSERT INTO `order` VALUES (?, ?, ?, ?, ?, ?)", id, userId,
                "pw-course-a1b2c3d4-order", status, ticketId, sessionId);
    }

    private CourseCleanupRequest request(List<Long> orders, List<Long> users) {
        return new CourseCleanupRequest("a1b2c3d4", users, orders, List.of(), List.of());
    }

    private int scalar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
