package com.ticket.app.repository.banner;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BannerRepository extends BaseMapper<Banner> {

    List<Banner> selectVisibleByPlatform(@Param("platform") String platform, @Param("now") LocalDateTime now);
}
