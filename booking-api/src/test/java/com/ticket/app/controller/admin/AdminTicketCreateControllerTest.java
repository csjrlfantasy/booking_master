package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminCreateTicketRequest;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import com.ticket.app.service.ticket.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTicketCreateControllerTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketPriceLevelRepository priceLevelRepository;

    @Mock
    private TicketSessionRepository sessionRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpRequest;

    private AdminTicketController adminTicketController;

    @BeforeEach
    void setUp() {
        adminTicketController = new AdminTicketController(
                ticketService,
                ticketRepository,
                priceLevelRepository,
                sessionRepository,
                jwtUtils
        );
    }

    @Test
    void createTicketShouldPersistTicketWithDefaultPriceLevelAndSession() {
        AdminCreateTicketRequest request = new AdminCreateTicketRequest();
        request.setName("AI测试公开课");
        request.setSubtitle("LangChain 与 LangGraph 实战");
        request.setCategoryId(1);
        request.setVenueName("线上直播间");
        request.setShowDate("2026-06-20");
        request.setShowTime("19:30");
        request.setPriceMin(new BigDecimal("99.00"));
        request.setPriceMax(new BigDecimal("199.00"));
        request.setTotalStock(80);
        request.setDescription("一节面向测试学员的 AI 自动化测试课");
        request.setNotice("请提前 10 分钟入场");

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);
        when(ticketRepository.insert(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(9001L);
            return 1;
        });

        Result<Long> result = adminTicketController.createTicket(request, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals(9001L, result.getData());

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).insert(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertEquals("AI测试公开课", savedTicket.getName());
        assertEquals("LangChain 与 LangGraph 实战", savedTicket.getSubName());
        assertEquals("线上直播间", savedTicket.getVenueName());
        assertEquals(new BigDecimal("99.00"), savedTicket.getMinimumPrice());
        assertEquals(new BigDecimal("199.00"), savedTicket.getMaximumPrice());
        assertEquals(80, savedTicket.getTotalStock());
        assertEquals("[]", savedTicket.getHighlights());
        assertEquals("[]", savedTicket.getTags());
        assertEquals("{}", savedTicket.getExtInfo());
        assertEquals(Ticket.STATUS_ON_SALE, savedTicket.getStatus());

        ArgumentCaptor<TicketPriceLevel> priceCaptor = ArgumentCaptor.forClass(TicketPriceLevel.class);
        verify(priceLevelRepository).insert(priceCaptor.capture());
        TicketPriceLevel priceLevel = priceCaptor.getValue();
        assertEquals(9001L, priceLevel.getTicketId());
        assertEquals(new BigDecimal("99.00"), priceLevel.getPrice());
        assertEquals(80, priceLevel.getAvailableStock());
        assertEquals(TicketPriceLevel.SEAT_TYPE_FREE, priceLevel.getSeatType());
        assertEquals("[]", priceLevel.getBenefits());

        ArgumentCaptor<TicketSession> sessionCaptor = ArgumentCaptor.forClass(TicketSession.class);
        verify(sessionRepository).insert(sessionCaptor.capture());
        TicketSession session = sessionCaptor.getValue();
        assertEquals(9001L, session.getTicketId());
        assertEquals(LocalDate.of(2026, 6, 20), session.getShowDate());
        assertEquals(LocalTime.of(19, 30), session.getShowStartTime());
        assertEquals(80, session.getAvailableCapacity());
        assertEquals(TicketSession.STATUS_ON_SALE, session.getStatus());
    }
}
