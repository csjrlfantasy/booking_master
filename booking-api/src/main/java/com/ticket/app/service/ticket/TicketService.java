package com.ticket.app.service.ticket;

import com.ticket.app.common.PageResult;

import java.util.List;

/**
 * 票务服务接口
 */
public interface TicketService {

    /**
     * 获取票务列表
     *
     * @param categoryId 分类ID
     * @param keyword 关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<?> getTicketList(
            Integer categoryId,
            String keyword,
            String startDate,
            String endDate,
            Double minPrice,
            Double maxPrice,
            String sortBy,
            String sortOrder,
            Integer pageNum,
            Integer pageSize
    );

    /**
     * 获取票务详情
     *
     * @param ticketId 票务ID
     * @return 票务详情
     */
    Object getTicketDetail(Long ticketId);

    /**
     * 获取热门票务
     *
     * @param limit 返回数量
     * @return 热门票务列表
     */
    List<?> getHotTickets(Integer limit);

    /**
     * 获取即将开售票务
     *
     * @param limit 返回数量
     * @return 即将开售票务列表
     */
    List<?> getUpcomingTickets(Integer limit);
}
