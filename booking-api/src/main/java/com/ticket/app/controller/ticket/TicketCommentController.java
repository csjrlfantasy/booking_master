package com.ticket.app.controller.ticket;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.config.interceptor.JwtAuthInterceptor;
import com.ticket.app.controller.ticket.dto.CreateTicketCommentRequest;
import com.ticket.app.controller.ticket.dto.TicketCommentResponse;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketComment;
import com.ticket.app.repository.ticket.TicketCommentRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class TicketCommentController {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;

    @GetMapping
    public Result<TicketCommentResponse.Summary> list(
            @PathVariable Long ticketId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        int safeLimit = limit == null ? 10 : Math.max(1, Math.min(limit, 50));
        TicketCommentResponse.Summary summary = new TicketCommentResponse.Summary();
        List<TicketCommentResponse> comments = ticketCommentRepository.selectVisibleByTicketId(ticketId, safeLimit)
                .stream()
                .map(TicketCommentResponse::fromEntity)
                .toList();
        BigDecimal averageRating = ticketCommentRepository.averageRatingByTicketId(ticketId);
        summary.setCount(ticketCommentRepository.countVisibleByTicketId(ticketId));
        summary.setAverageRating(averageRating != null ? averageRating : BigDecimal.ZERO);
        summary.setList(comments);
        return Result.success(summary);
    }

    @PostMapping
    public Result<TicketCommentResponse> create(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateTicketCommentRequest request,
            HttpServletRequest httpRequest) {
        Object userIdAttr = httpRequest.getAttribute(JwtAuthInterceptor.USER_ID_ATTR);
        Object usernameAttr = httpRequest.getAttribute(JwtAuthInterceptor.USERNAME_ATTR);
        if (!(userIdAttr instanceof Long userId)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "请先登录后再评论");
        }

        Ticket ticket = ticketRepository.selectById(ticketId);
        if (ticket == null || ticket.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "票务不存在");
        }

        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setUserId(userId);
        comment.setUsername(usernameAttr != null ? String.valueOf(usernameAttr) : "票多星用户");
        comment.setRating(request.getRating());
        comment.setContent(request.getContent().trim());
        comment.setStatus(1);
        ticketCommentRepository.insert(comment);
        return Result.success(TicketCommentResponse.fromEntity(comment));
    }
}
