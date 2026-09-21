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
import com.ticket.app.service.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.ticket.app.service.order.InventoryCompensationService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final InventoryCompensationService inventory;

    private static final int PAYMENT_EXPIRE_MINUTES = 30;

    @Autowired
    public PaymentServiceImpl(OrderRepository orderRepository, UserRepository userRepository,
                              CouponService couponService, InventoryCompensationService inventory) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.couponService = couponService;
        this.inventory = inventory;
    }

    public PaymentServiceImpl(OrderRepository orderRepository, UserRepository userRepository, CouponService couponService) {
        this(orderRepository, userRepository, couponService, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultResponse createPayment(Long userId, CreatePaymentRequest request) {
        log.info("鍒涘缓鏀粯璁㈠崟: userId={}, orderNo={}, amount={}, payChannel={}",
                userId, request.getOrderNo(), request.getAmount(), request.getPayChannel());

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getOrderNo, request.getOrderNo());
        orderWrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(orderWrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "璁㈠崟涓嶅瓨鍦?");
        }

        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            if (Order.STATUS_PAID.equals(order.getOrderStatus())) {
                throw new BusinessException(ResultCode.ORDER_ALREADY_PAID, "璁㈠崟宸叉敮浠?");
            }
            if (Order.STATUS_CANCELLED.equals(order.getOrderStatus())) {
                throw new BusinessException(ResultCode.ORDER_ALREADY_CANCELLED, "璁㈠崟宸插彇娑?");
            }
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "璁㈠崟鐘舵€佷笉鍏佽鏀粯");
        }

        if (request.getAmount().compareTo(order.getPayAmount()) != 0) {
            log.warn("鏀粯閲戦涓嶅尮閰? requestAmount={}, orderAmount={}",
                    request.getAmount(), order.getPayAmount());
            throw new BusinessException(ResultCode.PAYMENT_AMOUNT_ERROR, "鏀粯閲戦涓嶆纭?");
        }

        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException(ResultCode.PAYMENT_EXPIRED, "璁㈠崟宸茶繃鏈?");
        }

        if (!isValidPayChannel(request.getPayChannel())) {
            throw new BusinessException(ResultCode.PAYMENT_CHANNEL_ERROR, "涓嶆敮鎸佺殑鏀粯娓犻亾");
        }

        order.setPayChannel(request.getPayChannel());
        orderRepository.updateById(order);

        PaymentResultResponse response = buildPaymentResponse(order, request);
        log.info("鏀粯璁㈠崟鍒涘缓鎴愬姛: orderNo={}, paymentId={}",
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
        log.info("鏌ヨ鏀粯鐘舵€? userId={}, orderNo={}", userId, orderNo);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "璁㈠崟涓嶅瓨鍦?");
        }

        return buildPaymentStatusResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResultResponse applyRefund(Long userId, RefundRequest request) {
        log.info("鐢宠閫€娆? userId={}, orderNo={}, reason={}",
                userId, request.getOrderNo(), request.getReason());

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, request.getOrderNo());
        wrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "璁㈠崟涓嶅瓨鍦?");
        }

        if (!Order.STATUS_PAID.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.REFUND_NOT_ALLOWED, "鍙湁宸叉敮浠樿鍗曟墠鑳界敵璇烽€€娆?");
        }

        if (Order.STATUS_REFUNDING.equals(order.getOrderStatus())
                || Order.STATUS_REFUNDED.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.REFUND_ALREADY_PROCESSED, "閫€娆惧凡澶勭悊");
        }

        BigDecimal refundAmount = calculateCustomerRefundAmount(order);

        order.setOrderStatus(Order.STATUS_REFUNDING);
        order.setRemark("閫€娆剧敵璇? " + request.getReason());
        orderRepository.updateById(order);

        if (order.getUserCouponId() != null) {
            couponService.returnCoupon(order.getUserId(), order.getId(), true);
        }

        RefundResultResponse response = RefundResultResponse.builder()
                .refundId(System.currentTimeMillis())
                .orderNo(request.getOrderNo())
                .payAmount(order.getPayAmount())
                .refundAmount(refundAmount)
                .status("PROCESSING")
                .statusText("閫€娆惧鐞嗕腑")
                .refundType("USER")
                .reason(request.getReason())
                .applyTime(LocalDateTime.now())
                .build();

        log.info("閫€娆剧敵璇锋垚鍔? orderNo={}, refundId={}, refundAmount={}",
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
            throw new BusinessException(ResultCode.REFUND_NOT_ALLOWED, "婕斿嚭宸插紑濮嬶紝涓嶈兘鐢宠閫€绁?");
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
        log.info("鏌ヨ閫€娆剧姸鎬? userId={}, orderNo={}", userId, orderNo);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "璁㈠崟涓嶅瓨鍦?");
        }

        String status;
        String statusText;
        if (Order.STATUS_REFUNDED.equals(order.getOrderStatus())) {
            status = "SUCCESS";
            statusText = "閫€娆炬垚鍔?";
        } else if (Order.STATUS_REFUNDING.equals(order.getOrderStatus())) {
            status = "PROCESSING";
            statusText = "閫€娆惧鐞嗕腑";
        } else {
            status = "NONE";
            statusText = "鏃犻€€娆?";
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
        log.info("鍙栨秷鏀粯: userId={}, orderNo={}", userId, orderNo);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "璁㈠崟涓嶅瓨鍦?");
        }

        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "褰撳墠璁㈠崟鐘舵€佷笉鍏佽鍙栨秷鏀粯");
        }

        order.setOrderStatus(Order.STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("鐢ㄦ埛鍙栨秷鏀粯");
        orderRepository.updateById(order);
        inventory.restoreOnce(order.getId());

        log.info("鏀粯鍙栨秷鎴愬姛: orderNo={}", orderNo);
    }

    @Override
    public boolean verifyCallbackSign(String orderNo, String amount, String timestamp, String sign) {
        return PaymentSignatureUtils.verifyPaymentCallback(orderNo, amount, timestamp, sign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayCallback(String orderNo, String payChannel, String tradeNo) {
        log.info("澶勭悊鏀粯鍥炶皟: orderNo={}, payChannel={}, tradeNo={}",
                orderNo, payChannel, tradeNo);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderRepository.selectOne(wrapper);

        if (order == null) {
            log.error("鏀粯鍥炶皟璁㈠崟涓嶅瓨鍦? orderNo={}", orderNo);
            return;
        }

        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            log.info("璁㈠崟宸叉敮浠橈紝蹇界暐鍥炶皟: orderNo={}", orderNo);
            return;
        }

        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            log.warn("璁㈠崟鐘舵€佷笉鍏佽鏀粯: orderNo={}, orderStatus={}",
                    orderNo, order.getOrderStatus());
            return;
        }

        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            log.warn("璁㈠崟宸茶繃鏈燂紝蹇界暐鏀粯鍥炶皟: orderNo={}, expireTime={}",
                    orderNo, order.getExpireTime());
            return;
        }

        order.setPayStatus(Order.PAY_STATUS_PAID);
        order.setOrderStatus(Order.STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayChannel(payChannel);
        order.setTradeNo(tradeNo);
        orderRepository.updateById(order);

        log.info("璁㈠崟鏀粯鎴愬姛: orderNo={}, payChannel={}, tradeNo={}",
                orderNo, payChannel, tradeNo);
    }

    private boolean isValidPayChannel(String payChannel) {
        if (payChannel == null) {
            return false;
        }
        return "alipay".equalsIgnoreCase(payChannel)
                || "wechat".equalsIgnoreCase(payChannel)
                || "bank".equalsIgnoreCase(payChannel)
                || "unionpay".equalsIgnoreCase(payChannel);
    }

    private PaymentResultResponse buildPaymentResponse(Order order, CreatePaymentRequest request) {
        String status;
        String statusText;

        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            status = "SUCCESS";
            statusText = "鏀粯鎴愬姛";
        } else {
            status = "PENDING";
            statusText = "寰呮敮浠?";
        }

        String qrCodeUrl = null;
        String payUrl = null;

        if ("NATIVE".equalsIgnoreCase(request.getPaymentScene())) {
            qrCodeUrl = "https://api.example.com/qrcode/" + UUID.randomUUID();
        } else if ("H5".equalsIgnoreCase(request.getPaymentScene())
                || "APP".equalsIgnoreCase(request.getPaymentScene())) {
            payUrl = "https://pay.example.com/h5?orderNo=" + order.getOrderNo()
                    + "&amount=" + order.getPayAmount();
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

    private PaymentResultResponse buildPaymentStatusResponse(Order order) {
        String status;
        String statusText;

        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            status = "SUCCESS";
            statusText = "鏀粯鎴愬姛";
        } else if (Order.PAY_STATUS_REFUNDED.equals(order.getPayStatus())) {
            status = "REFUNDED";
            statusText = "宸查€€娆?";
        } else {
            if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
                status = "CLOSED";
                statusText = "宸插叧闂?";
            } else {
                status = "PENDING";
                statusText = "寰呮敮浠?";
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
