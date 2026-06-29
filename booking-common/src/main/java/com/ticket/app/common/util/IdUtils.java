package com.ticket.app.common.util;

import cn.hutool.core.util.StrUtil;

import java.util.UUID;

/**
 * 生成唯一ID的工具类
 */
public class IdUtils {

    private static final Snowflake SNOWFLAKE = new Snowflake();

    /**
     * 生成Snowflake ID
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 生成Snowflake ID字符串
     */
    public static String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 生成UUID (去横线)
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成订单号
     * 格式: 年月日时分秒 + 6位随机数
     */
    public static String generateOrderNo() {
        return cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss")
                + cn.hutool.core.util.RandomUtil.randomNumbers(6);
    }

    /**
     * 生成支付流水号
     */
    public static String generatePaymentId() {
        return "PAY" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss")
                + cn.hutool.core.util.RandomUtil.randomNumbers(8);
    }

    /**
     * 生成退款单号
     */
    public static String generateRefundNo() {
        return "REF" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss")
                + cn.hutool.core.util.RandomUtil.randomNumbers(8);
    }

    /**
     * 简单Snowflake实现
     */
    private static class Snowflake {
        private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00
        private static final long WORKER_ID_BITS = 5L;
        private static final long DATA_CENTER_ID_BITS = 5L;
        private static final long SEQUENCE_BITS = 12L;

        private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

        private final long workerId;
        private final long dataCenterId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;

        public Snowflake() {
            this(1, 1);
        }

        public Snowflake(long workerId, long dataCenterId) {
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException("Worker ID must be between 0 and " + MAX_WORKER_ID);
            }
            if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
                throw new IllegalArgumentException("Data center ID must be between 0 and " + MAX_DATA_CENTER_ID);
            }
            this.workerId = workerId;
            this.dataCenterId = dataCenterId;
        }

        public synchronized long nextId() {
            long timestamp = System.currentTimeMillis();

            if (timestamp < lastTimestamp) {
                throw new RuntimeException("Clock moved backwards. Refusing to generate ID");
            }

            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1);
                if (sequence == 0) {
                    // 等待下一毫秒
                    timestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            return ((timestamp - EPOCH) << (SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS))
                    | (dataCenterId << (SEQUENCE_BITS + WORKER_ID_BITS))
                    | (workerId << SEQUENCE_BITS)
                    | sequence;
        }

        private long waitNextMillis(long lastTimestamp) {
            long timestamp = System.currentTimeMillis();
            while (timestamp <= lastTimestamp) {
                timestamp = System.currentTimeMillis();
            }
            return timestamp;
        }
    }
}
