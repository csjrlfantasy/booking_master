package com.ticket.app.repository.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单Repository
 */
@Mapper
public interface OrderRepository extends BaseMapper<Order> {

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单
     */
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据用户ID分页查询订单列表
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param status 订单状态(可选)
     * @param startDate 开始日期(可选)
     * @param endDate 结束日期(可选)
     * @return 分页结果
     */
    IPage<Order> selectByUserIdPage(
            Page<Order> page,
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 根据用户ID查询订单列表
     *
     * @param userId 用户ID
     * @param status 订单状态(可选)
     * @return 订单列表
     */
    List<Order> selectByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 根据用户ID和订单号查询订单
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 订单
     */
    Order selectByUserIdAndOrderNo(@Param("userId") Long userId, @Param("orderNo") String orderNo);
}
