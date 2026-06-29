package com.ticket.app.config;

import com.ticket.app.common.util.PaymentSignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 支付配置初始化器
 * 在应用启动时初始化支付相关配置
 */
@Slf4j
@Component
public class PaymentConfigInitializer implements CommandLineRunner {

    @Value("${payment.callback.secret-key:}")
    private String paymentSecretKey;

    @Override
    public void run(String... args) {
        // 初始化支付签名密钥
        if (paymentSecretKey != null && !paymentSecretKey.isEmpty()) {
            PaymentSignatureUtils.init(paymentSecretKey);
            log.info("支付签名密钥初始化完成");
        } else {
            // 使用默认值（仅开发环境）
            PaymentSignatureUtils.init(null);
            log.warn("使用默认支付签名密钥，仅适用于开发环境，生产环境请配置 payment.callback.secret-key");
        }
    }
}
