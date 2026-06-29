package com.ticket.app.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoTicketDataInitializer implements CommandLineRunner {

    private final TicketRepository ticketRepository;
    private final TicketPriceLevelRepository ticketPriceLevelRepository;
    private final TicketSessionRepository ticketSessionRepository;

    @Override
    public void run(String... args) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getStatus, Ticket.STATUS_ON_SALE);
        List<Ticket> tickets = ticketRepository.selectList(wrapper);
        for (Ticket ticket : tickets) {
            ensurePriceLevel(ticket);
            ensureSession(ticket);
        }
    }

    private void ensurePriceLevel(Ticket ticket) {
        List<TicketPriceLevel> existingLevels = ticketPriceLevelRepository.selectByTicketId(ticket.getId());
        if (existingLevels != null && !existingLevels.isEmpty()) {
            return;
        }

        int stock = safeStock(ticket);
        TicketPriceLevel level = new TicketPriceLevel();
        level.setTicketId(ticket.getId());
        level.setName("标准票");
        level.setPrice(resolvePrice(ticket));
        level.setTotalStock(stock);
        level.setAvailableStock(stock);
        level.setSoldCount(0);
        level.setSeatType(TicketPriceLevel.SEAT_TYPE_FREE);
        level.setDescription("演示票档");
        level.setSortOrder(1);
        level.setStatus(1);
        ticketPriceLevelRepository.insert(level);
        log.info("已补充演示票档: ticketId={}, price={}", ticket.getId(), level.getPrice());
    }

    private void ensureSession(Ticket ticket) {
        List<TicketSession> existingSessions = ticketSessionRepository.selectByTicketId(ticket.getId());
        if (existingSessions != null && !existingSessions.isEmpty()) {
            return;
        }

        LocalDate showDate = ticket.getShowStartDate() != null ? ticket.getShowStartDate() : LocalDate.now().plusDays(7);
        LocalTime showTime = ticket.getShowStartTime() != null ? ticket.getShowStartTime() : LocalTime.of(19, 30);
        int stock = safeStock(ticket);

        TicketSession session = new TicketSession();
        session.setTicketId(ticket.getId());
        session.setSessionNo("DEMO-" + ticket.getId());
        session.setSessionName("默认场次");
        session.setShowDate(showDate);
        session.setShowStartTime(showTime);
        session.setShowEndTime(showTime.plusHours(2));
        session.setWeekDay(resolveWeekDay(showDate));
        session.setTotalCapacity(stock);
        session.setAvailableCapacity(stock);
        session.setStatus(TicketSession.STATUS_ON_SALE);
        ticketSessionRepository.insert(session);
        log.info("已补充演示场次: ticketId={}, showDate={}, showTime={}", ticket.getId(), showDate, showTime);
    }

    private BigDecimal resolvePrice(Ticket ticket) {
        if (ticket.getMinimumPrice() != null && ticket.getMinimumPrice().compareTo(BigDecimal.ZERO) > 0) {
            return ticket.getMinimumPrice();
        }
        if (ticket.getMaximumPrice() != null && ticket.getMaximumPrice().compareTo(BigDecimal.ZERO) > 0) {
            return ticket.getMaximumPrice();
        }
        return new BigDecimal("99.00");
    }

    private int safeStock(Ticket ticket) {
        if (ticket.getAvailableStock() != null && ticket.getAvailableStock() > 0) {
            return ticket.getAvailableStock();
        }
        if (ticket.getTotalStock() != null && ticket.getTotalStock() > 0) {
            return ticket.getTotalStock();
        }
        return 100;
    }

    private String resolveWeekDay(LocalDate date) {
        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekDays[date.getDayOfWeek().getValue() - 1];
    }
}
