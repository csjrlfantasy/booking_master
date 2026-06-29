/**
 * PaymentScreen - 支付页面
 */
import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';
import {useNavigation, useRoute} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RouteProp} from '@react-navigation/native';
import type {RootStackParamList} from '../../navigation/types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {Button} from '../../components';
import {toast} from '../../components/common/Toast';
import {paymentApi} from '../../services/api';
import {orderApi} from '../../services/api';

type PaymentScreenNavigationProp = NativeStackNavigationProp<RootStackParamList>;
type PaymentScreenRouteProp = RouteProp<RootStackParamList, 'Payment'>;

// 支付方式
interface PaymentMethod {
  id: string;
  name: string;
  icon: string;
  description: string;
}

const paymentMethods: PaymentMethod[] = [
  {id: 'wechat', name: '微信支付', icon: '💳', description: '推荐'},
  {id: 'alipay', name: '支付宝', icon: '💰', description: ''},
  {id: 'bankcard', name: '银行卡支付', icon: '🏦', description: ''},
];

const PaymentScreen: React.FC = () => {
  const navigation = useNavigation<PaymentScreenNavigationProp>();
  const route = useRoute<PaymentScreenRouteProp>();
  const {orderId} = route.params;

  const [selectedMethod, setSelectedMethod] = useState<string>('wechat');
  const [countdown, setCountdown] = useState(15 * 60); // 15分钟倒计时
  const [loading, setLoading] = useState(false);
  const [orderInfo, setOrderInfo] = useState<{
    totalAmount: number;
    paidAmount: number;
    orderNo: string;
  } | null>(null);
  const [paymentCreated, setPaymentCreated] = useState(false);

  // 加载订单信息
  useEffect(() => {
    loadOrderInfo();
  }, [orderId]);

  const loadOrderInfo = async () => {
    try {
      const order = await orderApi.getOrderDetail(orderId);
      setOrderInfo({
        totalAmount: order.totalAmount,
        paidAmount: order.paidAmount || order.totalAmount,
        orderNo: order.orderNo,
      });
    } catch (error) {
      console.error('加载订单信息失败:', error);
      // 使用mock数据作为后备
      setOrderInfo({
        totalAmount: 0.01,
        paidAmount: 0.01,
        orderNo: orderId,
      });
    }
  };

  // 倒计时
  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          handlePaymentTimeout();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [navigation]);

  // 支付超时处理
  const handlePaymentTimeout = useCallback(() => {
    Alert.alert('支付超时', '订单已取消，请重新下单', [
      {
        text: '确定',
        onPress: () => {
          navigation.goBack();
        },
      },
    ]);
  }, [navigation]);

  // 格式化时间
  const formatCountdown = () => {
    const minutes = Math.floor(countdown / 60);
    const seconds = countdown % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  // 创建支付订单
  const createPayment = async () => {
    try {
      const result = await paymentApi.createPayment({
        orderId,
        paymentMethod: selectedMethod as 'wechat' | 'alipay' | 'bankcard',
      });
      return result;
    } catch (error: any) {
      console.error('创建支付订单失败:', error);
      // 如果后端API暂未实现，使用mock成功响应
      return {
        paymentId: 'pay-' + Date.now(),
        orderId,
        amount: orderInfo?.paidAmount || 0.01,
        expireTime: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      };
    }
  };

  // 轮询支付结果
  const pollPaymentResult = async (maxAttempts: number = 30): Promise<boolean> => {
    for (let i = 0; i < maxAttempts; i++) {
      try {
        const result = await paymentApi.queryPaymentResult(orderId);
        if (result.paid) {
          return true;
        }
      } catch (error) {
        console.error('查询支付结果失败:', error);
      }
      // 等待2秒
      await new Promise(resolve => setTimeout(resolve, 2000));
    }
    return false;
  };

  // 确认支付
  const handleConfirmPay = async () => {
    Alert.alert(
      '确认支付',
      `即将使用${paymentMethods.find(m => m.id === selectedMethod)?.name}支付`,
      [
        {text: '取消', style: 'cancel'},
        {
          text: '确定',
          onPress: async () => {
            setLoading(true);
            try {
              // 1. 创建支付订单
              await createPayment();

              // 2. 模拟支付流程（实际应跳转到第三方支付）
              // 这里假设支付立即成功，实际项目中需要根据支付SDK回调
              toast.loading('支付处理中...');

              // 3. 更新订单状态
              await orderApi.payOrder({
                orderId,
                paymentMethod: selectedMethod as 'wechat' | 'alipay' | 'bankcard',
              });

              toast.hideLoading();

              // 4. 跳转到支付结果页
              navigation.replace('PaymentResult', {orderId, success: true});
            } catch (error: any) {
              toast.hideLoading();
              toast.error(error.message || '支付失败，请重试');

              // 即使API调用失败，也跳转到结果页（可能是支付成功但回调失败）
              navigation.replace('PaymentResult', {orderId, success: false});
            } finally {
              setLoading(false);
            }
          },
        },
      ]
    );
  };

  // 取消支付
  const handleCancelPayment = () => {
    Alert.alert(
      '取消支付',
      '确定要取消当前支付吗？',
      [
        {text: '继续支付', style: 'cancel'},
        {
          text: '确定取消',
          style: 'destructive',
          onPress: async () => {
            try {
              await orderApi.cancelOrder({orderId, reason: '用户取消支付'});
              toast.success('订单已取消');
              navigation.goBack();
            } catch (error) {
              console.error('取消订单失败:', error);
              navigation.goBack();
            }
          },
        },
      ]
    );
  };

  return (
    <View style={styles.container}>
      {/* 订单信息 */}
      <View style={[styles.orderInfo, Shadows.sm]}>
        <Text style={styles.orderLabel}>待支付金额</Text>
        <Text style={styles.orderAmount}>
          ¥{orderInfo?.paidAmount?.toFixed(2) || '0.01'}
        </Text>
        <Text style={styles.orderNo}>订单号: {orderInfo?.orderNo || orderId}</Text>
        <View style={styles.countdownContainer}>
          <Text style={styles.countdownLabel}>剩余支付时间:</Text>
          <Text style={[styles.countdown, countdown <= 60 && styles.countdownUrgent]}>
            {formatCountdown()}
          </Text>
        </View>
      </View>

      {/* 支付方式 */}
      <View style={[styles.paymentSection, Shadows.sm]}>
        <Text style={styles.sectionTitle}>选择支付方式</Text>

        {paymentMethods.map(method => (
          <TouchableOpacity
            key={method.id}
            style={[
              styles.paymentMethod,
              selectedMethod === method.id && styles.paymentMethodSelected,
            ]}
            onPress={() => setSelectedMethod(method.id)}
            activeOpacity={0.7}
          >
            <View style={styles.methodLeft}>
              <Text style={styles.methodIcon}>{method.icon}</Text>
              <View>
                <Text style={styles.methodName}>{method.name}</Text>
                {method.description && (
                  <Text style={styles.methodDesc}>{method.description}</Text>
                )}
              </View>
            </View>
            <View
              style={[
                styles.radio,
                selectedMethod === method.id && styles.radioSelected,
              ]}
            >
              {selectedMethod === method.id && (
                <View style={styles.radioInner} />
              )}
            </View>
          </TouchableOpacity>
        ))}
      </View>

      {/* 安全提示 */}
      <View style={styles.securityTips}>
        <Text style={styles.securityIcon}>🔒</Text>
        <Text style={styles.securityText}>支付安全由微信/支付宝提供保障</Text>
      </View>

      {/* 底部按钮 */}
      <View style={[styles.footer, Shadows.lg]}>
        <TouchableOpacity
          style={styles.cancelBtn}
          onPress={handleCancelPayment}
          disabled={loading}
        >
          <Text style={styles.cancelBtnText}>取消</Text>
        </TouchableOpacity>
        <Button
          title={loading ? '支付中...' : '确认支付'}
          variant="primary"
          size="large"
          onPress={handleConfirmPay}
          disabled={loading}
          loading={loading}
          style={styles.confirmBtn}
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  orderInfo: {
    backgroundColor: Colors.white,
    margin: Spacing.md,
    padding: Spacing.xl,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
  },
  orderLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
  },
  orderAmount: {
    fontSize: 40,
    fontWeight: '700',
    color: Colors.error[500],
    marginVertical: Spacing.sm,
  },
  orderNo: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.sm,
  },
  countdownContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: Spacing.sm,
  },
  countdownLabel: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  countdown: {
    fontSize: Typography.body.fontSize,
    color: Colors.warning[500],
    fontWeight: '600',
    marginLeft: Spacing.xs,
  },
  countdownUrgent: {
    color: Colors.error[500],
  },
  paymentSection: {
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    padding: Spacing.base,
    borderRadius: BorderRadius.md,
  },
  sectionTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.md,
  },
  paymentMethod: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
  },
  paymentMethodSelected: {
    backgroundColor: Colors.primary[50],
    marginHorizontal: -Spacing.base,
    paddingHorizontal: Spacing.base,
    borderRadius: BorderRadius.sm,
  },
  methodLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  methodIcon: {
    fontSize: 28,
    marginRight: Spacing.md,
  },
  methodName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '500',
    color: Colors.gray[900],
  },
  methodDesc: {
    fontSize: Typography.caption.fontSize,
    color: Colors.success[500],
    marginTop: 2,
  },
  radio: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
    borderColor: Colors.gray[300],
    justifyContent: 'center',
    alignItems: 'center',
  },
  radioSelected: {
    borderColor: Colors.primary[500],
  },
  radioInner: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: Colors.primary[500],
  },
  securityTips: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: Spacing.xl,
    paddingHorizontal: Spacing.md,
  },
  securityIcon: {
    fontSize: 14,
    marginRight: Spacing.xs,
  },
  securityText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    backgroundColor: Colors.white,
    padding: Spacing.base,
    paddingBottom: Spacing.lg,
  },
  cancelBtn: {
    flex: 1,
    height: 48,
    borderRadius: BorderRadius.sm,
    borderWidth: 1,
    borderColor: Colors.gray[300],
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: Spacing.md,
  },
  cancelBtnText: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[700],
  },
  confirmBtn: {
    flex: 2,
  },
});

export default PaymentScreen;
