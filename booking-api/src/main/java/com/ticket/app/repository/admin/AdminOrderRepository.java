package com.ticket.app.repository.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员订单Repository
 */
@Mapper
public interface AdminOrderRepository extends BaseMapper<Order> {

    /**
     * 分页查询订单列表（管理员）
     *
     * @param page 分页参数
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param status 订单状态
     * @param payStatus 支付状态
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 分页结果
     */
    IPage<Order> selectAdminOrderList(
            Page<Order> page,
            @Param("orderNo") String orderNo,
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("payStatus") Integer payStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 统计各状态订单数
     *
     * @return 统计结果列表 (status, count)
     */
    List<Object> selectOrderStatusCount();

    /**
     * 统计今日订单数
     *
     * @param todayEnd 今日结束时间
     * @return 订单数
     */
    Integer selectTodayOrderCount(@Param("todayEnd") LocalDateTime todayEnd);
}