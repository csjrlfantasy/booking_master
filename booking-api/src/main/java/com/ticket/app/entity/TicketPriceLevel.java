package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票档实体类
 */
@Data
@TableName("ticket_price_level")
public class TicketPriceLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 票档ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 票务ID
     */
    private Long ticketId;

    /**
     * 票档名称: VIP、贵宾票、普通票
     */
    private String name;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 成本价
     */
    private BigDecimal costPrice;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 可用库存
     */
    private Integer availableStock;

    /**
     * 已售数量
     */
    private Integer soldCount;

    /**
     * 座位类型: 1-对号入座, 2-自由入座, 3-站票
     */
    private Integer seatType;

    /**
     * 票档说明
     */
    private String description;

    /**
     * 权益描述
     */
    private String benefits;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 0-不可售, 1-可售
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
     * 座位类型常量
     */
    public static final Integer SEAT_TYPE_RESERVED = 1;
    public static final Integer SEAT_TYPE_FREE = 2;
    public static final Integer SEAT_TYPE_STANDING = 3;
}