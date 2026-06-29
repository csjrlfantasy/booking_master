package com.ticket.app.service.ticket;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /**
     * 获取所有票务分类
     *
     * @return 分类列表
     */
    List<?> getCategories();

    /**
     * 获取分类及其票务数量
     *
     * @return 分类列表(含数量)
     */
    List<?> getCategoriesWithCount();
}