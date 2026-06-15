/**
 * CheckoutScreen - 订单确认页（结算页面）
 * 功能：订单商品预览、收货地址选择/编辑、优惠券选择、价格明细、提交订单
 */
import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  TextInput,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Image,
  Alert,
} from 'react-native';
import {useNavigation, useRoute} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RouteProp} from '@react-navigation/native';
import type {RootStackParamList, CartStackParamList} from '../../navigation/types';
import type {CartItem, UserCoupon} from '../../types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {orderApi, userApi} from '../../services/api';
import type {UserIdentity} from '../../services/api/userApi';
import {Button} from '../../components';
import Loading from '../../components/common/Loading';
import {toast} from '../../components/common/Toast';

type CheckoutScreenNavigationProp = NativeStackNavigationProp<CartStackParamList>;
type CheckoutScreenRouteProp = RouteProp<CartStackParamList, 'Checkout'>;

// Mock收货地址
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
  phone: '138****8888',
  province: '北京市',
  city: '北京市',
  district: '朝阳区',
  detail: '建国路88号SOHO现代城',
  isDefault: true,
};

// Mock可用优惠券
const mockCoupons: UserCoupon[] = [
  {
    id: 'uc-1',
    couponId: 'c-1',
    coupon: {
      id: 'c-1',
      name: '满100减10券',
      description: '限购买票使用',
      type: 'cash',
      value: 10,
      minAmount: 100,
      startTime: '2026-01-01',
      endTime: '2026-12-31',
      status: 'available',
    },
    userId: 'user-1',
    status: 'available',
    receiveTime: '2026-01-01',
  },
  {
    id: 'uc-2',
    couponId: 'c-2',
    coupon: {
      id: 'c-2',
      name: '9折优惠券',
      description: '最高减50元',
      type: 'discount',
      value: 10,
      maxDiscount: 50,
      startTime: '2026-01-01',
      endTime: '2026-12-31',
      status: 'available',
    },
    userId: 'user-1',
    status: 'available',
    receiveTime: '2026-01-01',
  },
];

// Mock购物车商品（实际应该从Redux获取）
const mockCartItems: CartItem[] = [
  {
    id: 'cart-1',
    ticketId: 'ticket-1',
    ticketName: '周杰伦2026世界巡回演唱会',
    posterUrl: 'https://picsum.photos/200/300',
    venueName: '北京鸟巢体育馆',
    showTime: '2026-05-20 19:30:00',
    price: 580,
    priceLevel: '内场A区',
    priceLevelId: 'level-1',
    quantity: 2,
    maxQuantity: 4,
  },
];

