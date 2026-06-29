package com.ticket.app.controller.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 票务列表响应
 */
@Schema(description = "票务列表响应")
@Data
public class TicketListResponse {

    @Schema(description = "票务ID")
    private Long id;

    @Schema(description = "票务名称")
    private String name;

    @Schema(description = "票务描述")
    private String description;

    @Schema(description = "场馆名称")
    private String venue;

    @Schema(description = "演出时间")
    private LocalDateTime eventTime;

    @Schema(description = "状态 0:下架 1:上架")
    private Integer status;

    @Schema(description = "总票数")
    private Integer totalTickets;

    @Schema(description = "已售票数")
    private Integer soldTickets;

    @Schema(description = "剩余票数")
    private Integer remainingTickets;

    @Schema(description = "最低价格")
    private Double minPrice;

    @Schema(description = "最高价格")
    private Double maxPrice;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "是否热门 0:否 1:是")
    private Integer isHot;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static TicketListResponse fromEntity(com.ticket.app.entity.Ticket ticket) {
        TicketListResponse response = new TicketListResponse();
        response.setId(ticket.getId());
        response.setName(ticket.getName());
        response.setDescription(ticket.getDescription());
        response.setVenue(ticket.getVenueName());
        // 使用演出开始日期时间
        if (ticket.getShowStartDate() != null) {
            response.setEventTime(ticket.getShowStartDate().atTime(ticket.getShowStartTime() != null ? ticket.getShowStartTime() : java.time.LocalTime.NOON));
        }
        response.setStatus(ticket.getStatus());
        response.setTotalTickets(ticket.getTotalStock());
        response.setSoldTickets(ticket.getSaleCount());
        response.setRemainingTickets(ticket.getAvailableStock());
        response.setMinPrice(ticket.getMinimumPrice() != null ? ticket.getMinimumPrice().doubleValue() : null);
        response.setMaxPrice(ticket.getMaximumPrice() != null ? ticket.getMaximumPrice().doubleValue() : null);
        response.setCategoryId(ticket.getCategoryId() != null ? ticket.getCategoryId().longValue() : null);
        response.setCategoryName(ticket.getCategoryName());
        response.setImageUrl(ticket.getPoster());
        response.setIsHot(ticket.getIsHot());
        response.setCreatedAt(ticket.getCreatedAt());
        return response;
    }
}