package com.ticket.app.controller.admin.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AdminCouponIssueRequest {

    private List<Long> userIds;
    private String issueMode;
}
