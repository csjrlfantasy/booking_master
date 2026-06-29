package com.ticket.app.controller.admin.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminAdjustBalanceRequest {

    private String channel;

    private BigDecimal amount;
}
