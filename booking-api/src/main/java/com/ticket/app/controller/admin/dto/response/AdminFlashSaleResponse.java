package com.ticket.app.controller.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminFlashSaleResponse {
    private Long id;
    private Long ticketId;
    private String ticketName;
    private BigDecimal flashPrice;
    private BigDecimal originalPrice;
    private Integer flashStock;
    private Integer soldCount;
    private Integer remainingStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
