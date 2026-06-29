package com.ticket.app.controller.ticket.dto;

import com.ticket.app.entity.TicketComment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketCommentResponse {

    private Long id;

    private Long ticketId;

    private String username;

    private Integer rating;

    private String content;

    private LocalDateTime createdAt;

    public static TicketCommentResponse fromEntity(TicketComment comment) {
        TicketCommentResponse response = new TicketCommentResponse();
        response.setId(comment.getId());
        response.setTicketId(comment.getTicketId());
        response.setUsername(maskUsername(comment.getUsername()));
        response.setRating(comment.getRating());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }

    private static String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "票多星用户";
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }

    @Data
    public static class Summary {
        private Integer count;
        private BigDecimal averageRating;
        private List<TicketCommentResponse> list;
    }
}