const CheckoutScreen: React.FC = () => {
  const navigation = useNavigation<CheckoutScreenNavigationProp>();
  const route = useRoute<CheckoutScreenRouteProp>();
  const {cartIds} = route.params || {};

  // 状态
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [address, setAddress] = useState<Address | null>(mockAddress);
  const [cartItems, setCartItems] = useState<CartItem[]>(mockCartItems);
  const [availableCoupons, setAvailableCoupons] = useState<UserCoupon[]>(mockCoupons);
  const [selectedCoupon, setSelectedCoupon] = useState<UserCoupon | null>(null);
  const [identities, setIdentities] = useState<UserIdentity[]>([]);
  const [selectedIdentityIds, setSelectedIdentityIds] = useState<Array<number | ''>>([]);
  const [remark, setRemark] = useState('');

  // 计算价格
  const originalAmount = cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const totalQuantity = cartItems.reduce((sum, item) => sum + item.quantity, 0);
  
  // 计算优惠券优惠
  const discountAmount = useCallback(() => {
    if (!selectedCoupon) return 0;
    const coupon = selectedCoupon.coupon;
    if (coupon.type === 'cash') {
      return coupon.value;
    } else if (coupon.type === 'discount') {
      // 9折优惠
      return originalAmount - (originalAmount * (10 - coupon.value) / 100);
    }
    return 0;
  }, [selectedCoupon, originalAmount]);

  const couponDiscount = discountAmount();
  const freight = 0; // 电子票无运费
  const finalAmount = originalAmount - couponDiscount + freight;

  // 加载数据
  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    setSelectedIdentityIds(current =>
      Array.from({length: totalQuantity}, (_, index) => current[index] || ''),
    );
  }, [totalQuantity]);

  const loadData = async () => {
    setLoading(true);
    try {
      const identityList = await userApi.getIdentities();
      setIdentities(identityList || []);
      // TODO: API联调
      // const [addressRes, couponRes] = await Promise.all([
      //   userApi.getDefaultAddress(),
      //   couponApi.getUserCoupons({status: 'available'}),
      // ]);
      // setAddress(addressRes);
      // setAvailableCoupons(couponRes.list);
    } catch (error) {
      console.error('加载结算数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const toApiId = (value: string | number | undefined, fallback = 1) => {
    const parsed = Number(value);
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed;
    }
    const match = String(value || '').match(/\d+/);
    return match ? Number(match[0]) : fallback;
  };

  const handleSelectIdentity = (index: number, identityId: number | '') => {
    setSelectedIdentityIds(current =>
      current.map((value, itemIndex) => (itemIndex === index ? identityId : value)),
    );
  };

  // 选择优惠券
  const handleSelectCoupon = () => {
    if (availableCoupons.length === 0) {
      toast.info('暂无可用优惠券');
      return;
    }

    Alert.alert(
      '选择优惠券',
      '',
      [
        {
          text: '不使用优惠券',
          onPress: () => setSelectedCoupon(null),
        },
        ...availableCoupons.map(coupon => ({
          text: coupon.coupon.name,
          onPress: () => setSelectedCoupon(coupon),
        })),
        {text: '取消', style: 'cancel'},
      ]
    );
  };

  // 编辑收货地址
  const handleEditAddress = () => {
    Alert.alert(
      '提示',
      '地址编辑功能开发中',
      [{text: '确定'}]
    );
  };

  // 提交订单
  const handleSubmit = async () => {
    if (!address) {
      toast.error('请选择收货地址');
      return;
    }
    if (selectedIdentityIds.length !== totalQuantity || selectedIdentityIds.some(identityId => !identityId)) {
      toast.error(`请为 ${totalQuantity} 张票选择常用观演人`);
      return;
    }
    const uniqueIdentityIds = new Set(selectedIdentityIds.filter(Boolean));
    if (uniqueIdentityIds.size !== totalQuantity) {
      toast.error('同一订单内观演人身份证不能重复');
      return;
    }

    Alert.alert(
      '确认提交订单',
      `订单金额：¥${finalAmount.toFixed(2)}`,
      [
        {text: '取消', style: 'cancel'},
        {
          text: '确认',
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
                couponId: selectedCoupon?.couponId,
                contactName: address.name,
                contactPhone: address.phone.replace(/\D/g, ''),
                userRemark: remark || undefined,
                source: 'app',
              });
              
              toast.success('订单提交成功');
              // 跳转到支付页面
              navigation.getParent()?.navigate('Payment' as any, {
                orderId: String(order.orderId || order.orderNo),
              });
            } catch (error: any) {
              toast.error(error.message || '订单提交失败');
            } finally {
              setSubmitting(false);
            }
          },
        },
      ]
    );
  };

  // 格式化时间
  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
  };

  if (loading) {
    return <Loading fullscreen text="加载中..." />;
  }

  return (
    <View style={styles.container}>
      <ScrollView 
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
      >
        {/* 收货地址 */}
        <View style={[styles.section, Shadows.sm]}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>📦 收货信息</Text>
          </View>
          {address ? (
            <TouchableOpacity onPress={handleEditAddress}>
              <View style={styles.addressContent}>
                <View style={styles.addressInfo}>
                  <Text style={styles.addressName}>{address.name}</Text>
                  <Text style={styles.addressPhone}>{address.phone}</Text>
                </View>
                <Text style={styles.addressDetail}>
                  {address.province}{address.city}{address.district}{address.detail}
                </Text>
              </View>
              <Text style={styles.editHint}>点击编辑</Text>
            </TouchableOpacity>
          ) : (
            <TouchableOpacity style={styles.addAddress}>
              <Text style={styles.addAddressText}>+ 添加收货地址</Text>
            </TouchableOpacity>
          )}
        </View>

        {/* 温馨提示：电子票无需物流 */}
        <View style={styles.tipsContainer}>
          <Text style={styles.tipsText}>🎫 电子票：购买后可直接在订单中查看电子票</Text>
        </View>

        {/* 商品清单 */}
        <View style={[styles.section, Shadows.sm]}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>🎟️ 商品清单</Text>
            <Text style={styles.goodsCount}>共{cartItems.length}件</Text>
          </View>
          
          {cartItems.map((item, index) => (
            <View 
              key={item.id} 
              style={[
                styles.goodsItem,
                index < cartItems.length - 1 && styles.goodsItemBorder
              ]}
            >
              <Image source={{uri: item.posterUrl}} style={styles.goodsPoster} />
              <View style={styles.goodsInfo}>
                <Text style={styles.goodsName} numberOfLines={2}>
                  {item.ticketName}
                </Text>
                <Text style={styles.goodsVenue}>📍 {item.venueName}</Text>
                <Text style={styles.goodsTime}>🕐 {formatTime(item.showTime)}</Text>
                <View style={styles.goodsBottom}>
                  <Text style={styles.goodsPrice}>¥{item.price}</Text>
                  <Text style={styles.goodsQuantity}>×{item.quantity}</Text>
                </View>
                <Text style={styles.goodsLevel}>{item.priceLevel}</Text>
              </View>
            </View>
          ))}
        </View>

        {/* 常用观演人 */}
        <View style={[styles.section, Shadows.sm]}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>常用观演人</Text>
            <Text style={styles.goodsCount}>需选择{totalQuantity}人</Text>
          </View>
          {identities.length === 0 ? (
            <View style={styles.identityEmpty}>
              <Text style={styles.identityEmptyTitle}>还没有保存身份证</Text>
              <Text style={styles.identityEmptyText}>
                请先到“我的-常用观演人”添加身份证，购票实名制会使用这里的信息。
              </Text>
            </View>
          ) : (
            <View style={styles.identityList}>
              {selectedIdentityIds.map((identityId, index) => (
                <View key={index} style={styles.identitySlot}>
                  <Text style={styles.identitySlotTitle}>第{index + 1}位观演人</Text>
                  <View style={styles.identityOptions}>
                    {identities.map(identity => {
                      const selected = identityId === identity.id;
                      const disabled = !selected && selectedIdentityIds.includes(identity.id);
                      return (
                        <TouchableOpacity
                          key={identity.id}
                          style={[
                            styles.identityOption,
                            selected && styles.identityOptionSelected,
                            disabled && styles.identityOptionDisabled,
                          ]}
                          activeOpacity={0.75}
                          disabled={disabled}
                          onPress={() => handleSelectIdentity(index, identity.id)}
                        >
                          <Text
                            style={[
                              styles.identityOptionName,
                              selected && styles.identityOptionNameSelected,
                            ]}
                          >
                            {identity.realName}
                          </Text>
                          <Text style={styles.identityOptionId}>
                            {identity.maskedIdCardNo || identity.idCardNo}
                          </Text>
                        </TouchableOpacity>
                      );
                    })}
                  </View>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* 优惠券 */}
        <View style={[styles.section, Shadows.sm]}>
          <TouchableOpacity 
            style={styles.couponRow}
            onPress={handleSelectCoupon}
          >
            <View style={styles.couponLeft}>
              <Text style={styles.couponLabel}>🎫 优惠券</Text>
              {selectedCoupon ? (
                <Text style={styles.couponName}>{selectedCoupon.coupon.name}</Text>
              ) : (
                <Text style={styles.couponEmpty}>
                  {availableCoupons.length > 0 
                    ? `${availableCoupons.length}张可用` 
                    : '暂无可用'}
                </Text>
              )}
            </View>
            <View style={styles.couponRight}>
              <Text style={styles.couponValue}>
                {selectedCoupon ? `-¥${couponDiscount.toFixed(2)}` : ''}
              </Text>
              <Text style={styles.arrow}>›</Text>
            </View>
          </TouchableOpacity>
        </View>

        {/* 订单备注 */}
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>📝 订单备注</Text>
          <View style={styles.remarkContainer}>
            <TextInput
              style={styles.remarkInput}
              placeholder="选填，可备注您的特殊需求"
              placeholderTextColor={Colors.gray[400]}
              value={remark}
              onChangeText={setRemark}
              multiline
              maxLength={200}
            />
            <Text style={styles.remarkCount}>{remark.length}/200</Text>
          </View>
        </View>

        {/* 价格明细 */}
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>💰 价格明细</Text>
          
          <View style={styles.priceRow}>
            <Text style={styles.priceLabel}>商品总价</Text>
            <Text style={styles.priceValue}>¥{originalAmount.toFixed(2)}</Text>
          </View>
          
          <View style={styles.priceRow}>
            <Text style={styles.priceLabel}>优惠券</Text>
            <Text style={[styles.priceValue, styles.discountText]}>
              {couponDiscount > 0 ? `-¥${couponDiscount.toFixed(2)}` : '¥0.00'}
            </Text>
          </View>
          
          <View style={styles.priceRow}>
            <Text style={styles.priceLabel}>运费</Text>
            <Text style={styles.priceValue}>
              {freight > 0 ? `¥${freight.toFixed(2)}` : '免费'}
            </Text>
          </View>
          
          <View style={[styles.priceRow, styles.totalRow]}>
            <Text style={styles.totalLabel}>实付金额</Text>
            <Text style={styles.totalValue}>¥{finalAmount.toFixed(2)}</Text>
          </View>
        </View>

        {/* 底部占位 */}
        <View style={{height: 100}} />
      </ScrollView>

      {/* 底部提交栏 */}
      <View style={[styles.footer, Shadows.lg]}>
        <View style={styles.footerAmount}>
          <Text style={styles.footerLabel}>实付:</Text>
          <Text style={styles.footerMoney}>¥{finalAmount.toFixed(2)}</Text>
        </View>
        <Button
          title={submitting ? '提交中...' : '提交订单'}
          variant="primary"
          size="large"
          loading={submitting}
          onPress={handleSubmit}
          style={styles.submitBtn}
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
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  sectionTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  goodsCount: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  // 地址样式
  addressContent: {
    paddingVertical: Spacing.sm,
  },
  addressInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  addressName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginRight: Spacing.md,
  },
  addressPhone: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  addressDetail: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    lineHeight: 20,
  },
  editHint: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[500],
    textAlign: 'right',
  },
  addAddress: {
    paddingVertical: Spacing.lg,
    alignItems: 'center',
  },
  addAddressText: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
  },
  tipsContainer: {
    marginHorizontal: Spacing.md,
    marginTop: Spacing.sm,
  },
  tipsText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.info[500],
  },
  // 商品样式
  goodsItem: {
    flexDirection: 'row',
    paddingVertical: Spacing.sm,
  },
  goodsItemBorder: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
  },
  goodsPoster: {
    width: 70,
    height: 90,
    borderRadius: BorderRadius.sm,
    backgroundColor: Colors.gray[200],
  },
  goodsInfo: {
    flex: 1,
    marginLeft: Spacing.md,
  },
  goodsName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    lineHeight: 20,
  },
  goodsVenue: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  goodsTime: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[500],
    marginTop: 2,
  },
  goodsBottom: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: Spacing.xs,
  },
  goodsPrice: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  goodsQuantity: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  goodsLevel: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  identityEmpty: {
    backgroundColor: Colors.gray[100],
    borderRadius: BorderRadius.sm,
    padding: Spacing.base,
  },
  identityEmptyTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  identityEmptyText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    lineHeight: 20,
    marginTop: Spacing.xs,
  },
  identityList: {
    gap: Spacing.md,
  },
  identitySlot: {
    borderTopWidth: 1,
    borderTopColor: Colors.gray[100],
    paddingTop: Spacing.md,
  },
  identitySlotTitle: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '600',
    color: Colors.gray[700],
    marginBottom: Spacing.sm,
  },
  identityOptions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.sm,
  },
  identityOption: {
    minWidth: '47%',
    borderWidth: 1,
    borderColor: Colors.gray[200],
    borderRadius: BorderRadius.sm,
    backgroundColor: Colors.white,
    padding: Spacing.sm,
  },
  identityOptionSelected: {
    borderColor: Colors.primary[500],
    backgroundColor: Colors.primary[50],
  },
  identityOptionDisabled: {
    opacity: 0.45,
  },
  identityOptionName: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  identityOptionNameSelected: {
    color: Colors.primary[600],
  },
  identityOptionId: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  // 优惠券样式
  couponRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
  },
  couponLeft: {
    flex: 1,
  },
  couponLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
    marginBottom: 4,
  },
  couponName: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.error[500],
  },
  couponEmpty: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[400],
  },
  couponRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  couponValue: {
    fontSize: Typography.body.fontSize,
    color: Colors.error[500],
    marginRight: Spacing.xs,
  },
  arrow: {
    fontSize: 20,
    color: Colors.gray[400],
  },
  // 备注样式
  remarkContainer: {
    marginTop: Spacing.sm,
  },
  remarkInput: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
    minHeight: 80,
    textAlignVertical: 'top',
  },
  remarkCount: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    textAlign: 'right',
  },
  // 价格明细样式
  priceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
  },
  priceLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  priceValue: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  discountText: {
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
    fontWeight: '600',
    color: Colors.gray[900],
  },
  totalValue: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  // 底部提交栏
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[200],
  },
  footerAmount: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  footerLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  footerMoney: {
    fontSize: Typography.h2.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
    marginLeft: Spacing.xs,
  },
  submitBtn: {
    paddingHorizontal: Spacing.xl,
  },
});

export default CheckoutScreen;
