package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 座位实体类
 */
@Data
@TableName("seat")
public class Seat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 座位ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 场次ID
     */
    private Long sessionId;

    /**
     * 场馆区域ID
     */
    private Long venueAreaId;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 区域名称
     */
    private String areaName;

    /**
     * 座位编号
     */
    private String seatNo;

    /**
     * 排号
     */
    private String seatRow;

    /**
     * 座号
     */
    private String seatCol;

    /**
     * 票档ID
     */
    private Long priceLevelId;

    /**
     * 实际价格
     */
    private BigDecimal price;

    /**
     * 状态: 0-不可售, 1-可选, 2-锁定, 3-已预订, 4-已售出
     */
    private Integer status;

    /**
     * 锁定过期时间
     */
    private LocalDateTime lockExpireTime;

    /**
     * 锁定用户ID
     */
    private Long lockUserId;

    /**
     * 锁定会话ID
     */
    private String lockSessionId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 锁定原因
     */
    private String lockReason;

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
     * 座位状态常量
     */
    public static final Integer STATUS_UNAVAILABLE = 0;
    public static final Integer STATUS_AVAILABLE = 1;
    public static final Integer STATUS_LOCKED = 2;
    public static final Integer STATUS_RESERVED = 3;
    public static final Integer STATUS_SOLD = 4;
}
