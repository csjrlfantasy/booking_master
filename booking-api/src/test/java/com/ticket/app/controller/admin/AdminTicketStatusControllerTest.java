package com.ticket.app.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminCreateTicketRequest;
import com.ticket.app.controller.admin.dto.request.AdminUpdateTicketStatusRequest;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTicketStatusControllerTest {

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

    private AdminTicketController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminTicketController(
                ticketService,
                ticketRepository,
                priceLevelRepository,
                sessionRepository,
                jwtUtils
        );
    }

    @Test
    void updateTicketStatusShouldPersistAllowedStatus() {
        Ticket ticket = new Ticket();
        ticket.setId(12L);
        ticket.setStatus(Ticket.STATUS_ON_SALE);

        AdminUpdateTicketStatusRequest request = new AdminUpdateTicketStatusRequest();
        request.setStatus(Ticket.STATUS_OFFLINE);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);
        when(ticketRepository.selectById(12L)).thenReturn(ticket);

        Result<Void> result = controller.updateTicketStatus(12L, request, httpRequest);

        assertTrue(result.isSuccess());
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).updateById(captor.capture());
        assertEquals(Ticket.STATUS_OFFLINE, captor.getValue().getStatus());
    }

    @Test
    void updateTicketStatusShouldRejectUnknownStatus() {
        AdminUpdateTicketStatusRequest request = new AdminUpdateTicketStatusRequest();
        request.setStatus(99);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);

        assertThrows(BusinessException.class, () -> controller.updateTicketStatus(12L, request, httpRequest));
    }

    @Test
    void createTicketShouldUseRequestedSaleStatus() {
        AdminCreateTicketRequest request = new AdminCreateTicketRequest();
        request.setName("Presale Show");
        request.setStatus(Ticket.STATUS_PENDING);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);

        Result<Long> result = controller.createTicket(request, httpRequest);

        assertTrue(result.isSuccess());
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).insert(captor.capture());
        assertEquals(Ticket.STATUS_PENDING, captor.getValue().getStatus());
        assertTrue(captor.getValue().getBookStartTime().isAfter(java.time.LocalDateTime.now()));
    }

    @Test
    void adminTicketListShouldUseAdminQueryAndRespectStatusFilter() {
        Ticket offlineTicket = new Ticket();
        offlineTicket.setId(88L);
        offlineTicket.setName("Offline Show");
        offlineTicket.setCategoryId(3);
        offlineTicket.setCategoryName("Drama");
        offlineTicket.setMinimumPrice(new BigDecimal("180.00"));
        offlineTicket.setMaximumPrice(new BigDecimal("680.00"));
        offlineTicket.setStatus(Ticket.STATUS_OFFLINE);

        Page<Ticket> page = new Page<>(1, 10);
        page.setRecords(List.of(offlineTicket));
        page.setTotal(1);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);
        when(ticketRepository.selectPage(any(Page.class), any())).thenReturn(page);

        Result<PageResult<?>> result = controller.getTicketList(
                1,
                10,
                null,
                null,
                Ticket.STATUS_OFFLINE,
                null,
                httpRequest
        );

        verify(ticketService, never()).getTicketList(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getList().size());
        Ticket responseTicket = (Ticket) result.getData().getList().get(0);
        assertEquals(Ticket.STATUS_OFFLINE, responseTicket.getStatus());
        assertEquals("Drama", responseTicket.getCategoryName());
        assertEquals(new BigDecimal("180.00"), responseTicket.getMinimumPrice());
        assertEquals(new BigDecimal("680.00"), responseTicket.getMaximumPrice());
    }

    @Test
    void createTicketShouldCreateRequestedPriceLevels() {
        AdminCreateTicketRequest.PriceLevelRequest vip = new AdminCreateTicketRequest.PriceLevelRequest();
        vip.setName("VIP区");
        vip.setPrice(new BigDecimal("680.00"));
        vip.setTotalStock(20);

        AdminCreateTicketRequest.PriceLevelRequest normal = new AdminCreateTicketRequest.PriceLevelRequest();
        normal.setName("看台区");
        normal.setPrice(new BigDecimal("280.00"));
        normal.setTotalStock(80);

        AdminCreateTicketRequest request = new AdminCreateTicketRequest();
        request.setName("Multi Price Show");
        request.setPriceLevels(List.of(vip, normal));

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);

        controller.createTicket(request, httpRequest);

        ArgumentCaptor<TicketPriceLevel> captor = ArgumentCaptor.forClass(TicketPriceLevel.class);
        verify(priceLevelRepository, times(2)).insert(captor.capture());
        assertEquals("VIP区", captor.getAllValues().get(0).getName());
        assertEquals(new BigDecimal("680.00"), captor.getAllValues().get(0).getPrice());
        assertEquals(20, captor.getAllValues().get(0).getTotalStock());
        assertEquals("看台区", captor.getAllValues().get(1).getName());
    }
}
