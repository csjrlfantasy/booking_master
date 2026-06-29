package com.ticket.app.repository.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类Repository
 */
@Mapper
public interface CategoryRepository extends BaseMapper<Category> {

    /**
     * 查询所有启用的票务分类
     *
     * @param type 分类类型(可选)
     * @return 分类列表
     */
    List<Category> selectCategories(@Param("type") Integer type);

    /**
     * 查询分类及其票务数量
     *
     * @param type 分类类型
     * @return 分类列表(含数量)
     */
    List<Category> selectCategoriesWithCount(@Param("type") Integer type);
}