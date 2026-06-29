package com.ticket.app.repository.cart;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车Repository
 */
@Mapper
public interface CartRepository extends BaseMapper<Cart> {

    /**
     * 根据用户ID查询购物车列表
     *
     * @param userId 用户ID
     * @return 购物车列表
     */
    List<Cart> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID删除购物车项
     *
     * @param userId 用户ID
     * @param id 购物车ID
     * @return 影响行数
     */
    int deleteByUserIdAndId(@Param("userId") Long userId, @Param("id") Long id);

    /**
     * 根据用户ID清空购物车
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 检查购物车项是否已存在
     *
     * @param userId 用户ID
     * @param ticketId 票务ID
     * @param sessionId 场次ID
     * @param seatId 座位ID
     * @return 已存在的购物车项
     */
    Cart selectExistingItem(@Param("userId") Long userId,
                            @Param("ticketId") Long ticketId,
                            @Param("sessionId") Long sessionId,
                            @Param("seatId") Long seatId);
}
