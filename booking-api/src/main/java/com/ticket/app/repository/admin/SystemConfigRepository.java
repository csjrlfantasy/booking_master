package com.ticket.app.repository.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.admin.SystemConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置Repository
 */
@Mapper
public interface SystemConfigRepository extends BaseMapper<SystemConfig> {
}