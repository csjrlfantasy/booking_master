package com.ticket.app.repository.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户Repository
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {

    @Update("UPDATE `user` SET wechat_balance = wechat_balance - #{amount} " +
            "WHERE id = #{userId} AND wechat_balance >= #{amount}")
    int debitWechatBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE `user` SET alipay_balance = alipay_balance - #{amount} " +
            "WHERE id = #{userId} AND alipay_balance >= #{amount}")
    int debitAlipayBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
