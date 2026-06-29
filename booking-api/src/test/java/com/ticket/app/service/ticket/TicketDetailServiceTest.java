package com.ticket.app.service.ticket;

import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketDetailServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketPriceLevelRepository ticketPriceLevelRepository;

    @Mock
    private TicketSessionRepository ticketSessionRepository;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketRepository, ticketPriceLevelRepository, ticketSessionRepository);
    }

    @Test
    void ticketDetailShouldIncludeSaleSessionsForCheckout() {
        when(ticketRepository.selectTicketDetailById(1L)).thenReturn(createTicket());
        when(ticketPriceLevelRepository.selectByTicketId(1L)).thenReturn(List.of(createPriceLevel()));
        when(ticketSessionRepository.selectByTicketId(1L)).thenReturn(List.of(createSession()));

        TicketServiceImpl.TicketDetailVO detail = (TicketServiceImpl.TicketDetailVO) ticketService.getTicketDetail(1L);

        assertNotNull(detail);
        assertEquals(1, detail.getSessions().size());
        assertEquals(10L, detail.getSessions().get(0).getId());
        assertEquals("2026-06-01", detail.getSessions().get(0).getShowDate());
        assertEquals("19:30", detail.getSessions().get(0).getShowTime());
        assertEquals(20, detail.getSessions().get(0).getAvailableCapacity());
    }

    private Ticket createTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setName("Test Show");
        ticket.setVenueId(2L);
        ticket.setVenueName("Main Hall");
        ticket.setShowStartDate(LocalDate.of(2026, 6, 1));
        ticket.setShowStartTime(LocalTime.of(19, 30));
        ticket.setStatus(Ticket.STATUS_ON_SALE);
        return ticket;
    }

    private TicketPriceLevel createPriceLevel() {
        TicketPriceLevel priceLevel = new TicketPriceLevel();
        priceLevel.setId(100L);
        priceLevel.setTicketId(1L);
        priceLevel.setName("VIP");
        priceLevel.setPrice(new BigDecimal("199.00"));
        priceLevel.setAvailableStock(20);
        return priceLevel;
    }

    private TicketSession createSession() {
        TicketSession session = new TicketSession();
        session.setId(10L);
        session.setTicketId(1L);
        session.setSessionName("Night Show");
        session.setShowDate(LocalDate.of(2026, 6, 1));
        session.setShowStartTime(LocalTime.of(19, 30));
        session.setShowEndTime(LocalTime.of(21, 30));
        session.setAvailableCapacity(20);
        session.setStatus(TicketSession.STATUS_ON_SALE);
        return session;
    }
}
