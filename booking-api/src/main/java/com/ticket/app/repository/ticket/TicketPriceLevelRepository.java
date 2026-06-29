package com.ticket.app.repository.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.TicketPriceLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 票档Repository
 */
@Mapper
public interface TicketPriceLevelRepository extends BaseMapper<TicketPriceLevel> {

    /**
     * 根据票务ID查询票档列表
     *
     * @param ticketId 票务ID
     * @return 票档列表
     */
    List<TicketPriceLevel> selectByTicketId(@Param("ticketId") Long ticketId);

    @Update("UPDATE ticket_price_level SET available_stock = available_stock - #{quantity}, sold_count = COALESCE(sold_count, 0) + #{quantity} WHERE id = #{priceLevelId} AND status = 1 AND available_stock >= #{quantity}")
    int decreaseStock(@Param("priceLevelId") Long priceLevelId, @Param("quantity") Integer quantity);

    @Update("UPDATE ticket_price_level SET available_stock = available_stock + #{quantity}, sold_count = CASE WHEN COALESCE(sold_count, 0) >= #{quantity} THEN sold_count - #{quantity} ELSE 0 END WHERE id = #{priceLevelId}")
    int restoreStock(@Param("priceLevelId") Long priceLevelId, @Param("quantity") Integer quantity);
}
