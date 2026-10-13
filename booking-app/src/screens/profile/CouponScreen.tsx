import React, {useCallback, useEffect, useState} from 'react';
import {
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {useRoute} from '@react-navigation/native';
import type {RouteProp} from '@react-navigation/native';
import type {ProfileStackParamList} from '../../navigation/types';
import {BorderRadius, Colors, Shadows, Spacing, Typography} from '../../constants';
import {couponApi, type CouponCenterItem} from '../../services/api/couponApi';
import type {UserCoupon} from '../../types';
import Loading from '../../components/common/Loading';
import {toast} from '../../components/common/Toast';

type CouponRouteProp = RouteProp<ProfileStackParamList, 'Coupon'>;
type CouponTab = 'available' | 'used' | 'expired';

const tabs: Array<{key: CouponTab; label: string}> = [
  {key: 'available', label: '可用'},
  {key: 'used', label: '已用'},
  {key: 'expired', label: '失效'},
];

function couponValueText(coupon: UserCoupon | CouponCenterItem) {
  const couponType =
    'coupon' in coupon
      ? coupon.coupon.type
      : coupon.couponType === 2
        ? 'discount'
        : 'cash';
  const couponValue =
    'coupon' in coupon ? coupon.coupon.value : coupon.couponValue;
  return couponType === 'discount' ? `${couponValue} 折` : `¥${couponValue}`;
}

const CouponScreen: React.FC = () => {
  const route = useRoute<CouponRouteProp>();
  const [tab, setTab] = useState<CouponTab>(route.params?.mode || 'available');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [myCoupons, setMyCoupons] = useState<UserCoupon[]>([]);
  const [centerCoupons, setCenterCoupons] = useState<CouponCenterItem[]>([]);

  const loadData = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
    }
    try {
      const [myCouponRes, centerRes] = await Promise.all([
        couponApi.getUserCoupons({status: tab, page: 1, pageSize: 50}),
        couponApi.getCouponCenter({page: 1, pageSize: 20}),
      ]);
      setMyCoupons(myCouponRes.list || []);
      setCenterCoupons(centerRes.list || []);
    } catch (error: any) {
      toast.error(error?.message || '加载优惠券失败');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [tab]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleReceive = async (couponId: string) => {
    try {
      await couponApi.receiveCoupon(couponId);
      toast.success('领取成功');
      loadData(true);
    } catch (error: any) {
      toast.error(error?.message || '领取失败');
    }
  };

  if (loading) {
    return <Loading fullscreen text="加载优惠券..." />;
  }

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => {
        setRefreshing(true);
        loadData(true);
      }} />}
    >
      <View style={[styles.card, Shadows.sm]}>
        <View style={styles.tabRow}>
          {tabs.map(item => (
            <TouchableOpacity
              key={item.key}
              style={[styles.tab, item.key === tab && styles.tabActive]}
              onPress={() => setTab(item.key)}
            >
              <Text style={[styles.tabText, item.key === tab && styles.tabTextActive]}>
                {item.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {myCoupons.length === 0 ? (
          <Text style={styles.emptyText}>当前分类下还没有优惠券。</Text>
        ) : (
          myCoupons.map(coupon => (
            <View key={coupon.id} style={styles.couponItem}>
              <View style={styles.couponMain}>
                <Text style={styles.couponTitle}>{coupon.coupon.name}</Text>
                <Text style={styles.couponMeta}>
                  {coupon.coupon.minAmount ? `满 ${coupon.coupon.minAmount} 可用` : '无门槛'} · 截止{' '}
                  {coupon.coupon.endTime?.slice(0, 10) || '--'}
                </Text>
              </View>
              <Text style={styles.couponValue}>{couponValueText(coupon)}</Text>
            </View>
          ))
        )}
      </View>

      <View style={[styles.card, Shadows.sm]}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>领券中心</Text>
          <Text style={styles.sectionHint}>可直接领取后台发放的优惠券</Text>
        </View>

        {centerCoupons.length === 0 ? (
          <Text style={styles.emptyText}>暂无可领取优惠券。</Text>
        ) : (
          centerCoupons.map(coupon => (
            <View key={coupon.couponId} style={styles.couponItem}>
              <View style={styles.couponMain}>
                <Text style={styles.couponTitle}>{coupon.couponName}</Text>
                <Text style={styles.couponMeta}>
                  {coupon.minOrderAmount ? `满 ${coupon.minOrderAmount} 可用` : '无门槛'} · 剩余 {coupon.remainCount || 0}
                </Text>
              </View>
              <TouchableOpacity
                style={[styles.claimButton, coupon.received && styles.claimButtonDisabled]}
                disabled={coupon.received}
                onPress={() => handleReceive(coupon.couponId)}
              >
                <Text style={styles.claimButtonText}>{coupon.received ? '已领取' : couponValueText(coupon)}</Text>
              </TouchableOpacity>
            </View>
          ))
        )}
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  card: {
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    padding: Spacing.base,
    borderRadius: BorderRadius.md,
    backgroundColor: Colors.white,
  },
  tabRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
    marginBottom: Spacing.base,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.gray[200],
    borderRadius: BorderRadius.sm,
    paddingVertical: Spacing.sm,
  },
  tabActive: {
    borderColor: Colors.primary[500],
    backgroundColor: Colors.primary[50],
  },
  tabText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
  },
  tabTextActive: {
    color: Colors.primary[600],
    fontWeight: '700',
  },
  sectionHeader: {
    marginBottom: Spacing.sm,
  },
  sectionTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  sectionHint: {
    marginTop: 4,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  couponItem: {
    borderWidth: 1,
    borderColor: Colors.gray[200],
    borderRadius: BorderRadius.sm,
    padding: Spacing.base,
    marginTop: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: Spacing.base,
  },
  couponMain: {
    flex: 1,
  },
  couponTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  couponMeta: {
    marginTop: 4,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  couponValue: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  claimButton: {
    backgroundColor: Colors.primary[500],
    borderRadius: BorderRadius.sm,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
  },
  claimButtonDisabled: {
    backgroundColor: Colors.gray[300],
  },
  claimButtonText: {
    color: Colors.white,
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
  },
  emptyText: {
    paddingVertical: Spacing.xl,
    textAlign: 'center',
    color: Colors.gray[500],
    fontSize: Typography.body.fontSize,
  },
});

export default CouponScreen;
