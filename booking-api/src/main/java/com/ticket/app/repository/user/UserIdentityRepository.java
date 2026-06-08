package com.ticket.app.repository.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.UserIdentity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserIdentityRepository extends BaseMapper<UserIdentity> {
}
