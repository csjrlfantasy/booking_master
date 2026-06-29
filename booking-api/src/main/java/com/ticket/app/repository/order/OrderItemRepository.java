package com.ticket.app.repository.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细Repository
 */
@Mapper
public interface OrderItemRepository extends BaseMapper<OrderItem> {

    /**
     * 根据订单ID查询订单明细列表
     *
     * @param orderId 订单ID
     * @return 订单明细列表
     */
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单号查询订单明细列表
     *
     * @param orderNo 订单号
     * @return 订单明细列表
     */
    List<OrderItem> selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据订单ID批量删除订单明细
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    int deleteByOrderId(@Param("orderId") Long orderId);
}
