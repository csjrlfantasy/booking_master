package com.ticket.app.controller.ticket;

import com.ticket.app.common.Result;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.config.interceptor.JwtAuthInterceptor;
import com.ticket.app.controller.ticket.dto.TicketReminderResponse;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketReminder;
import com.ticket.app.repository.ticket.TicketReminderRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketReminderControllerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketReminderRepository reminderRepository;

    @Mock
    private HttpServletRequest httpRequest;

    private TicketReminderController controller;

    @BeforeEach
    void setUp() {
        controller = new TicketReminderController(ticketRepository, reminderRepository);
    }

    @Test
    void remindShouldRequireLogin() {
        assertThrows(BusinessException.class, () -> controller.remind(8L, httpRequest));
    }

    @Test
    void remindShouldCreateReminderForUpcomingTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(8L);
        ticket.setStatus(Ticket.STATUS_PENDING);

        when(httpRequest.getAttribute(JwtAuthInterceptor.USER_ID_ATTR)).thenReturn(3L);
        when(ticketRepository.selectById(8L)).thenReturn(ticket);
        when(reminderRepository.selectByTicketIdAndUserId(8L, 3L)).thenReturn(null);

        Result<TicketReminderResponse> result = controller.remind(8L, httpRequest);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isReminded());
        ArgumentCaptor<TicketReminder> captor = ArgumentCaptor.forClass(TicketReminder.class);
        verify(reminderRepository).insert(captor.capture());
        assertEquals(8L, captor.getValue().getTicketId());
        assertEquals(3L, captor.getValue().getUserId());
    }

    @Test
    void remindShouldNotDuplicateExistingReminder() {
        Ticket ticket = new Ticket();
        ticket.setId(8L);
        ticket.setStatus(Ticket.STATUS_PENDING);
        TicketReminder existing = new TicketReminder();
        existing.setTicketId(8L);
        existing.setUserId(3L);

        when(httpRequest.getAttribute(JwtAuthInterceptor.USER_ID_ATTR)).thenReturn(3L);
        when(ticketRepository.selectById(8L)).thenReturn(ticket);
        when(reminderRepository.selectByTicketIdAndUserId(8L, 3L)).thenReturn(existing);

        Result<TicketReminderResponse> result = controller.remind(8L, httpRequest);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isReminded());
        verify(reminderRepository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mineShouldReturnReminderState() {
        TicketReminder existing = new TicketReminder();
        existing.setTicketId(8L);
        existing.setUserId(3L);

        when(httpRequest.getAttribute(JwtAuthInterceptor.USER_ID_ATTR)).thenReturn(3L);
        when(reminderRepository.selectByTicketIdAndUserId(8L, 3L)).thenReturn(existing);

        Result<TicketReminderResponse> result = controller.mine(8L, httpRequest);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isReminded());
    }
}
