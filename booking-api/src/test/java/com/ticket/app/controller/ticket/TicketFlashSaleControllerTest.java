package com.ticket.app.controller.ticket;

import com.ticket.app.common.Result;
import com.ticket.app.controller.ticket.dto.TicketFlashSaleResponse;
import com.ticket.app.repository.ticket.TicketFlashSaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketFlashSaleControllerTest {

    @Mock
    private TicketFlashSaleRepository flashSaleRepository;

    private TicketFlashSaleController controller;

    @BeforeEach
    void setUp() {
        controller = new TicketFlashSaleController(flashSaleRepository);
    }

    @Test
    void listShouldReturnActiveFlashSaleTickets() {
        TicketFlashSaleResponse item = new TicketFlashSaleResponse();
        item.setFlashSaleId(12L);
        item.setId(9L);
        item.setTitle("2026 AI 测试大会");
        item.setFlashPrice(new BigDecimal("99.00"));
        item.setPrice(new BigDecimal("99.00"));
        item.setOriginalPrice(new BigDecimal("299.00"));
        item.setRemainingStock(20);

        when(flashSaleRepository.selectActiveFlashSaleTickets(2)).thenReturn(List.of(item));

        Result<List<TicketFlashSaleResponse>> result = controller.list(2);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("2026 AI 测试大会", result.getData().get(0).getTitle());
        assertEquals(new BigDecimal("99.00"), result.getData().get(0).getPrice());
        assertEquals(20, result.getData().get(0).getRemainingStock());
    }
}
