package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体类
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 票务ID
     */
    private Long ticketId;

    /**
     * 场次ID
     */
    private Long sessionId;

    /**
     * 票档ID
     */
    private Long priceLevelId;

    /**
     * 票档名称
     */
    private String priceLevelName;

    /**
     * 座位ID
     */
    private Long seatId;

    /**
     * 座位号
     */
    private String seatNo;

    private String attendeeName;

    private String attendeeIdCard;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 小计金额
     */
    private BigDecimal subtotal;

    /**
     * 状态: 1-正常, 0-已退款
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
    public static final Integer STATUS_NORMAL = 1;
    public static final Integer STATUS_REFUNDED = 0;
}
