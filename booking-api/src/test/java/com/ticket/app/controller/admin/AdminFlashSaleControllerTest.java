package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminFlashSaleRequest;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketFlashSale;
import com.ticket.app.repository.ticket.TicketFlashSaleRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFlashSaleControllerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketFlashSaleRepository flashSaleRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpRequest;

    private AdminFlashSaleController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminFlashSaleController(ticketRepository, flashSaleRepository, jwtUtils);
    }

    @Test
    void createShouldPersistFlashSaleForTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(8L);
        ticket.setStatus(Ticket.STATUS_ON_SALE);

        AdminFlashSaleRequest request = new AdminFlashSaleRequest();
        request.setTicketId(8L);
        request.setFlashPrice(new BigDecimal("88.00"));
        request.setFlashStock(50);
        request.setStartTime(LocalDateTime.now().minusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(5));
        request.setStatus(TicketFlashSale.STATUS_ENABLED);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);
        when(ticketRepository.selectById(8L)).thenReturn(ticket);

        Result<Long> result = controller.create(request, httpRequest);

        assertTrue(result.isSuccess());
        ArgumentCaptor<TicketFlashSale> captor = ArgumentCaptor.forClass(TicketFlashSale.class);
        verify(flashSaleRepository).insert(captor.capture());
        assertEquals(8L, captor.getValue().getTicketId());
        assertEquals(new BigDecimal("88.00"), captor.getValue().getFlashPrice());
        assertEquals(50, captor.getValue().getFlashStock());
        assertEquals(TicketFlashSale.STATUS_ENABLED, captor.getValue().getStatus());
    }

    @Test
    void createShouldRejectMissingTicket() {
        AdminFlashSaleRequest request = new AdminFlashSaleRequest();
        request.setTicketId(404L);
        request.setFlashPrice(new BigDecimal("88.00"));
        request.setFlashStock(50);
        request.setStartTime(LocalDateTime.now().minusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(5));

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);
        when(ticketRepository.selectById(404L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.create(request, httpRequest));
    }
}
