package com.ticket.app.controller.ticket;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.config.interceptor.JwtAuthInterceptor;
import com.ticket.app.controller.ticket.dto.TicketReminderResponse;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketReminder;
import com.ticket.app.repository.ticket.TicketReminderRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets/{ticketId}/reminders")
@RequiredArgsConstructor
public class TicketReminderController {

    private final TicketRepository ticketRepository;
    private final TicketReminderRepository reminderRepository;

    @GetMapping("/mine")
    public Result<TicketReminderResponse> mine(@PathVariable Long ticketId, HttpServletRequest request) {
        Long userId = requireUserId(request);
        TicketReminder reminder = reminderRepository.selectByTicketIdAndUserId(ticketId, userId);
        return Result.success(new TicketReminderResponse(reminder != null));
    }

    @PostMapping
    public Result<TicketReminderResponse> remind(@PathVariable Long ticketId, HttpServletRequest request) {
        Long userId = requireUserId(request);
        Ticket ticket = ticketRepository.selectById(ticketId);
        if (ticket == null || ticket.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "票务不存在");
        }
        if (!Ticket.STATUS_PENDING.equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有即将开售的票务可以设置提醒");
        }

        TicketReminder existing = reminderRepository.selectByTicketIdAndUserId(ticketId, userId);
        if (existing == null) {
            TicketReminder reminder = new TicketReminder();
            reminder.setTicketId(ticketId);
            reminder.setUserId(userId);
            reminder.setStatus(1);
            reminderRepository.insert(reminder);
        }
        return Result.success(new TicketReminderResponse(true));
    }

    private Long requireUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute(JwtAuthInterceptor.USER_ID_ATTR);
        if (userIdAttr instanceof Long userId) {
            return userId;
        }
        throw new BusinessException(ResultCode.TOKEN_INVALID, "请先登录");
    }
}
