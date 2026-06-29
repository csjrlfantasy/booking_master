package com.ticket.app.controller.banner.dto;

import com.ticket.app.entity.Banner;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannerResponse {

    private Long id;
    private String title;
    private String imageUrl;
    private String linkType;
    private String linkTarget;
    private String platform;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public static BannerResponse from(Banner banner) {
        BannerResponse response = new BannerResponse();
        response.setId(banner.getId());
        response.setTitle(banner.getTitle());
        response.setImageUrl(banner.getImageUrl());
        response.setLinkType(banner.getLinkType());
        response.setLinkTarget(banner.getLinkTarget());
        response.setPlatform(banner.getPlatform());
        response.setSortOrder(banner.getSortOrder());
        response.setStatus(banner.getStatus());
        response.setStartTime(banner.getStartTime());
        response.setEndTime(banner.getEndTime());
        return response;
    }
}
