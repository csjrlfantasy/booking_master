package com.ticket.app.service.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.PaymentSignatureUtils;
import com.ticket.app.controller.payment.dto.CreatePaymentRequest;
import com.ticket.app.controller.payment.dto.PaymentResultResponse;
import com.ticket.app.controller.payment.dto.RefundRequest;
import com.ticket.app.controller.payment.dto.RefundResultResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.User;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 支付服务实现
 * 
 * 注意：这是一个模拟支付服务实现
 * 实际生产环境需要对接真实的支付渠道(支付宝、微信支付等)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * 支付过期时间(分钟)
     */
    private static final int PAYMENT_EXPIRE_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultResponse createPayment(Long userId, CreatePaymentRequest request) {
        log.info("创建支付订单: userId={}, orderNo={}, amount={}, payChannel={}", 
                userId, request.getOrderNo(), request.getAmount(), request.getPayChannel());

        // 1. 查询订单
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getOrderNo, request.getOrderNo());
        orderWrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(orderWrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 2. 检查订单状态
        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            if (Order.STATUS_PAID.equals(order.getOrderStatus())) {
                throw new BusinessException(ResultCode.ORDER_ALREADY_PAID, "订单已支付");
            }
            if (Order.STATUS_CANCELLED.equals(order.getOrderStatus())) {
                throw new BusinessException(ResultCode.ORDER_ALREADY_CANCELLED, "订单已取消");
            }
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
        }

        // 3. 验证支付金额
        if (request.getAmount().compareTo(order.getPayAmount()) != 0) {
            log.warn("支付金额不匹配: requestAmount={}, orderAmount={}", 
                    request.getAmount(), order.getPayAmount());
            throw new BusinessException(ResultCode.PAYMENT_AMOUNT_ERROR, "支付金额不正确");
        }

        // 4. 检查订单是否过期
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException(ResultCode.PAYMENT_EXPIRED, "订单已过期");
        }

        // 5. 验证支付渠道
        if (!isValidPayChannel(request.getPayChannel())) {
            throw new BusinessException(ResultCode.PAYMENT_CHANNEL_ERROR, "不支持的支付渠道");
        }

        // 6. 更新订单支付渠道
        order.setPayChannel(request.getPayChannel());
        orderRepository.updateById(order);

        // 7. 生成支付链接/二维码(模拟实现)
        // 实际生产环境：调用支付渠道SDK获取支付二维码或跳转链接
        PaymentResultResponse response = buildPaymentResponse(order, request);
        
        log.info("支付订单创建成功: orderNo={}, paymentId={}", 
                request.getOrderNo(), response.getPaymentId());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultResponse mockPay(Long userId, CreatePaymentRequest request) {
        createPayment(userId, request);
        debitWalletBalance(userId, request.getPayChannel(), request.getAmount());
        String tradeNo = "MOCK-" + request.getPayChannel().toUpperCase() + "-" + UUID.randomUUID();
        handlePayCallback(request.getOrderNo(), request.getPayChannel(), tradeNo);
        return getPaymentStatus(userId, request.getOrderNo());
    }

    private void debitWalletBalance(Long userId, String payChannel, BigDecimal amount) {
        if ("wechat".equalsIgnoreCase(payChannel)) {
            int updated = userRepository.debitWechatBalance(userId, amount);
            if (updated == 0) {
                throwInsufficientBalance(userId, "wechat");
            }
            return;
        }
        if ("alipay".equalsIgnoreCase(payChannel)) {
            int updated = userRepository.debitAlipayBalance(userId, amount);
            if (updated == 0) {
                throwInsufficientBalance(userId, "alipay");
            }
        }
    }

    private void throwInsufficientBalance(Long userId, String payChannel) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }
        String channelName = "wechat".equalsIgnoreCase(payChannel) ? "微信" : "支付宝";
        throw new BusinessException(ResultCode.PAYMENT_FAILED, channelName + "余额不足");
    }

    @Override
    public PaymentResultResponse getPaymentStatus(Long userId, String orderNo) {
        log.info("查询支付状态: userId={}, orderNo={}", userId, orderNo);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }

        return buildPaymentStatusResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResultResponse applyRefund(Long userId, RefundRequest request) {
        log.info("申请退款: userId={}, orderNo={}, reason={}", 
                userId, request.getOrderNo(), request.getReason());

        // 1. 查询订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, request.getOrderNo());
        wrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 2. 检查订单状态(只有已支付订单才能申请退款)
        if (!Order.STATUS_PAID.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.REFUND_NOT_ALLOWED, "只有已支付订单才能申请退款");
        }

        // 3. 检查是否已申请过退款
        if (Order.STATUS_REFUNDING.equals(order.getOrderStatus()) || 
            Order.STATUS_REFUNDED.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.REFUND_ALREADY_PROCESSED, "退款已处理");
        }

        // 4. 计算退款金额
        BigDecimal refundAmount = calculateCustomerRefundAmount(order);

        // 5. 更新订单状态为退款中
        order.setOrderStatus(Order.STATUS_REFUNDING);
        order.setRemark("退款申请: " + request.getReason());
        orderRepository.updateById(order);

        // 6. 构建退款响应(模拟实现)
        RefundResultResponse response = RefundResultResponse.builder()
                .refundId(System.currentTimeMillis())
                .orderNo(request.getOrderNo())
                .payAmount(order.getPayAmount())
                .refundAmount(refundAmount)
                .status("PROCESSING")
                .statusText("退款处理中")
                .refundType("USER")
                .reason(request.getReason())
                .applyTime(LocalDateTime.now())
                .build();

        log.info("退款申请成功: orderNo={}, refundId={}, refundAmount={}", 
                request.getOrderNo(), response.getRefundId(), refundAmount);

        return response;
    }

    private BigDecimal calculateCustomerRefundAmount(Order order) {
        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
        LocalDate showDate = order.getShowDate();
        LocalTime showTime = order.getShowTime() != null ? order.getShowTime() : LocalTime.of(0, 0);
        if (showDate == null) {
            return payAmount.setScale(2, RoundingMode.HALF_UP);
        }

        LocalDateTime showDateTime = LocalDateTime.of(showDate, showTime);
        long hoursBeforeShow = ChronoUnit.HOURS.between(LocalDateTime.now(), showDateTime);
        if (hoursBeforeShow <= 0) {
            throw new BusinessException(ResultCode.REFUND_NOT_ALLOWED, "演出已开始，不能申请退票");
        }

        BigDecimal feeRate;
        if (hoursBeforeShow <= 24) {
            feeRate = new BigDecimal("0.75");
        } else if (hoursBeforeShow <= 48) {
            feeRate = new BigDecimal("0.50");
        } else if (hoursBeforeShow <= 168) {
            feeRate = new BigDecimal("0.75");
        } else {
            feeRate = BigDecimal.ZERO;
        }
        return payAmount.multiply(BigDecimal.ONE.subtract(feeRate)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public RefundResultResponse getRefundStatus(Long userId, String orderNo) {
        log.info("查询退款状态: userId={}, orderNo={}", userId, orderNo);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 判断退款状态
        String status;
        String statusText;
        if (Order.STATUS_REFUNDED.equals(order.getOrderStatus())) {
            status = "SUCCESS";
            statusText = "退款成功";
        } else if (Order.STATUS_REFUNDING.equals(order.getOrderStatus())) {
            status = "PROCESSING";
            statusText = "退款处理中";
        } else {
            status = "NONE";
            statusText = "无退款";
        }

        return RefundResultResponse.builder()
                .orderNo(orderNo)
                .payAmount(order.getPayAmount())
                .refundAmount(order.getPayAmount())
                .status(status)
                .statusText(statusText)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPayment(Long userId, String orderNo) {
        log.info("取消支付: userId={}, orderNo={}", userId, orderNo);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 只有待支付订单可以取消
        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不允许取消支付");
        }

        // 更新订单状态为已取消
        order.setOrderStatus(Order.STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("用户取消支付");
        orderRepository.updateById(order);

        log.info("支付取消成功: orderNo={}", orderNo);
    }

    @Override
    public boolean verifyCallbackSign(String orderNo, String amount, String timestamp, String sign) {
        return PaymentSignatureUtils.verifyPaymentCallback(orderNo, amount, timestamp, sign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayCallback(String orderNo, String payChannel, String tradeNo) {
        log.info("处理支付回调: orderNo={}, payChannel={}, tradeNo={}", 
                orderNo, payChannel, tradeNo);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            log.error("支付回调订单不存在: orderNo={}", orderNo);
            return;
        }

        // 安全检查：验证是否已支付
        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            log.info("订单已支付，忽略回调: orderNo={}", orderNo);
            return;
        }

        // 安全检查：验证订单状态
        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            log.warn("订单状态不允许支付: orderNo={}, orderStatus={}", 
                    orderNo, order.getOrderStatus());
            return;
        }

        // 安全检查：验证订单是否过期
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            log.warn("订单已过期，忽略支付回调: orderNo={}, expireTime={}", 
                    orderNo, order.getExpireTime());
            return;
        }

        // 更新支付状态
        order.setPayStatus(Order.PAY_STATUS_PAID);
        order.setOrderStatus(Order.STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayChannel(payChannel);
        order.setTradeNo(tradeNo);
        orderRepository.updateById(order);

        log.info("订单支付成功: orderNo={}, payChannel={}, tradeNo={}", 
                orderNo, payChannel, tradeNo);
    }

    /**
     * 验证支付渠道是否有效
     */
    private boolean isValidPayChannel(String payChannel) {
        if (payChannel == null) {
            return false;
        }
        return "alipay".equalsIgnoreCase(payChannel) ||
               "wechat".equalsIgnoreCase(payChannel) ||
               "bank".equalsIgnoreCase(payChannel) ||
               "unionpay".equalsIgnoreCase(payChannel);
    }

    /**
     * 构建支付响应
     */
    private PaymentResultResponse buildPaymentResponse(Order order, CreatePaymentRequest request) {
        String status;
        String statusText;
        
        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            status = "SUCCESS";
            statusText = "支付成功";
        } else {
            status = "PENDING";
            statusText = "待支付";
        }

        // 根据支付场景生成不同的支付参数(模拟实现)
        String qrCodeUrl = null;
        String payUrl = null;
        
        if ("NATIVE".equalsIgnoreCase(request.getPaymentScene())) {
            // 扫码支付场景，生成模拟二维码
            qrCodeUrl = "https://api.example.com/qrcode/" + UUID.randomUUID().toString();
        } else if ("H5".equalsIgnoreCase(request.getPaymentScene()) || "APP".equalsIgnoreCase(request.getPaymentScene())) {
            // H5或APP支付场景，返回支付跳转链接
            payUrl = "https://pay.example.com/h5?orderNo=" + order.getOrderNo() + "&amount=" + order.getPayAmount();
        }

        return PaymentResultResponse.builder()
                .paymentId(System.currentTimeMillis())
                .orderNo(order.getOrderNo())
                .amount(order.getPayAmount())
                .status(status)
                .statusText(statusText)
                .payChannel(request.getPayChannel())
                .tradeNo(order.getTradeNo())
                .qrCodeUrl(qrCodeUrl)
                .payUrl(payUrl)
                .expireTime(order.getExpireTime())
                .payTime(order.getPayTime())
                .build();
    }

    /**
     * 构建支付状态响应
     */
    private PaymentResultResponse buildPaymentStatusResponse(Order order) {
        String status;
        String statusText;
        
        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            status = "SUCCESS";
            statusText = "支付成功";
        } else if (Order.PAY_STATUS_REFUNDED.equals(order.getPayStatus())) {
            status = "REFUNDED";
            statusText = "已退款";
        } else {
            // 检查是否过期
            if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
                status = "CLOSED";
                statusText = "已关闭";
            } else {
                status = "PENDING";
                statusText = "待支付";
            }
        }

        return PaymentResultResponse.builder()
                .paymentId(System.currentTimeMillis())
                .orderNo(order.getOrderNo())
                .amount(order.getPayAmount())
                .status(status)
                .statusText(statusText)
                .payChannel(order.getPayChannel())
                .tradeNo(order.getTradeNo())
                .expireTime(order.getExpireTime())
                .payTime(order.getPayTime())
                .build();
    }
}
