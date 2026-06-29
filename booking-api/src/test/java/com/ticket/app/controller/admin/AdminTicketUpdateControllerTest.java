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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTicketUpdateControllerTest {

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
    void updateTicketShouldPersistTicketPosterPriceLevelAndSession() {
        Ticket existingTicket = new Ticket();
        existingTicket.setId(9L);
        existingTicket.setName("旧票务");
        existingTicket.setSaleCount(5);

        TicketPriceLevel existingPrice = new TicketPriceLevel();
        existingPrice.setId(31L);
        existingPrice.setTicketId(9L);
        existingPrice.setSoldCount(5);

        TicketSession existingSession = new TicketSession();
        existingSession.setId(41L);
        existingSession.setTicketId(9L);

        AdminCreateTicketRequest request = new AdminCreateTicketRequest();
        request.setName("AI测试进阶课");
        request.setSubtitle("编辑保存验证");
        request.setCategoryId(2);
        request.setVenueName("票多星课堂");
        request.setPoster("data:image/png;base64,poster");
        request.setShowDate("2026-07-01");
        request.setShowTime("20:00");
        request.setPriceMin(new BigDecimal("128.00"));
        request.setPriceMax(new BigDecimal("288.00"));
        request.setTotalStock(120);
        request.setDescription("编辑后的详情");
        request.setNotice("编辑后的注意事项");

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);
        when(ticketRepository.selectTicketDetailById(9L)).thenReturn(existingTicket);
        when(priceLevelRepository.selectByTicketId(9L)).thenReturn(List.of(existingPrice));
        when(sessionRepository.selectByTicketId(9L)).thenReturn(List.of(existingSession));

        Result<Void> result = adminTicketController.updateTicket(9L, request, httpRequest);

        assertTrue(result.isSuccess());

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).updateById(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertEquals("AI测试进阶课", savedTicket.getName());
        assertEquals("编辑保存验证", savedTicket.getSubName());
        assertEquals("data:image/png;base64,poster", savedTicket.getPoster());
        assertEquals(new BigDecimal("128.00"), savedTicket.getMinimumPrice());
        assertEquals(new BigDecimal("288.00"), savedTicket.getMaximumPrice());
        assertEquals(120, savedTicket.getTotalStock());
        assertEquals(115, savedTicket.getAvailableStock());

        ArgumentCaptor<TicketPriceLevel> priceCaptor = ArgumentCaptor.forClass(TicketPriceLevel.class);
        verify(priceLevelRepository).updateById(priceCaptor.capture());
        TicketPriceLevel savedPrice = priceCaptor.getValue();
        assertEquals(31L, savedPrice.getId());
        assertEquals(new BigDecimal("128.00"), savedPrice.getPrice());
        assertEquals(120, savedPrice.getTotalStock());
        assertEquals(115, savedPrice.getAvailableStock());

        ArgumentCaptor<TicketSession> sessionCaptor = ArgumentCaptor.forClass(TicketSession.class);
        verify(sessionRepository).updateById(sessionCaptor.capture());
        TicketSession savedSession = sessionCaptor.getValue();
        assertEquals(41L, savedSession.getId());
        assertEquals(LocalDate.of(2026, 7, 1), savedSession.getShowDate());
        assertEquals(LocalTime.of(20, 0), savedSession.getShowStartTime());
        assertEquals(120, savedSession.getTotalCapacity());
        assertEquals(115, savedSession.getAvailableCapacity());
    }
}
