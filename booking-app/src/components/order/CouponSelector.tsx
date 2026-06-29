/**
 * 优惠券选择器组件
 */
import React, {useState, useEffect} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  Modal,
} from 'react-native';
import {Colors, Typography, Spacing, BorderRadius} from '../../constants';
import type {UserCoupon, Coupon} from '../../types';
import {couponApi} from '../../services/api';

interface CouponSelectorProps {
  orderAmount: number;
  selectedCouponId?: string;
  onSelect: (coupon: UserCoupon | null) => void;
}

const CouponSelector: React.FC<CouponSelectorProps> = ({
  orderAmount,
  selectedCouponId,
  onSelect,
}) => {
  const [visible, setVisible] = useState(false);
  const [coupons, setCoupons] = useState<UserCoupon[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (visible) {
      fetchCoupons();
    }
  }, [visible]);

  const fetchCoupons = async () => {
    setLoading(true);
    try {
      const result = await couponApi.getUserCoupons({status: 'available'});
      // 过滤出可用的优惠券
      const availableCoupons = result.list.filter((coupon) => {
        if (coupon.coupon.minAmount && orderAmount < coupon.coupon.minAmount) {
          return false;
        }
        return true;
      });
      setCoupons(availableCoupons);
    } catch (error) {
      console.error('获取优惠券失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (coupon: UserCoupon | null) => {
    onSelect(coupon);
    setVisible(false);
  };

  const selectedCoupon = coupons.find((c) => c.id === selectedCouponId);

  // 计算优惠金额
  const calculateDiscount = (coupon: Coupon): number => {
    if (coupon.type === 'cash') {
      return coupon.value;
    } else if (coupon.type === 'discount') {
      const discount = orderAmount * (1 - coupon.value / 100);
      const maxDiscount = coupon.maxDiscount || Infinity;
      return Math.min(discount, maxDiscount);
    }
    return 0;
  };

  return (
    <View>
      {/* 选择按钮 */}
      <TouchableOpacity
        style={styles.selector}
        onPress={() => setVisible(true)}
        activeOpacity={0.7}
      >
        <View style={styles.selectorLeft}>
          <Text style={styles.selectorIcon}>🎫</Text>
          <Text style={styles.selectorTitle}>优惠券</Text>
        </View>
        <View style={styles.selectorRight}>
          {selectedCoupon ? (
            <Text style={styles.selectedText}>
              -¥{calculateDiscount(selectedCoupon.coupon).toFixed(2)}
            </Text>
          ) : (
            <Text style={styles.placeholderText}>
              {coupons.length > 0 ? `${coupons.length}张可用` : '暂无可用'}
            </Text>
          )}
          <Text style={styles.arrow}>›</Text>
        </View>
      </TouchableOpacity>

      {/* 优惠券选择弹窗 */}
      <Modal
        visible={visible}
        animationType="slide"
        transparent
        onRequestClose={() => setVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            {/* 标题栏 */}
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>选择优惠券</Text>
              <TouchableOpacity
                style={styles.closeBtn}
                onPress={() => setVisible(false)}
              >
                <Text style={styles.closeText}>✕</Text>
              </TouchableOpacity>
            </View>

            {/* 不使用优惠券选项 */}
            <TouchableOpacity
              style={[
                styles.couponItem,
                !selectedCouponId && styles.couponItemSelected,
              ]}
              onPress={() => handleSelect(null)}
            >
              <View style={styles.couponLeft}>
                <Text style={styles.noCouponText}>不使用优惠券</Text>
              </View>
              <View style={styles.couponRight}>
                <View style={styles.radio}>
                  {!selectedCouponId && <View style={styles.radioInner} />}
                </View>
              </View>
            </TouchableOpacity>

            {/* 优惠券列表 */}
            <FlatList
              data={coupons}
              keyExtractor={(item) => item.id}
              style={styles.couponList}
              renderItem={({item}) => {
                const discount = calculateDiscount(item.coupon);
                const isSelected = item.id === selectedCouponId;
                const isDisabled = item.coupon.minAmount && orderAmount < item.coupon.minAmount;

                return (
                  <TouchableOpacity
                    style={[
                      styles.couponItem,
                      isSelected ? styles.couponItemSelected : undefined,
                      isDisabled ? styles.couponItemDisabled : undefined,
                    ]}
                    onPress={() => !isDisabled && handleSelect(item)}
                    disabled={!!isDisabled}
                  >
                    <View style={styles.couponLeft}>
                      <View style={styles.couponValue}>
                        {item.coupon.type === 'cash' ? (
                          <Text style={styles.couponAmount}>
                            ¥{item.coupon.value}
                          </Text>
                        ) : (
                          <Text style={styles.couponAmount}>
                            {item.coupon.value}折
                          </Text>
                        )}
                      </View>
                      <View style={styles.couponInfo}>
                        <Text style={styles.couponName}>{item.coupon.name}</Text>
                        {item.coupon.minAmount && (
                          <Text style={styles.couponRule}>
                            满¥{item.coupon.minAmount}可用
                          </Text>
                        )}
                        <Text style={styles.couponTime}>
                          有效期至: {item.coupon.endTime.split('T')[0]}
                        </Text>
                      </View>
                    </View>
                    <View style={styles.couponRight}>
                      <View style={[styles.radio, isSelected && styles.radioSelected]}>
                        {isSelected && <View style={styles.radioInner} />}
                      </View>
                    </View>
                  </TouchableOpacity>
                );
              }}
              ListEmptyComponent={
                <View style={styles.emptyContainer}>
                  <Text style={styles.emptyText}>暂无可用优惠券</Text>
                </View>
              }
            />
          </View>
        </View>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  selector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.base,
    borderRadius: BorderRadius.md,
  },
  selectorLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  selectorIcon: {
    fontSize: 18,
    marginRight: Spacing.sm,
  },
  selectorTitle: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  selectorRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  selectedText: {
    fontSize: Typography.body.fontSize,
    color: Colors.error[500],
    fontWeight: '600',
    marginRight: Spacing.xs,
  },
  placeholderText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
    marginRight: Spacing.xs,
  },
  arrow: {
    fontSize: 20,
    color: Colors.gray[400],
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: Colors.white,
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    maxHeight: '70%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.base,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
  },
  modalTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  closeBtn: {
    width: 30,
    height: 30,
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeText: {
    fontSize: 20,
    color: Colors.gray[500],
  },
  couponList: {
    flex: 1,
  },
  couponItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.base,
    marginHorizontal: Spacing.base,
    marginVertical: Spacing.sm,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    borderColor: Colors.gray[200],
  },
  couponItemSelected: {
    borderColor: Colors.primary[500],
    backgroundColor: Colors.primary[50],
  },
  couponItemDisabled: {
    opacity: 0.5,
  },
  couponLeft: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  couponValue: {
    width: 70,
    height: 70,
    backgroundColor: Colors.secondary[500],
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  couponAmount: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.white,
  },
  couponInfo: {
    flex: 1,
    marginLeft: Spacing.md,
  },
  couponName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  couponRule: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  couponTime: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  couponRight: {
    marginLeft: Spacing.md,
  },
  noCouponText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  radio: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
    borderColor: Colors.gray[300],
    alignItems: 'center',
    justifyContent: 'center',
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
  emptyContainer: {
    alignItems: 'center',
    paddingVertical: Spacing['3xl'],
  },
  emptyText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
  },
});

export default CouponSelector;
