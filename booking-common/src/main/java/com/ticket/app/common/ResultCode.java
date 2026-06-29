package com.ticket.app.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ========== 成功 ==========
    SUCCESS(0, "success"),

    // ========== 用户模块错误 (1001xx) ==========
    USER_NOT_FOUND(100100, "用户不存在"),
    USER_ALREADY_EXISTS(100101, "用户已存在"),
    USER_DISABLED(100102, "用户已被禁用"),
    USER_PASSWORD_ERROR(100103, "用户名或密码错误"),
    USER_PHONE_EXISTS(100104, "手机号已被注册"),
    USER_EMAIL_EXISTS(100105, "邮箱已被注册"),
    USER_USERNAME_EXISTS(100106, "用户名已被注册"),
    USER_SMS_CODE_ERROR(100107, "短信验证码错误"),
    USER_SMS_CODE_EXPIRED(100108, "短信验证码已过期"),
    USER_SMS_CODE_SEND_FAILED(100109, "短信验证码发送失败"),
    USER_LOGIN_LOCKED(100110, "账号已被锁定，请30分钟后再试"),
    USER_AUTH_NOT_PASS(100111, "实名认证未通过"),
    USER_AUTH_PENDING(100112, "实名认证审核中"),

    // ========== 票务模块错误 (1002xx) ==========
    TICKET_NOT_FOUND(100200, "票务不存在"),
    TICKET_OFF_SALE(100201, "票务已下架"),
    TICKET_NOT_START_SALE(100202, "票务尚未开始销售"),
    TICKET_SOLD_OUT(100203, "票务已售罄"),
    TICKET_EXPIRED(100204, "票务已过期"),
    TICKET_CATEGORY_NOT_FOUND(100205, "票务分类不存在"),

    // ========== 购物车模块错误 (1002xx) ==========
    CART_ITEM_NOT_FOUND(100250, "购物车项不存在"),
    CART_ITEM_INVALID(100251, "购物车项已失效"),
    CART_ITEM_QUANTITY_ERROR(100252, "数量不正确"),

    // ========== 订单模块错误 (1003xx) ==========
    ORDER_NOT_FOUND(100300, "订单不存在"),
    ORDER_ALREADY_PAID(100301, "订单已支付"),
    ORDER_ALREADY_CANCELLED(100302, "订单已取消"),
    ORDER_EXPIRED(100303, "订单已过期"),
    ORDER_STATUS_ERROR(100304, "订单状态不正确"),
    ORDER_CREATE_FAILED(100305, "订单创建失败"),
    SEAT_NOT_AVAILABLE(100306, "座位不可选"),
    SEAT_ALREADY_LOCKED(100307, "座位已被锁定"),
    SEAT_ALREADY_SOLD(100308, "座位已售出"),
    SEAT_LOCK_FAILED(100309, "座位锁定失败"),
    SEAT_SELECT_MAX_LIMIT(100310, "选座数量已达上限"),
    INSUFFICIENT_STOCK(100311, "库存不足"),
    COUPON_NOT_FOUND(100312, "优惠券不存在"),
    COUPON_NOT_USABLE(100313, "优惠券不可用"),
    COUPON_EXPIRED(100314, "优惠券已过期"),
    COUPON_ALREADY_USED(100315, "优惠券已使用"),
    COUPON_REACH_LIMIT(100316, "优惠券已达领取上限"),

    // ========== 支付模块错误 (1004xx) ==========
    PAYMENT_FAILED(100400, "支付失败"),
    PAYMENT_CREATE_FAILED(100401, "支付创建失败"),
    PAYMENT_EXPIRED(100402, "支付已过期"),
    PAYMENT_AMOUNT_ERROR(100403, "支付金额不正确"),
    PAYMENT_CHANNEL_ERROR(100404, "支付渠道不支持"),
    PAYMENT_NOTIFY_SIGN_ERROR(100405, "支付回调签名验证失败"),

    // ========== 退款模块错误 (1005xx) ==========
    REFUND_NOT_FOUND(100500, "退款单不存在"),
    REFUND_ALREADY_PROCESSED(100501, "退款已处理"),
    REFUND_REJECTED(100502, "退款被拒绝"),
    REFUND_AMOUNT_ERROR(100503, "退款金额不正确"),
    REFUND_NOT_ALLOWED(100504, "当前订单不允许退款"),

    // ========== 通用业务错误 (1006xx) ==========
    NOT_FOUND(100000, "资源不存在"),
    FORBIDDEN(100001, "禁止访问"),
    PARAM_ERROR(100600, "参数错误"),
    PARAM_MISSING(100601, "缺少必填参数"),
    TOKEN_INVALID(100603, "Token无效"),
    TOKEN_EXPIRED(100604, "Token已过期"),
    TOKEN_MISSING(100605, "Token不能为空"),
    PARAM_INVALID(100602, "参数格式不正确"),
    VERIFY_CODE_ERROR(100603, "验证码错误"),
    VERIFY_CODE_EXPIRED(100604, "验证码已过期"),

    // ========== 系统错误 (2001xx) ==========
    SYSTEM_ERROR(200100, "系统繁忙，请稍后再试"),
    SERVICE_UNAVAILABLE(200101, "服务不可用"),
    DATABASE_ERROR(200102, "数据库操作失败"),
    CACHE_ERROR(200103, "缓存操作失败"),
    NETWORK_ERROR(200104, "网络请求失败"),

    // ========== 第三方服务错误 (3001xx) ==========
    SMS_SERVICE_ERROR(300100, "短信服务异常"),
    PAYMENT_SERVICE_ERROR(300101, "支付服务异常"),
    THIRD_PARTY_ERROR(300102, "第三方服务异常");

    private final Integer code;
    private final String message;
}
