package com.ticket.app.repository.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.TicketSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 场次Repository
 */
@Mapper
public interface TicketSessionRepository extends BaseMapper<TicketSession> {

    /**
     * 根据票务ID查询场次列表
     *
     * @param ticketId 票务ID
     * @return 场次列表
     */
    @Select("SELECT id, ticket_id, session_no, session_name, show_date, show_start_time, show_end_time, week_day, venue_area_id, total_capacity, available_capacity, status, created_at, updated_at FROM ticket_session WHERE ticket_id = #{ticketId} ORDER BY show_date ASC, show_start_time ASC")
    List<TicketSession> selectByTicketId(@Param("ticketId") Long ticketId);

    /**
     * 根据ID查询场次详情
     *
     * @param id 场次ID
     * @return 场次
     */
    TicketSession selectById(@Param("id") Long id);

    @Update("UPDATE ticket_session SET available_capacity = available_capacity - #{quantity} WHERE id = #{sessionId} AND status = 1 AND available_capacity >= #{quantity}")
    int decreaseCapacity(@Param("sessionId") Long sessionId, @Param("quantity") Integer quantity);

    @Update("UPDATE ticket_session SET available_capacity = available_capacity + #{quantity} WHERE id = #{sessionId}")
    int restoreCapacity(@Param("sessionId") Long sessionId, @Param("quantity") Integer quantity);
}
