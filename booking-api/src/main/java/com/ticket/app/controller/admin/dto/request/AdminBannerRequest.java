package com.ticket.app.controller.admin.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBannerRequest {

    private String title;
    private String imageUrl;
    private String linkType;
    private String linkTarget;
    private String platform;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
