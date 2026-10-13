import React, {useEffect, useMemo, useState} from 'react';
import {
  Alert,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {useNavigation, useRoute} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RouteProp} from '@react-navigation/native';
import type {CartStackParamList} from '../../navigation/types';
import type {CartItem, UserCoupon} from '../../types';
import {BorderRadius, Colors, Shadows, Spacing, Typography} from '../../constants';
import {Button, CouponSelector} from '../../components';
import {orderApi, userApi} from '../../services/api';
import type {UserIdentity} from '../../services/api/userApi';
import Loading from '../../components/common/Loading';
import {toast} from '../../components/common/Toast';

type CheckoutScreenNavigationProp = NativeStackNavigationProp<CartStackParamList>;
type CheckoutScreenRouteProp = RouteProp<CartStackParamList, 'Checkout'>;

interface Address {
  id: string;
  name: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault: boolean;
}

const mockAddress: Address = {
  id: 'addr-1',
  name: '张三',
  phone: '13800138000',
  province: '北京市',
  city: '北京市',
  district: '朝阳区',
  detail: '建国路 88 号',
  isDefault: true,
};

const mockCartItems: CartItem[] = [
  {
    id: 'cart-1',
    ticketId: 'ticket-1',
    ticketName: '周杰伦 2026 世界巡回演唱会',
    posterUrl: 'https://picsum.photos/240/320',
    venueName: '北京鸟巢体育馆',
    showTime: '2026-05-20 19:30:00',
    price: 580,
    priceLevel: '内场 A 区',
    priceLevelId: 'level-1',
    quantity: 2,
    maxQuantity: 4,
  },
];

function toApiId(value: string | number | undefined, fallback = 1) {
  const parsed = Number(value);
  if (Number.isFinite(parsed) && parsed > 0) {
    return parsed;
  }
  const match = String(value || '').match(/\d+/);
  return match ? Number(match[0]) : fallback;
}

function getCouponDiscount(orderAmount: number, coupon: UserCoupon | null) {
  if (!coupon) {
    return 0;
  }
  if (coupon.coupon.type === 'cash') {
    return coupon.coupon.value;
  }
  return Math.min(orderAmount, orderAmount * (coupon.coupon.value / 100));
}

