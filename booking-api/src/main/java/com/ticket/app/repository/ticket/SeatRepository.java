package com.ticket.app.repository.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.app.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 座位Repository
 */
@Mapper
public interface SeatRepository extends BaseMapper<Seat> {

    /**
     * 根据场次ID查询座位列表
     *
     * @param sessionId 场次ID
     * @return 座位列表
     */
    List<Seat> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据ID列表查询座位
     *
     * @param ids 座位ID列表
     * @return 座位列表
     */
    List<Seat> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根据场次ID和座位ID查询座位
     *
     * @param sessionId 场次ID
     * @param seatNo 座位号
     * @return 座位
     */
    Seat selectBySessionIdAndSeatNo(@Param("sessionId") Long sessionId, @Param("seatNo") String seatNo);

    /**
     * 批量更新座位状态
     *
     * @param seatIds 座位ID列表
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatusBatch(@Param("seatIds") List<Long> seatIds, @Param("status") Integer status);
}
