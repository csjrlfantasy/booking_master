package com.ticket.app.service.ticket;

import com.ticket.app.entity.Ticket;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpcomingTicketServiceTest {

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
    void upcomingTicketsShouldReturnPendingTickets() {
        Ticket pending = createTicket(10L, "Upcoming Show", Ticket.STATUS_PENDING, LocalDateTime.now().plusDays(2));
        when(ticketRepository.selectUpcomingTickets(6)).thenReturn(List.of(pending));

        List<?> result = ticketService.getUpcomingTickets(6);

        assertEquals(1, result.size());
        TicketServiceImpl.TicketListVO item = (TicketServiceImpl.TicketListVO) result.get(0);
        assertEquals(10L, item.getId());
        assertEquals("Upcoming Show", item.getTitle());
        assertEquals(Ticket.STATUS_PENDING, item.getStatus());
    }

    private Ticket createTicket(Long id, String name, Integer status, LocalDateTime bookStartTime) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setName(name);
        ticket.setPoster("/api/uploads/tickets/demo.png");
        ticket.setVenueName("TicketMore Hall");
        ticket.setMinimumPrice(new BigDecimal("180.00"));
        ticket.setShowStartDate(LocalDate.of(2026, 8, 1));
        ticket.setShowStartTime(LocalTime.of(19, 30));
        ticket.setCategoryName("Concert");
        ticket.setStatus(status);
        ticket.setBookStartTime(bookStartTime);
        return ticket;
    }
}
