package com.ticket.app.controller.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单请求
 */
@Schema(description = "创建订单请求")
@Data
public class CreateOrderRequest {

    @Schema(description = "票务ID", required = true, example = "1")
    @NotNull(message = "票务ID不能为空")
    private Long ticketId;

    @Schema(description = "场次ID", required = true, example = "1")
    @NotNull(message = "场次ID不能为空")
    private Long sessionId;

    @Schema(description = "订单项列表", required = true)
    @NotEmpty(message = "订单项不能为空")
    private List<OrderItemRequest> items;

    @Schema(description = "收货地址ID", example = "1")
    private Long addressId;

    @Schema(description = "联系人姓名", required = true, example = "张三")
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @Schema(description = "联系人手机", required = true, example = "13800138000")
    @NotBlank(message = "联系人手机不能为空")
    private String contactPhone;

    @Schema(description = "联系人身份证", example = "110101199001011234")
    private String contactIdCard;

    @Schema(description = "优惠券ID", example = "1")
    private Long couponId;

    @Schema(description = "用户优惠券ID", example = "301")
    private Long userCouponId;

    @Schema(description = "用户备注", example = "需要发票")
    private String userRemark;

    @Schema(description = "订单来源", example = "app")
    private String source;

    @Schema(description = "是否使用积分抵扣", example = "false")
    private Boolean usePoints = false;

    @Schema(description = "积分抵扣金额", example = "10.00")
    private BigDecimal pointsDiscount = BigDecimal.ZERO;

    /**
     * 订单项请求
     */
    @Schema(description = "订单项请求")
    @Data
    public static class OrderItemRequest {
        
        @Schema(description = "票档ID", required = true, example = "1")
        @NotNull(message = "票档ID不能为空")
        private Long priceLevelId;

        @Schema(description = "票档名称", required = true, example = "VIP区")
        private String priceLevelName;

        @Schema(description = "数量", required = true, example = "2")
        @NotNull(message = "数量不能为空")
        private Integer quantity;

        @Schema(description = "单价", required = true, example = "299.00")
        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;

        @Schema(description = "座位ID(可选)", example = "1")
        private Long seatId;

        @Schema(description = "座位号(可选)", example = "A1")
        private String seatNo;

        @Schema(description = "观演人列表，每张票一个观演人")
        private List<AttendeeRequest> attendees;
    }

    @Schema(description = "观演人请求")
    @Data
    public static class AttendeeRequest {

        @Schema(description = "常用观演人ID", example = "1")
        private Long identityId;

        @Schema(description = "观演人姓名", example = "张三")
        private String realName;

        @Schema(description = "观演人身份证号", example = "11010519491231002X")
        private String idCardNo;
    }
}
