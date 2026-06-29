package com.ticket.app.controller.ticket.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketFlashSaleResponse {
    private Long flashSaleId;
    private Long id;
    private Long ticketId;
    private String title;
    private BigDecimal price;
    private BigDecimal flashPrice;
    private BigDecimal originalPrice;
    private String venue;
    private String date;
    private String category;
    private String imageUrl;
    private Integer status;
    private String statusName;
    private Integer flashStock;
    private Integer soldCount;
    private Integer remainingStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
