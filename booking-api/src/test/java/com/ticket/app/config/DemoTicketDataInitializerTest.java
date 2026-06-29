package com.ticket.app.config;

import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoTicketDataInitializerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketPriceLevelRepository ticketPriceLevelRepository;

    @Mock
    private TicketSessionRepository ticketSessionRepository;

    private DemoTicketDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DemoTicketDataInitializer(ticketRepository, ticketPriceLevelRepository, ticketSessionRepository);
    }

    @Test
    void shouldCreateMissingDemoPriceLevelAndSessionForOnSaleTicket() {
        Ticket ticket = createTicket();
        when(ticketRepository.selectList(any())).thenReturn(List.of(ticket));
        when(ticketPriceLevelRepository.selectByTicketId(7L)).thenReturn(List.of());
        when(ticketSessionRepository.selectByTicketId(7L)).thenReturn(List.of());

        initializer.run();

        verify(ticketPriceLevelRepository).insert(any(TicketPriceLevel.class));
        ArgumentCaptor<TicketSession> sessionCaptor = ArgumentCaptor.forClass(TicketSession.class);
        verify(ticketSessionRepository).insert(sessionCaptor.capture());
        assertEquals("周一", sessionCaptor.getValue().getWeekDay());
    }

    @Test
    void shouldNotDuplicateExistingTicketSalesData() {
        Ticket ticket = createTicket();
        when(ticketRepository.selectList(any())).thenReturn(List.of(ticket));
        when(ticketPriceLevelRepository.selectByTicketId(7L)).thenReturn(List.of(new TicketPriceLevel()));
        when(ticketSessionRepository.selectByTicketId(7L)).thenReturn(List.of(new TicketSession()));

        initializer.run();

        verify(ticketPriceLevelRepository, never()).insert(any(TicketPriceLevel.class));
        verify(ticketSessionRepository, never()).insert(any(TicketSession.class));
    }

    private Ticket createTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(7L);
        ticket.setName("Demo Show");
        ticket.setStatus(Ticket.STATUS_ON_SALE);
        ticket.setMinimumPrice(new BigDecimal("180.00"));
        ticket.setMaximumPrice(new BigDecimal("880.00"));
        ticket.setTotalStock(3000);
        ticket.setAvailableStock(2800);
        ticket.setShowStartDate(LocalDate.of(2026, 6, 15));
        ticket.setShowStartTime(LocalTime.of(19, 30));
        return ticket;
    }
}
