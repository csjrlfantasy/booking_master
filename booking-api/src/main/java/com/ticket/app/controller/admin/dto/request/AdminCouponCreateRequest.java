package com.ticket.app.controller.admin.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminCouponCreateRequest {

    private String name;
    private Integer couponType;
    private BigDecimal couponValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer totalCount;
    private Integer remainCount;
    private Integer perLimit;
    private Integer receiveType;
    private Integer validityType;
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    private Integer validDays;
    private Integer applicableScope;
    private String applicableTickets;
    private String applicableCategories;
    private String description;
    private String notice;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private Integer status;
}
