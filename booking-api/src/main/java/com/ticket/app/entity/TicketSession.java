package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场次实体类
 */
@Data
@TableName("ticket_session")
public class TicketSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 场次ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 票务ID
     */
    private Long ticketId;

    /**
     * 场次编号
     */
    private String sessionNo;

    /**
     * 场次名称
     */
    private String sessionName;

    /**
     * 演出日期
     */
    private LocalDate showDate;

    /**
     * 开始时间
     */
    private LocalTime showStartTime;

    /**
     * 结束时间
     */
    private LocalTime showEndTime;

    /**
     * 星期几
     */
    private String weekDay;

    /**
     * 场馆区域ID
     */
    private Long venueAreaId;

    /**
     * 总座位数
     */
    private Integer totalCapacity;

    /**
     * 可用座位数
     */
    private Integer availableCapacity;

    /**
     * 状态: 0-取消, 1-可售, 2-停售
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 状态常量
     */
    public static final Integer STATUS_CANCELLED = 0;
    public static final Integer STATUS_ON_SALE = 1;
    public static final Integer STATUS_STOPPED = 2;
}
