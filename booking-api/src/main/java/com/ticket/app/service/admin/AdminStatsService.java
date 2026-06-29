package com.ticket.app.service.admin;

import com.ticket.app.controller.admin.dto.request.*;
import com.ticket.app.controller.admin.dto.response.*;

/**
 * 数据统计服务接口
 */
public interface AdminStatsService {

    /**
     * 获取概览统计
     *
     * @return 概览统计数据
     */
    OverviewStatsResponse getOverviewStats();

    /**
     * 获取销售统计
     *
     * @param request 查询参数
     * @return 销售统计数据
     */
    SalesStatsResponse getSalesStats(SalesStatsRequest request);

    /**
     * 获取订单统计
     *
     * @return 订单统计数据
     */
    OrderStatsResponse getOrderStats();

    /**
     * 获取用户统计
     *
     * @return 用户统计数据
     */
    UserStatsResponse getUserStats();
}