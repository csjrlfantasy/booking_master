package com.ticket.app.controller.admin.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminFlashSaleRequest {
    private Long ticketId;
    private BigDecimal flashPrice;
    private Integer flashStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
