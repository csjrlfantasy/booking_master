package com.ticket.app.repository.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.TicketComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TicketCommentRepository extends BaseMapper<TicketComment> {

    List<TicketComment> selectVisibleByTicketId(@Param("ticketId") Long ticketId, @Param("limit") Integer limit);

    int countVisibleByTicketId(@Param("ticketId") Long ticketId);

    BigDecimal averageRatingByTicketId(@Param("ticketId") Long ticketId);
}
