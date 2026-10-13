package com.ticket.app.controller.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminCouponListItemResponse {

    private Long id;
    private String name;
    private Integer couponType;
    private String couponTypeText;
    private BigDecimal couponValue;
    private BigDecimal minOrderAmount;
    private Integer totalCount;
    private Integer remainCount;
    private Integer perLimit;
    private Integer receiveType;
    private Integer validityType;
    private Integer applicableScope;
    private Integer status;
    private String statusText;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private LocalDateTime createdAt;
}
