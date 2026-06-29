package com.ticket.app.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 支付签名验证工具
 * 用于验证支付回调的真实性
 * 
 * 配置方式：
 * - 系统属性：payment.callback.secret-key（最高优先级）
 * - 环境变量：PAYMENT_CALLBACK_SECRET_KEY
 * - 应用配置：payment.callback.secret-key
 * - 默认值：仅用于开发环境，生产必须配置
 * 
 * 签名算法：HMAC-SHA256
 * 签名有效期：5分钟
 */
public class PaymentSignatureUtils {

    /**
     * 配置属性名
     */
    private static final String CONFIG_KEY = "payment.callback.secret-key";
    
    /**
     * 支付回调签名密钥
     */
    private static volatile String secretKey;
    
    /**
     * 签名算法
     */
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    /**
     * 签名过期时间（毫秒），默认5分钟
     */
    private static final long SIGN_EXPIRE_MILLIS = 5 * 60 * 1000;
    
    /**
     * 初始化密钥（应用启动时调用）
     * 优先级：参数 > 系统属性 > 环境变量 > 配置文件默认值
     */
    public static void init(String key) {
        // 1. 优先使用传入的密钥
        if (key != null && !key.isEmpty()) {
            secretKey = key;
            return;
        }
        
        // 2. 从系统属性获取
        String sysKey = System.getProperty(CONFIG_KEY);
        if (sysKey != null && !sysKey.isEmpty()) {
            secretKey = sysKey;
            return;
        }
        
        // 3. 从环境变量获取
        String envKey = System.getenv("PAYMENT_CALLBACK_SECRET_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            secretKey = envKey;
            return;
        }
        
        // 4. 开发环境默认值，生产必须通过配置覆盖
        // 使用随机生成的开发密钥，避免安全问题
        secretKey = System.getProperty("payment.dev.secret", "dev_only_key_" + System.currentTimeMillis());
    }
    
    /**
     * 获取密钥（延迟初始化）
     */
    private static String getSecretKey() {
        if (secretKey == null) {
            init(null);
        }
        return secretKey;
    }
    
    /**
     * 验证支付回调签名
     * 
     * @param orderNo 订单号
     * @param amount 支付金额
     * @param timestamp 时间戳（毫秒）
     * @param sign 签名
     * @return 是否验证通过
     */
    public static boolean verifyPaymentCallback(String orderNo, String amount, String timestamp, String sign) {
        if (orderNo == null || amount == null || timestamp == null || sign == null) {
            return false;
        }
        
        // 检查时间戳是否过期（5分钟）
        long currentTime = System.currentTimeMillis();
        try {
            long callbackTime = Long.parseLong(timestamp);
            if (Math.abs(currentTime - callbackTime) > SIGN_EXPIRE_MILLIS) {
                // 回调时间超过5分钟，认为过期
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        // 生成签名字符串（不包含密钥）
        String signSource = String.format("orderNo=%s&amount=%s&timestamp=%s", 
            orderNo, amount, timestamp);
        
        // 计算签名
        String calculateSign = calculateHmacSha256(signSource, getSecretKey());
        
        return calculateSign.equalsIgnoreCase(sign);
    }
    
    /**
     * 生成支付回调签名（供支付系统调用）
     * 
     * @param orderNo 订单号
     * @param amount 支付金额
     * @return 签名信息 [timestamp, sign] 用逗号分隔
     */
    public static String generatePaymentSign(String orderNo, String amount) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signSource = String.format("orderNo=%s&amount=%s&timestamp=%s", 
            orderNo, amount, timestamp);
        String sign = calculateHmacSha256(signSource, getSecretKey());
        return timestamp + "," + sign;
    }
    
    /**
     * 生成支付回调签名（供支付系统调用 - 独立返回时间戳和签名）
     */
    public static String[] generatePaymentSignParts(String orderNo, String amount) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signSource = String.format("orderNo=%s&amount=%s&timestamp=%s", 
            orderNo, amount, timestamp);
        String sign = calculateHmacSha256(signSource, getSecretKey());
        return new String[] {timestamp, sign};
    }
    
    /**
     * 计算HMAC-SHA256签名
     */
    private static String calculateHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}