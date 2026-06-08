package com.ticket.app.controller.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 订单详情响应
 */
@Schema(description = "订单详情响应")
@Data
public class OrderDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", example = "1")
    private Long id;

    @Schema(description = "订单号", example = "ORD2026042800001")
    private String orderNo;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "票务ID", example = "1")
    private Long ticketId;

    @Schema(description = "票务名称", example = "周杰伦演唱会")
    private String ticketName;

    @Schema(description = "票务海报", example = "https://example.com/poster.jpg")
    private String ticketPoster;

    @Schema(description = "场次ID", example = "1")
    private Long sessionId;

    @Schema(description = "场馆ID", example = "1")
    private Long venueId;

    @Schema(description = "场馆名称", example = "北京鸟巢")
    private String venueName;

    @Schema(description = "演出日期", example = "2026-05-01")
    private LocalDate showDate;

    @Schema(description = "演出时间", example = "19:30:00")
    private LocalTime showTime;

    @Schema(description = "总数量", example = "2")
    private Integer totalQuantity;

    @Schema(description = "订单总金额", example = "598.00")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额", example = "50.00")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额", example = "548.00")
    private BigDecimal payAmount;

    @Schema(description = "优惠券ID", example = "1")
    private Long couponId;

    @Schema(description = "优惠券名称", example = "满100减50券")
    private String couponName;

    @Schema(description = "优惠券优惠金额", example = "50.00")
    private BigDecimal couponAmount;

    @Schema(description = "积分抵扣金额", example = "10.00")
    private BigDecimal pointDiscount;

    @Schema(description = "订单状态: 1-待支付, 2-已支付, 3-已完成, 4-已取消, 5-已退款, 6-退款中", example = "1")
    private Integer orderStatus;

    @Schema(description = "订单状态描述", example = "待支付")
    private String orderStatusText;

    @Schema(description = "支付状态: 0-未支付, 1-已支付, 2-已退款", example = "0")
    private Integer payStatus;

    @Schema(description = "支付状态描述", example = "未支付")
    private String payStatusText;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "支付渠道", example = "alipay")
    private String payChannel;

    @Schema(description = "第三方交易号", example = "2026042800001")
    private String tradeNo;

    @Schema(description = "联系人姓名", example = "张三")
    private String contactName;

    @Schema(description = "联系人手机", example = "13800138000")
    private String contactPhone;

    @Schema(description = "联系人身份证", example = "110101199001011234")
    private String contactIdCard;

    @Schema(description = "用户备注", example = "需要发票")
    private String userRemark;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因", example = "不想要了")
    private String cancelReason;

    @Schema(description = "订单过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "订单项列表")
    private List<OrderItemResponse> items;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 订单项响应
     */
    @Schema(description = "订单项响应")
    @Data
    public static class OrderItemResponse implements Serializable {
        
        private static final long serialVersionUID = 1L;

        @Schema(description = "明细ID", example = "1")
        private Long id;

        @Schema(description = "票档ID", example = "1")
        private Long priceLevelId;

        @Schema(description = "票档名称", example = "VIP区")
        private String priceLevelName;

        @Schema(description = "座位ID", example = "1")
        private Long seatId;

        @Schema(description = "座位号", example = "A1")
        private String seatNo;

        @Schema(description = "观演人姓名，多个用逗号分隔")
        private String attendeeName;

        @Schema(description = "观演人身份证，多个用逗号分隔")
        private String attendeeIdCard;

        @Schema(description = "数量", example = "2")
        private Integer quantity;

        @Schema(description = "单价", example = "299.00")
        private BigDecimal unitPrice;

        @Schema(description = "小计金额", example = "598.00")
        private BigDecimal subtotal;

        @Schema(description = "状态: 1-正常, 0-已退款", example = "1")
        private Integer status;
    }
}
