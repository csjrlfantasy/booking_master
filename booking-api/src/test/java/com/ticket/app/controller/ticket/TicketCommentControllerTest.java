package com.ticket.app.controller.ticket;

import com.ticket.app.common.Result;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.config.interceptor.JwtAuthInterceptor;
import com.ticket.app.controller.ticket.dto.CreateTicketCommentRequest;
import com.ticket.app.controller.ticket.dto.TicketCommentResponse;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketComment;
import com.ticket.app.repository.ticket.TicketCommentRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketCommentControllerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private HttpServletRequest httpRequest;

    private TicketCommentController controller;

    @BeforeEach
    void setUp() {
        controller = new TicketCommentController(ticketRepository, ticketCommentRepository);
    }

    @Test
    void listShouldReturnCommentSummary() {
        TicketComment comment = new TicketComment();
        comment.setId(1L);
        comment.setTicketId(8L);
        comment.setUsername("student01");
        comment.setRating(5);
        comment.setContent("出票很顺畅");
        comment.setCreatedAt(LocalDateTime.now());

        when(ticketCommentRepository.selectVisibleByTicketId(8L, 10)).thenReturn(List.of(comment));
        when(ticketCommentRepository.countVisibleByTicketId(8L)).thenReturn(1);
        when(ticketCommentRepository.averageRatingByTicketId(8L)).thenReturn(new BigDecimal("5.0"));

        Result<TicketCommentResponse.Summary> result = controller.list(8L, 10);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getCount());
        assertEquals(1, result.getData().getList().size());
        assertEquals(5, result.getData().getList().get(0).getRating());
    }

    @Test
    void createShouldRequireLoggedInUser() {
        CreateTicketCommentRequest request = new CreateTicketCommentRequest();
        request.setRating(5);
        request.setContent("很棒");

        assertThrows(BusinessException.class, () -> controller.create(8L, request, httpRequest));
    }

    @Test
    void createShouldPersistComment() {
        CreateTicketCommentRequest request = new CreateTicketCommentRequest();
        request.setRating(4);
        request.setContent("座位区域清楚");

        Ticket ticket = new Ticket();
        ticket.setId(8L);

        when(httpRequest.getAttribute(JwtAuthInterceptor.USER_ID_ATTR)).thenReturn(3L);
        when(httpRequest.getAttribute(JwtAuthInterceptor.USERNAME_ATTR)).thenReturn("student01");
        when(ticketRepository.selectById(8L)).thenReturn(ticket);

        Result<TicketCommentResponse> result = controller.create(8L, request, httpRequest);

        assertTrue(result.isSuccess());
        ArgumentCaptor<TicketComment> captor = ArgumentCaptor.forClass(TicketComment.class);
        verify(ticketCommentRepository).insert(captor.capture());
        assertEquals(8L, captor.getValue().getTicketId());
        assertEquals(3L, captor.getValue().getUserId());
        assertEquals(4, captor.getValue().getRating());
        assertEquals("座位区域清楚", captor.getValue().getContent());
    }
}
