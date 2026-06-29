package com.ticket.app.repository.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员用户Repository
 */
@Mapper
public interface AdminUserRepository extends BaseMapper<User> {

    /**
     * 分页查询用户列表（管理员）
     *
     * @param page 分页参数
     * @param keyword 搜索关键词
     * @param status 用户状态
     * @param userType 用户类型
     * @param startDate 注册开始时间
     * @param endDate 注册结束时间
     * @return 分页结果
     */
    IPage<User> selectAdminUserList(
            Page<User> page,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("userType") Integer userType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 查询用户订单数
     *
     * @param userId 用户ID
     * @return 订单数
     */
    @Select("""
            SELECT COUNT(1)
            FROM `order`
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
            """)
    Integer selectUserOrderCount(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(SUM(pay_amount), 0)
            FROM `order`
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
              AND pay_status = 1
              AND order_status IN (2, 3)
            """)
    BigDecimal selectUserPaidOrderAmount(@Param("userId") Long userId);
}
