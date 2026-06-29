package com.ticket.app.service.payment;

import com.ticket.app.controller.payment.dto.CreatePaymentRequest;
import com.ticket.app.controller.payment.dto.PaymentResultResponse;
import com.ticket.app.controller.payment.dto.RefundRequest;
import com.ticket.app.controller.payment.dto.RefundResultResponse;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付订单
     *
     * @param userId 用户ID
     * @param request 创建支付请求
     * @return 支付结果
     */
    PaymentResultResponse createPayment(Long userId, CreatePaymentRequest request);

    /**
     * Mock payment for demo and teaching workflows.
     *
     * @param userId user ID
     * @param request payment request
     * @return payment result
     */
    PaymentResultResponse mockPay(Long userId, CreatePaymentRequest request);

    /**
     * 查询支付状态
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 支付结果
     */
    PaymentResultResponse getPaymentStatus(Long userId, String orderNo);

    /**
     * 申请退款
     *
     * @param userId 用户ID
     * @param request 退款请求
     * @return 退款结果
     */
    RefundResultResponse applyRefund(Long userId, RefundRequest request);

    /**
     * 查询退款状态
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 退款结果
     */
    RefundResultResponse getRefundStatus(Long userId, String orderNo);

    /**
     * 取消支付(关闭支付订单)
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     */
    void cancelPayment(Long userId, String orderNo);

    /**
     * 验证支付签名
     *
     * @param orderNo 订单号
     * @param amount 金额
     * @param timestamp 时间戳
     * @param sign 签名
     * @return 是否验证通过
     */
    boolean verifyCallbackSign(String orderNo, String amount, String timestamp, String sign);

    /**
     * 处理支付回调
     *
     * @param orderNo 订单号
     * @param payChannel 支付渠道
     * @param tradeNo 第三方交易号
     */
    void handlePayCallback(String orderNo, String payChannel, String tradeNo);
}
