package com.ticket.app.repository.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.TicketReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TicketReminderRepository extends BaseMapper<TicketReminder> {

    @Select("""
            SELECT id, ticket_id, user_id, status, created_at, updated_at
            FROM ticket_reminder
            WHERE ticket_id = #{ticketId}
              AND user_id = #{userId}
              AND status = 1
            LIMIT 1
            """)
    TicketReminder selectByTicketIdAndUserId(@Param("ticketId") Long ticketId, @Param("userId") Long userId);
}
