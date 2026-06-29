package com.ticket.app.controller.admin.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AdminCreateTicketRequest {

    private String name;

    private String subtitle;

    private String subName;

    private Integer categoryId;

    private String categoryName;

    private Long venueId;

    private String venueName;

    private String poster;

    private String showDate;

    private String showTime;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    private Integer totalStock;

    private Integer status;

    private List<PriceLevelRequest> priceLevels;

    private String description;

    private String notice;

    @Data
    public static class PriceLevelRequest {
        private String name;

        private BigDecimal price;

        private Integer totalStock;

        private String description;
    }
}
