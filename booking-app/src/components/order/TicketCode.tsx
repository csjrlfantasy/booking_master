/**
 * 电子票展示组件
 * 展示票券二维码和票码
 */
import React, {useState} from 'react';
import {View, Text, StyleSheet, TouchableOpacity, Image} from 'react-native';
import {Colors, Typography, Spacing, BorderRadius} from '../../constants';

interface TicketCodeProps {
  code: string;
  codeUrl?: string;
  orderId: string;
  ticketName: string;
  venueName?: string;
  showTime?: string;
  seatCode?: string;
}

const TicketCode: React.FC<TicketCodeProps> = ({
  code,
  codeUrl,
  orderId,
  ticketName,
  venueName,
  showTime,
  seatCode,
}) => {
  const [enlarged, setEnlarged] = useState(false);

  // 复制票码
  const handleCopy = async () => {
    // 注意：实际项目中需要使用Clipboard API
    console.log('复制票码:', code);
  };

  return (
    <View style={styles.container}>
      {/* 演出信息 */}
      <View style={styles.header}>
        <Text style={styles.ticketName}>{ticketName}</Text>
        {venueName && <Text style={styles.venue}>📍 {venueName}</Text>}
        {showTime && <Text style={styles.time}>📅 {showTime}</Text>}
        {seatCode && <Text style={styles.seat}>🎫 {seatCode}</Text>}
      </View>

      {/* 二维码区域 */}
      <TouchableOpacity
        style={styles.qrContainer}
        onPress={() => setEnlarged(true)}
        activeOpacity={0.8}
      >
        {codeUrl ? (
          <Image
            source={{uri: codeUrl}}
            style={styles.qrCode}
            resizeMode="contain"
          />
        ) : (
          <View style={styles.qrPlaceholder}>
            <Text style={styles.qrPlaceholderText}>二维码加载中...</Text>
          </View>
        )}
        <Text style={styles.qrHint}>点击放大</Text>
      </TouchableOpacity>

      {/* 票码 */}
      <View style={styles.codeSection}>
        <Text style={styles.codeLabel}>取票码</Text>
        <View style={styles.codeRow}>
          <Text style={styles.code}>{formatCode(code)}</Text>
          <TouchableOpacity style={styles.copyBtn} onPress={handleCopy}>
            <Text style={styles.copyText}>复制</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* 提示信息 */}
      <View style={styles.notice}>
        <Text style={styles.noticeTitle}>入场须知</Text>
        <Text style={styles.noticeText}>• 请提前30分钟到达场馆</Text>
        <Text style={styles.noticeText}>• 入场时请出示电子票或取票码</Text>
        <Text style={styles.noticeText}>• 票券一经使用不可退换</Text>
      </View>

      {/* 订单号 */}
      <Text style={styles.orderNo}>订单号: {orderId}</Text>
    </View>
  );
};

// 格式化票码，每4位一组
const formatCode = (code: string): string => {
  return code.replace(/(.{4})/g, '$1 ').trim();
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    margin: Spacing.base,
  },
  header: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
    paddingBottom: Spacing.base,
    marginBottom: Spacing.base,
  },
  ticketName: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
    marginBottom: Spacing.sm,
  },
  venue: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
    marginTop: Spacing.xs,
  },
  time: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
    marginTop: Spacing.xs,
  },
  seat: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
    marginTop: Spacing.xs,
    fontWeight: '600',
  },
  qrContainer: {
    alignItems: 'center',
    paddingVertical: Spacing.xl,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    borderColor: Colors.gray[200],
  },
  qrCode: {
    width: 200,
    height: 200,
  },
  qrPlaceholder: {
    width: 200,
    height: 200,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.gray[100],
  },
  qrPlaceholderText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  qrHint: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.sm,
  },
  codeSection: {
    alignItems: 'center',
    marginTop: Spacing.lg,
    paddingTop: Spacing.lg,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[100],
  },
  codeLabel: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginBottom: Spacing.sm,
  },
  codeRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  code: {
    fontSize: 28,
    fontWeight: '700',
    color: Colors.gray[900],
    letterSpacing: 4,
    fontVariant: ['tabular-nums'],
  },
  copyBtn: {
    marginLeft: Spacing.lg,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    backgroundColor: Colors.primary[50],
    borderRadius: BorderRadius.sm,
  },
  copyText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.primary[500],
    fontWeight: '600',
  },
  notice: {
    marginTop: Spacing.lg,
    padding: Spacing.base,
    backgroundColor: Colors.info[100],
    borderRadius: BorderRadius.md,
  },
  noticeTitle: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '600',
    color: Colors.info[500],
    marginBottom: Spacing.sm,
  },
  noticeText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[700],
    lineHeight: 22,
  },
  orderNo: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    textAlign: 'center',
    marginTop: Spacing.lg,
  },
});

export default TicketCode;
