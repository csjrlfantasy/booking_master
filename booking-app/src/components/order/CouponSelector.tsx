import React, {useEffect, useMemo, useState} from 'react';
import {
  FlatList,
  Modal,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {Colors, Typography, Spacing, BorderRadius} from '../../constants';
import type {UserCoupon} from '../../types';
import {couponApi} from '../../services/api';

interface CouponSelectorProps {
  orderAmount: number;
  ticketId?: number;
  selectedCouponId?: string;
  onSelect: (coupon: UserCoupon | null) => void;
}

function getCouponDiscount(coupon: UserCoupon, orderAmount: number) {
  if (coupon.coupon.type === 'cash') {
    return coupon.coupon.value;
  }
  const discountAmount = orderAmount * (coupon.coupon.value / 100);
  return Math.min(orderAmount, discountAmount);
}

const CouponSelector: React.FC<CouponSelectorProps> = ({
  orderAmount,
  ticketId,
  selectedCouponId,
  onSelect,
}) => {
  const [visible, setVisible] = useState(false);
  const [coupons, setCoupons] = useState<UserCoupon[]>([]);

  useEffect(() => {
    if (!visible) {
      return;
    }

    const availableCouponRequest = couponApi.getAvailableCoupons({
      orderAmount,
      ticketId,
    });
    availableCouponRequest
      .then(setCoupons)
      .catch(error => {
        console.error('load available coupons failed', error);
        setCoupons([]);
      });
  }, [visible, orderAmount, ticketId]);

  const selectedCoupon = useMemo(
    () => coupons.find(item => item.id === selectedCouponId),
    [coupons, selectedCouponId],
  );

  return (
    <View>
      <TouchableOpacity
        style={styles.selector}
        activeOpacity={0.75}
        onPress={() => setVisible(true)}
      >
        <View>
          <Text style={styles.selectorTitle}>优惠券</Text>
          <Text style={styles.selectorHint}>
            {selectedCoupon ? selectedCoupon.coupon.name : coupons.length > 0 ? `${coupons.length}张可用` : '选择可用优惠券'}
          </Text>
        </View>
        <Text style={styles.selectorValue}>
          {selectedCoupon ? `-¥${getCouponDiscount(selectedCoupon, orderAmount).toFixed(2)}` : '去选择'}
        </Text>
      </TouchableOpacity>

      <Modal visible={visible} transparent animationType="slide" onRequestClose={() => setVisible(false)}>
        <View style={styles.overlay}>
          <View style={styles.sheet}>
            <View style={styles.header}>
              <Text style={styles.headerTitle}>选择优惠券</Text>
              <TouchableOpacity onPress={() => setVisible(false)}>
                <Text style={styles.closeText}>关闭</Text>
              </TouchableOpacity>
            </View>

            <TouchableOpacity
              style={[styles.item, !selectedCouponId && styles.itemSelected]}
              onPress={() => {
                onSelect(null);
                setVisible(false);
              }}
            >
              <Text style={styles.itemTitle}>不使用优惠券</Text>
              <Text style={styles.itemMeta}>原价支付</Text>
            </TouchableOpacity>

            <FlatList
              data={coupons}
              keyExtractor={item => item.id}
              renderItem={({item}) => {
                const selected = item.id === selectedCouponId;
                return (
                  <TouchableOpacity
                    style={[styles.item, selected && styles.itemSelected]}
                    activeOpacity={0.8}
                    onPress={() => {
                      onSelect(item);
                      setVisible(false);
                    }}
                  >
                    <View style={styles.itemLeft}>
                      <Text style={styles.itemTitle}>{item.coupon.name}</Text>
                      <Text style={styles.itemMeta}>
                        满{item.coupon.minAmount || 0}可用 · 截止 {item.coupon.endTime?.slice(0, 10) || '--'}
                      </Text>
                    </View>
                    <Text style={styles.itemValue}>-¥{getCouponDiscount(item, orderAmount).toFixed(2)}</Text>
                  </TouchableOpacity>
                );
              }}
              ListEmptyComponent={<Text style={styles.emptyText}>暂无可用优惠券</Text>}
            />
          </View>
        </View>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  selector: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    borderColor: Colors.gray[200],
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.base,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  selectorTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  selectorHint: {
    marginTop: 4,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  selectorValue: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
    fontWeight: '700',
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.36)',
    justifyContent: 'flex-end',
  },
  sheet: {
    maxHeight: '72%',
    backgroundColor: Colors.white,
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    paddingBottom: Spacing.lg,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.base,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
  },
  headerTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  closeText: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
  },
  item: {
    marginHorizontal: Spacing.base,
    marginTop: Spacing.base,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    borderColor: Colors.gray[200],
    padding: Spacing.base,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  itemSelected: {
    borderColor: Colors.primary[500],
    backgroundColor: Colors.primary[50],
  },
  itemLeft: {
    flex: 1,
    paddingRight: Spacing.base,
  },
  itemTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  itemMeta: {
    marginTop: 4,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  itemValue: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  emptyText: {
    textAlign: 'center',
    color: Colors.gray[500],
    paddingVertical: Spacing.xl,
    fontSize: Typography.body.fontSize,
  },
});

export default CouponSelector;
