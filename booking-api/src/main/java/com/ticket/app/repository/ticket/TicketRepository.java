package com.ticket.app.repository.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 票务Repository
 */
@Mapper
public interface TicketRepository extends BaseMapper<Ticket> {

    /**
     * 分页查询票务列表
     *
     * @param page 分页参数
     * @param categoryId 分类ID(可选)
     * @param keyword 关键词(可选)
     * @param startDate 开始日期(可选)
     * @param endDate 结束日期(可选)
     * @param minPrice 最低价格(可选)
     * @param maxPrice 最高价格(可选)
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 分页结果
     */
    IPage<Ticket> selectTicketPage(
            Page<Ticket> page,
            @Param("categoryId") Integer categoryId,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 查询热门票务
     *
     * @param limit 返回数量
     * @return 热门票务列表
     */
    List<Ticket> selectHotTickets(@Param("limit") Integer limit);

    /**
     * 查询即将开售票务
     *
     * @param limit 返回数量
     * @return 即将开售票务列表
     */
    List<Ticket> selectUpcomingTickets(@Param("limit") Integer limit);

    /**
     * 根据ID查询票务详情
     *
     * @param id 票务ID
     * @return 票务详情
     */
    Ticket selectTicketDetailById(@Param("id") Long id);

    @Update("UPDATE ticket SET available_stock = available_stock - #{quantity}, sale_count = COALESCE(sale_count, 0) + #{quantity} WHERE id = #{ticketId} AND status = 2 AND available_stock >= #{quantity}")
    int decreaseStock(@Param("ticketId") Long ticketId, @Param("quantity") Integer quantity);

    @Update("UPDATE ticket SET available_stock = available_stock + #{quantity}, sale_count = CASE WHEN COALESCE(sale_count, 0) >= #{quantity} THEN sale_count - #{quantity} ELSE 0 END WHERE id = #{ticketId}")
    int restoreStock(@Param("ticketId") Long ticketId, @Param("quantity") Integer quantity);
}