const CheckoutScreen: React.FC = () => {
  const navigation = useNavigation<CheckoutScreenNavigationProp>();
  const route = useRoute<CheckoutScreenRouteProp>();
  const {cartIds} = route.params || {};

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [address] = useState<Address | null>(mockAddress);
  const [cartItems] = useState<CartItem[]>(mockCartItems);
  const [selectedCoupon, setSelectedCoupon] = useState<UserCoupon | null>(null);
  const [identities, setIdentities] = useState<UserIdentity[]>([]);
  const [selectedIdentityIds, setSelectedIdentityIds] = useState<Array<number | ''>>([]);
  const [remark, setRemark] = useState('');

  const originalAmount = useMemo(
    () => cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0),
    [cartItems],
  );
  const totalQuantity = useMemo(
    () => cartItems.reduce((sum, item) => sum + item.quantity, 0),
    [cartItems],
  );
  const couponDiscount = useMemo(
    () => getCouponDiscount(originalAmount, selectedCoupon),
    [originalAmount, selectedCoupon],
  );
  const finalAmount = Math.max(0, originalAmount - couponDiscount);

  useEffect(() => {
    setSelectedIdentityIds(current =>
      Array.from({length: totalQuantity}, (_, index) => current[index] || ''),
    );
  }, [totalQuantity]);

  useEffect(() => {
    setLoading(true);
    const identityRequest = userApi.getIdentities();
    identityRequest
      .then(result => setIdentities(result || []))
      .catch(error => {
        console.error('load identities failed', error);
        setIdentities([]);
      })
      .finally(() => setLoading(false));
  }, []);

  const handleSelectIdentity = (index: number, identityId: number | '') => {
    setSelectedIdentityIds(current =>
      current.map((value, itemIndex) => (itemIndex === index ? identityId : value)),
    );
  };

  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(
      date.getMinutes(),
    ).padStart(2, '0')}`;
  };

  const handleSubmit = () => {
    if (!address) {
      toast.error('请先填写联系人信息');
      return;
    }
    if (selectedIdentityIds.length !== totalQuantity || selectedIdentityIds.some(identityId => !identityId)) {
      toast.error(`请为 ${totalQuantity} 张票选择实名观演人`);
      return;
    }
    const uniqueIdentityIds = new Set(selectedIdentityIds.filter(Boolean));
    if (uniqueIdentityIds.size !== totalQuantity) {
      toast.error('同一订单内不能重复使用相同身份证');
      return;
    }

    Alert.alert('确认提交订单', `需支付 ¥${finalAmount.toFixed(2)}`, [
      {text: '取消', style: 'cancel'},
      {
        text: '提交',
        onPress: async () => {
          setSubmitting(true);
          try {
            let attendeeOffset = 0;
            const firstItem = cartItems[0];
            const order = await orderApi.createOrder({
              ticketId: toApiId(firstItem?.ticketId),
              sessionId: 1,
              items: cartItems.map(item => {
                const itemIdentityIds = selectedIdentityIds.slice(
                  attendeeOffset,
                  attendeeOffset + item.quantity,
                );
                attendeeOffset += item.quantity;
                return {
                  ticketId: toApiId(item.ticketId),
                  priceLevelId: toApiId(item.priceLevelId),
                  priceLevelName: item.priceLevel,
                  quantity: item.quantity,
                  unitPrice: item.price,
                  seatCode: item.seatCode,
                  attendees: itemIdentityIds.map(identityId => ({
                    identityId: Number(identityId),
                  })),
                };
              }),
              userCouponId: selectedCoupon?.id,
              contactName: address.name,
              contactPhone: address.phone.replace(/\D/g, ''),
              userRemark: remark || undefined,
              source: 'app',
            });

            toast.success('订单提交成功');
            (navigation.getParent() as any)?.navigate('Payment', {
              orderId: String(order.orderId || order.orderNo),
            });
          } catch (error: any) {
            toast.error(error?.message || '订单提交失败');
          } finally {
            setSubmitting(false);
          }
        },
      },
    ]);
  };

  if (loading) {
    return <Loading fullscreen text="加载中..." />;
  }

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>联系人</Text>
          <Text style={styles.contactName}>{address?.name || '--'}</Text>
          <Text style={styles.contactMeta}>{address?.phone || '--'}</Text>
          <Text style={styles.contactMeta}>
            {[address?.province, address?.city, address?.district, address?.detail].filter(Boolean).join(' ')}
          </Text>
        </View>

        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>票务清单</Text>
          {cartItems.map(item => (
            <View key={item.id} style={styles.itemRow}>
              <Image source={{uri: item.posterUrl}} style={styles.poster} />
              <View style={styles.itemBody}>
                <Text style={styles.itemTitle}>{item.ticketName}</Text>
                <Text style={styles.itemMeta}>{item.venueName}</Text>
                <Text style={styles.itemMeta}>{formatTime(item.showTime)}</Text>
                <View style={styles.itemFooter}>
                  <Text style={styles.itemPrice}>¥{item.price}</Text>
                  <Text style={styles.itemQuantity}>x{item.quantity}</Text>
                </View>
                <Text style={styles.itemLevel}>{item.priceLevel}</Text>
              </View>
            </View>
          ))}
          <Text style={styles.tipText}>cartIds: {cartIds?.join(', ') || 'mock'}</Text>
        </View>

        <View style={[styles.section, Shadows.sm]}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>实名观演人</Text>
            <Text style={styles.sectionHint}>需选择 {totalQuantity} 位</Text>
          </View>
          {identities.length === 0 ? (
            <Text style={styles.emptyText}>请先在“我的 - 常用观演人”里绑定身份证信息。</Text>
          ) : (
            selectedIdentityIds.map((identityId, index) => (
              <View key={index} style={styles.identityBlock}>
                <Text style={styles.identityTitle}>第 {index + 1} 位观演人</Text>
                <View style={styles.identityOptions}>
                  {identities.map(identity => {
                    const selected = identityId === identity.id;
                    const disabled = !selected && selectedIdentityIds.includes(identity.id);
                    return (
                      <TouchableOpacity
                        key={identity.id}
                        disabled={disabled}
                        activeOpacity={0.8}
                        style={[
                          styles.identityOption,
                          selected && styles.identityOptionSelected,
                          disabled && styles.identityOptionDisabled,
                        ]}
                        onPress={() => handleSelectIdentity(index, identity.id)}
                      >
                        <Text style={[styles.identityName, selected && styles.identityNameSelected]}>
                          {identity.realName}
                        </Text>
                        <Text style={styles.identityId}>{identity.maskedIdCardNo || identity.idCardNo}</Text>
                      </TouchableOpacity>
                    );
                  })}
                </View>
              </View>
            ))
          )}
        </View>

        <View style={[styles.section, Shadows.sm]}>
          <CouponSelector
            orderAmount={originalAmount}
            ticketId={toApiId(cartItems[0]?.ticketId)}
            selectedCouponId={selectedCoupon?.id}
            onSelect={setSelectedCoupon}
          />
        </View>

        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>备注</Text>
          <TextInput
            style={styles.remarkInput}
            placeholder="可填写特殊说明"
            placeholderTextColor={Colors.gray[400]}
            value={remark}
            onChangeText={setRemark}
            multiline
            maxLength={200}
          />
          <Text style={styles.remarkCount}>{remark.length}/200</Text>
        </View>

        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>金额明细</Text>
          <View style={styles.amountRow}>
            <Text style={styles.amountLabel}>原价</Text>
            <Text style={styles.amountValue}>¥{originalAmount.toFixed(2)}</Text>
          </View>
          <View style={styles.amountRow}>
            <Text style={styles.amountLabel}>优惠券</Text>
            <Text style={[styles.amountValue, styles.discountValue]}>-¥{couponDiscount.toFixed(2)}</Text>
          </View>
          <View style={[styles.amountRow, styles.totalRow]}>
            <Text style={styles.totalLabel}>应付</Text>
            <Text style={styles.totalValue}>¥{finalAmount.toFixed(2)}</Text>
          </View>
        </View>

        <View style={styles.bottomSpacer} />
      </ScrollView>

      <View style={[styles.footer, Shadows.lg]}>
        <View>
          <Text style={styles.footerLabel}>合计</Text>
          <Text style={styles.footerAmount}>¥{finalAmount.toFixed(2)}</Text>
        </View>
        <Button
          title={submitting ? '提交中...' : '提交订单'}
          onPress={handleSubmit}
          loading={submitting}
          style={styles.submitButton}
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
  scrollView: {
    flex: 1,
  },
  section: {
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    padding: Spacing.base,
    borderRadius: BorderRadius.md,
    backgroundColor: Colors.white,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.sm,
  },
  sectionTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  sectionHint: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  contactName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  contactMeta: {
    marginTop: 4,
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[600],
  },
  itemRow: {
    flexDirection: 'row',
    marginTop: Spacing.sm,
  },
  poster: {
    width: 76,
    height: 96,
    borderRadius: BorderRadius.sm,
    backgroundColor: Colors.gray[200],
  },
  itemBody: {
    flex: 1,
    marginLeft: Spacing.md,
  },
  itemTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  itemMeta: {
    marginTop: 4,
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  itemFooter: {
    marginTop: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  itemPrice: {
    fontSize: Typography.body.fontSize,
    color: Colors.error[500],
    fontWeight: '700',
  },
  itemQuantity: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  itemLevel: {
    marginTop: 4,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  tipText: {
    marginTop: Spacing.sm,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
  },
  emptyText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    lineHeight: 20,
  },
  identityBlock: {
    marginTop: Spacing.sm,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[100],
    paddingTop: Spacing.sm,
  },
  identityTitle: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '700',
    color: Colors.gray[700],
    marginBottom: Spacing.sm,
  },
  identityOptions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.sm,
  },
  identityOption: {
    width: '48%',
    borderWidth: 1,
    borderColor: Colors.gray[200],
    borderRadius: BorderRadius.sm,
    padding: Spacing.sm,
  },
  identityOptionSelected: {
    borderColor: Colors.primary[500],
    backgroundColor: Colors.primary[50],
  },
  identityOptionDisabled: {
    opacity: 0.4,
  },
  identityName: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  identityNameSelected: {
    color: Colors.primary[600],
  },
  identityId: {
    marginTop: 4,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  remarkInput: {
    minHeight: 84,
    textAlignVertical: 'top',
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  remarkCount: {
    marginTop: 8,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    textAlign: 'right',
  },
  amountRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.sm,
  },
  amountLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
  },
  amountValue: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  discountValue: {
    color: Colors.error[500],
  },
  totalRow: {
    borderTopWidth: 1,
    borderTopColor: Colors.gray[200],
    marginTop: Spacing.sm,
    paddingTop: Spacing.md,
  },
  totalLabel: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  totalValue: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  bottomSpacer: {
    height: 110,
  },
  footer: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.base,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[200],
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  footerLabel: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  footerAmount: {
    marginTop: 4,
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  submitButton: {
    minWidth: 148,
  },
});

export default CheckoutScreen;
