package com.ticket.app.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.controller.admin.dto.request.SalesStatsRequest;
import com.ticket.app.controller.admin.dto.response.OrderStatsResponse;
import com.ticket.app.controller.admin.dto.response.OverviewStatsResponse;
import com.ticket.app.controller.admin.dto.response.SalesStatsResponse;
import com.ticket.app.controller.admin.dto.response.UserStatsResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.User;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private static final BigDecimal ZERO = money(BigDecimal.ZERO);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public OverviewStatsResponse getOverviewStats() {
        List<Order> orders = loadOrders();
        List<User> users = loadUsers();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        OverviewStatsResponse response = new OverviewStatsResponse();
        response.setTodaySales(sumPaidSales(orders, today, today));
        response.setTodayOrders(countOrdersCreated(orders, today, today));
        response.setTodayUsers(countUsersCreated(users, today, today));
        response.setTodayRefunds(countRefunds(orders, today, today));
        response.setYesterdaySales(sumPaidSales(orders, yesterday, yesterday));
        response.setSalesGrowth(growthPercent(response.getTodaySales(), response.getYesterdaySales()));
        response.setPendingRefundCount(countStatus(orders, Order.STATUS_REFUNDING));
        response.setPendingOrderCount(countStatus(orders, Order.STATUS_PENDING_PAY));
        return response;
    }

    @Override
    public SalesStatsResponse getSalesStats(SalesStatsRequest request) {
        List<Order> orders = loadOrders();
        Period period = resolveSalesPeriod(request);

        SalesStatsResponse response = new SalesStatsResponse();
        BigDecimal totalSales = sumPaidSales(orders, period.start, period.end);
        int totalOrders = countPaidOrders(orders, period.start, period.end);
        response.setTotalSales(totalSales);
        response.setTotalOrders(totalOrders);
        response.setAverageOrderAmount(totalOrders == 0 ? ZERO : money(totalSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)));
        response.setTrendList(buildSalesTrend(orders, period));
        return response;
    }

    @Override
    public OrderStatsResponse getOrderStats() {
        List<Order> orders = loadOrders();
        Map<Integer, Long> counts = orders.stream()
                .filter(order -> order.getOrderStatus() != null)
                .collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));

        OrderStatsResponse response = new OrderStatsResponse();
        response.setTotalOrders(orders.size());
        response.setPendingPayOrders(toInt(counts.get(Order.STATUS_PENDING_PAY)));
        response.setPaidOrders(toInt(counts.get(Order.STATUS_PAID)));
        response.setCompletedOrders(toInt(counts.get(Order.STATUS_COMPLETED)));
        response.setCancelledOrders(toInt(counts.get(Order.STATUS_CANCELLED)));
        response.setRefundingOrders(toInt(counts.get(Order.STATUS_REFUNDING)));

        List<OrderStatsResponse.StatusItem> distribution = counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    OrderStatsResponse.StatusItem item = new OrderStatsResponse.StatusItem();
                    item.setStatus(entry.getKey());
                    item.setCount(toInt(entry.getValue()));
                    return item;
                })
                .collect(Collectors.toList());
        response.setOrderStatusDistribution(distribution);
        return response;
    }

    @Override
    public UserStatsResponse getUserStats() {
        List<User> users = loadUsers();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);

        UserStatsResponse response = new UserStatsResponse();
        response.setTotalUsers(users.size());
        response.setActiveUsers(toInt(users.stream().filter(user -> Objects.equals(user.getStatus(), User.STATUS_NORMAL)).count()));
        response.setNewUsersToday(countUsersCreated(users, today, today));
        response.setNewUsersThisWeek(countUsersCreated(users, weekStart, today));
        response.setNewUsersThisMonth(countUsersCreated(users, monthStart, today));
        response.setVipUsers(toInt(users.stream().filter(user -> Objects.equals(user.getUserType(), User.USER_TYPE_VIP)).count()));
        response.setUserGrowthTrend(buildUserGrowthTrend(users, today.minusDays(6), today));
        return response;
    }

    private List<Order> loadOrders() {
        List<Order> orders = orderRepository.selectList(new LambdaQueryWrapper<Order>().isNull(Order::getDeletedAt));
        if (orders == null) {
            return List.of();
        }
        return orders.stream()
                .filter(order -> order.getDeletedAt() == null)
                .collect(Collectors.toList());
    }

    private List<User> loadUsers() {
        List<User> users = userRepository.selectList(new LambdaQueryWrapper<User>().isNull(User::getDeletedAt));
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .filter(user -> user.getDeletedAt() == null)
                .collect(Collectors.toList());
    }

    private BigDecimal sumPaidSales(List<Order> orders, LocalDate start, LocalDate end) {
        return money(orders.stream()
                .filter(order -> Objects.equals(order.getPayStatus(), Order.PAY_STATUS_PAID))
                .filter(order -> isDateInRange(order.getPayTime(), start, end))
                .map(Order::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private int countPaidOrders(List<Order> orders, LocalDate start, LocalDate end) {
        return toInt(orders.stream()
                .filter(order -> Objects.equals(order.getPayStatus(), Order.PAY_STATUS_PAID))
                .filter(order -> isDateInRange(order.getPayTime(), start, end))
                .count());
    }

    private int countOrdersCreated(List<Order> orders, LocalDate start, LocalDate end) {
        return toInt(orders.stream()
                .filter(order -> isDateInRange(order.getCreatedAt(), start, end))
                .count());
    }

    private int countUsersCreated(List<User> users, LocalDate start, LocalDate end) {
        return toInt(users.stream()
                .filter(user -> isDateInRange(user.getCreatedAt(), start, end))
                .count());
    }

    private int countRefunds(List<Order> orders, LocalDate start, LocalDate end) {
        return toInt(orders.stream()
                .filter(order -> Objects.equals(order.getOrderStatus(), Order.STATUS_REFUNDED)
                        || Objects.equals(order.getOrderStatus(), Order.STATUS_REFUNDING))
                .filter(order -> isDateInRange(refundDate(order), start, end))
                .count());
    }

    private int countStatus(List<Order> orders, Integer status) {
        return toInt(orders.stream().filter(order -> Objects.equals(order.getOrderStatus(), status)).count());
    }

    private List<SalesStatsResponse.TrendItem> buildSalesTrend(List<Order> orders, Period period) {
        List<SalesStatsResponse.TrendItem> trend = new ArrayList<>();
        LocalDate cursor = period.start;
        while (!cursor.isAfter(period.end)) {
            LocalDate bucketStart = cursor;
            LocalDate bucketEnd = switch (period.type) {
                case "month" -> cursor.withDayOfMonth(cursor.lengthOfMonth());
                case "week" -> cursor.plusDays(6);
                default -> cursor;
            };
            if (bucketEnd.isAfter(period.end)) {
                bucketEnd = period.end;
            }

            SalesStatsResponse.TrendItem item = new SalesStatsResponse.TrendItem();
            item.setDate(label(period.type, bucketStart));
            item.setSales(sumPaidSales(orders, bucketStart, bucketEnd));
            item.setOrderCount(countPaidOrders(orders, bucketStart, bucketEnd));
            item.setRefundCount(countRefunds(orders, bucketStart, bucketEnd));
            item.setRefundAmount(sumRefundAmount(orders, bucketStart, bucketEnd));
            trend.add(item);

            cursor = switch (period.type) {
                case "month" -> cursor.plusMonths(1).withDayOfMonth(1);
                case "week" -> cursor.plusWeeks(1);
                default -> cursor.plusDays(1);
            };
        }
        return trend;
    }

    private List<UserStatsResponse.TrendItem> buildUserGrowthTrend(List<User> users, LocalDate start, LocalDate end) {
        List<UserStatsResponse.TrendItem> trend = new ArrayList<>();
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
            UserStatsResponse.TrendItem item = new UserStatsResponse.TrendItem();
            item.setDate(cursor.toString());
            item.setCount(countUsersCreated(users, cursor, cursor));
            trend.add(item);
        }
        return trend;
    }

    private BigDecimal sumRefundAmount(List<Order> orders, LocalDate start, LocalDate end) {
        return money(orders.stream()
                .filter(order -> Objects.equals(order.getOrderStatus(), Order.STATUS_REFUNDED)
                        || Objects.equals(order.getOrderStatus(), Order.STATUS_REFUNDING)
                        || Objects.equals(order.getPayStatus(), Order.PAY_STATUS_REFUNDED))
                .filter(order -> isDateInRange(refundDate(order), start, end))
                .map(Order::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Period resolveSalesPeriod(SalesStatsRequest request) {
        String type = request == null || request.getType() == null ? "day" : request.getType();
        LocalDate today = LocalDate.now();
        LocalDate end = request != null && request.getEndDate() != null ? request.getEndDate() : today;
        LocalDate start;
        if (request != null && request.getStartDate() != null) {
            start = request.getStartDate();
            type = "day";
        } else if ("month".equals(type)) {
            start = end.minusMonths(5).withDayOfMonth(1);
        } else if ("week".equals(type)) {
            start = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(6);
        } else {
            type = "day";
            start = end.minusDays(6);
        }
        if (start.isAfter(end)) {
            start = end;
        }
        if ("month".equals(type)) {
            start = start.withDayOfMonth(1);
        }
        return new Period(type, start, end);
    }

    private LocalDateTime refundDate(Order order) {
        if (order.getUpdatedAt() != null) {
            return order.getUpdatedAt();
        }
        if (order.getCancelTime() != null) {
            return order.getCancelTime();
        }
        return order.getCreatedAt();
    }

    private boolean isDateInRange(LocalDateTime value, LocalDate start, LocalDate end) {
        if (value == null || start == null || end == null) {
            return false;
        }
        LocalDate date = value.toLocalDate();
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private BigDecimal growthPercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100.00") : ZERO;
        }
        return current.subtract(previous)
                .multiply(new BigDecimal("100"))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private String label(String type, LocalDate date) {
        if ("month".equals(type)) {
            return date.toString().substring(0, 7);
        }
        return date.toString();
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static int toInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }

    private record Period(String type, LocalDate start, LocalDate end) {
    }
}
