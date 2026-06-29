package com.ticket.app.repository.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.controller.admin.dto.response.AdminFlashSaleResponse;
import com.ticket.app.controller.ticket.dto.TicketFlashSaleResponse;
import com.ticket.app.entity.TicketFlashSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TicketFlashSaleRepository extends BaseMapper<TicketFlashSale> {

    @Select("""
            <script>
            SELECT
                fs.id AS flashSaleId,
                t.id AS id,
                t.id AS ticketId,
                t.name AS title,
                fs.flash_price AS price,
                fs.flash_price AS flashPrice,
                t.minimum_price AS originalPrice,
                t.venue_name AS venue,
                CAST(t.show_start_date AS CHAR) AS date,
                t.category_name AS category,
                t.poster AS imageUrl,
                t.status AS status,
                CASE t.status
                    WHEN 1 THEN '待开售'
                    WHEN 2 THEN '售票中'
                    WHEN 3 THEN '已售罄'
                    WHEN 4 THEN '已结束'
                    ELSE '已下架'
                END AS statusName,
                fs.flash_stock AS flashStock,
                COALESCE(fs.sold_count, 0) AS soldCount,
                GREATEST(fs.flash_stock - COALESCE(fs.sold_count, 0), 0) AS remainingStock,
                fs.start_time AS startTime,
                fs.end_time AS endTime
            FROM ticket_flash_sale fs
            JOIN ticket t ON t.id = fs.ticket_id
            WHERE fs.status = 1
              AND fs.start_time &lt;= NOW()
              AND fs.end_time &gt;= NOW()
              AND fs.flash_stock &gt; COALESCE(fs.sold_count, 0)
              AND t.deleted_at IS NULL
              AND t.status = 2
            ORDER BY fs.start_time DESC, fs.id DESC
            <choose>
                <when test="limit != null and limit &gt; 0">
                    LIMIT #{limit}
                </when>
                <otherwise>
                    LIMIT 10
                </otherwise>
            </choose>
            </script>
            """)
    List<TicketFlashSaleResponse> selectActiveFlashSaleTickets(@Param("limit") Integer limit);

    @Select("""
            SELECT
                fs.id AS id,
                fs.ticket_id AS ticketId,
                t.name AS ticketName,
                fs.flash_price AS flashPrice,
                t.minimum_price AS originalPrice,
                fs.flash_stock AS flashStock,
                COALESCE(fs.sold_count, 0) AS soldCount,
                GREATEST(fs.flash_stock - COALESCE(fs.sold_count, 0), 0) AS remainingStock,
                fs.start_time AS startTime,
                fs.end_time AS endTime,
                fs.status AS status
            FROM ticket_flash_sale fs
            JOIN ticket t ON t.id = fs.ticket_id
            WHERE t.deleted_at IS NULL
            ORDER BY fs.updated_at DESC, fs.id DESC
            """)
    List<AdminFlashSaleResponse> selectAdminFlashSales();
}
