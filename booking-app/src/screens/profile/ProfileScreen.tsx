import React, {useCallback, useEffect, useState} from 'react';
import {
  Alert,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {CommonActions, useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import Loading from '../../components/common/Loading';
import type {RootStackParamList} from '../../navigation/types';
import {userApi, type UserProfileResponse} from '../../services/api/userApi';
import {couponApi} from '../../services/api';
import {userStorage} from '../../services/storage';
import {useAppDispatch, useAppSelector} from '../../store/hooks';
import {logout} from '../../store/slices/authSlice';
import {setCoupons} from '../../store/slices/userSlice';

type ProfileScreenNavigationProp = NativeStackNavigationProp<RootStackParamList>;

interface MenuItem {
  id: string;
  icon: string;
  title: string;
  badge?: number;
  onPress?: () => void;
}

const memberLevelColor: Record<string, string> = {
  普通会员: Colors.gray[500],
  白银会员: '#8A94A6',
  黄金会员: '#D99A00',
  VIP会员: Colors.primary[500],
};

const ProfileScreen: React.FC = () => {
  const navigation = useNavigation<ProfileScreenNavigationProp>();
  const dispatch = useAppDispatch();
  const {user} = useAppSelector(state => state.auth);
  const {coupons, favoriteTickets} = useAppSelector(state => state.user);
  const [userProfile, setUserProfile] = useState<UserProfileResponse | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);

  const availableCouponCount = coupons.filter(coupon => coupon.status === 'available').length;
  const favoriteCount = favoriteTickets.length;

  const loadUserProfile = useCallback(async () => {
    if (!user) {
      return;
    }
    setProfileLoading(true);
    try {
      const profile = await userApi.getUserProfile();
      setUserProfile(profile);
    } catch (error) {
      console.error('加载用户资料失败:', error);
      setUserProfile({
        userId: Number(user.id) || 0,
        username: user.username,
        phone: user.phone,
        email: user.email,
        nickname: user.nickname,
        avatar: user.avatar,
        memberLevel: user.memberLevel || '普通会员',
        points: user.points || 0,
        totalOrderAmount: 0,
        totalOrders: 0,
      });
    } finally {
      setProfileLoading(false);
    }
  }, [user]);

  useEffect(() => {
    loadUserProfile();
  }, [loadUserProfile]);

  useEffect(() => {
    if (!user) {
      dispatch(setCoupons([]));
      return;
    }
    const couponSummaryRequest = couponApi.getUserCoupons({status: 'available', page: 1, pageSize: 50});
    couponSummaryRequest
      .then(result => dispatch(setCoupons(result.list || [])))
      .catch(error => {
        console.error('load coupon summary failed', error);
        dispatch(setCoupons([]));
      });
  }, [dispatch, user]);

  const handleLogout = useCallback(() => {
    Alert.alert('退出登录', '确定要退出当前账号吗？', [
      {text: '取消', style: 'cancel'},
      {
        text: '退出',
        style: 'destructive',
        onPress: () => {
          userStorage.clearAuth();
          dispatch(logout());
          navigation.dispatch(
            CommonActions.reset({
              index: 0,
              routes: [{name: 'Auth'}],
            }),
          );
        },
      },
    ]);
  }, [dispatch, navigation]);

  const handleGoToOrders = useCallback((status?: string) => {
    navigation.navigate('Order', {
      screen: 'OrderList',
      params: {filter: status},
    });
  }, [navigation]);

  const handleGoToCoupons = useCallback(() => {
    navigation.navigate('Profile', {
      screen: 'Coupon',
      params: {mode: 'available'},
    });
  }, [navigation]);

  const handleGoToFavorites = useCallback(() => {
    navigation.navigate('Profile', {
      screen: 'Favorite',
    });
  }, [navigation]);

  const handleGoToIdentities = useCallback(() => {
    navigation.navigate('Profile', {
      screen: 'Identity',
    });
  }, [navigation]);

  const handleGoToSettings = useCallback(() => {
    navigation.navigate('Profile', {
      screen: 'Settings',
    });
  }, [navigation]);

  const orderMenus: MenuItem[] = [
    {id: 'pending', icon: '待', title: '待付款', onPress: () => handleGoToOrders('PENDING_PAYMENT')},
    {id: 'paid', icon: '票', title: '待使用', onPress: () => handleGoToOrders('TICKET_ISSUED')},
    {id: 'completed', icon: '完', title: '已完成', onPress: () => handleGoToOrders('COMPLETED')},
    {id: 'refund', icon: '退', title: '退款/售后', onPress: () => handleGoToOrders('REFUNDED')},
  ];

  const serviceMenus: MenuItem[] = [
    {id: 'identity', icon: '证', title: '常用观演人', onPress: handleGoToIdentities},
    {id: 'coupon', icon: '券', title: '我的优惠券', badge: availableCouponCount, onPress: handleGoToCoupons},
    {id: 'favorite', icon: '藏', title: '我的收藏', badge: favoriteCount, onPress: handleGoToFavorites},
    {id: 'address', icon: '址', title: '收货地址', onPress: () => Alert.alert('提示', '地址管理功能开发中')},
    {id: 'settings', icon: '设', title: '设置', onPress: handleGoToSettings},
  ];

  const displayName = userProfile?.nickname || userProfile?.username || user?.username || '游客用户';
  const memberLevel = userProfile?.memberLevel || '普通会员';
  const memberColor = memberLevelColor[memberLevel] || Colors.primary[500];
  const avatarUri = userProfile?.avatar || user?.avatar;

  const renderMenuItem = (item: MenuItem, isLast: boolean) => (
    <View key={item.id}>
      <TouchableOpacity style={styles.menuItem} onPress={item.onPress} activeOpacity={0.75}>
        <View style={styles.menuIconBox}>
          <Text style={styles.menuIcon}>{item.icon}</Text>
        </View>
        <Text style={styles.menuTitle}>{item.title}</Text>
        {item.badge != null && item.badge > 0 ? (
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{item.badge > 99 ? '99+' : item.badge}</Text>
          </View>
        ) : null}
        <Text style={styles.menuArrow}>›</Text>
      </TouchableOpacity>
      {!isLast ? <View style={styles.menuDivider} /> : null}
    </View>
  );

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {profileLoading ? <Loading fullscreen text="加载中..." /> : null}
      <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
        <View style={[styles.userCard, Shadows.md]}>
          <View style={styles.userInfo}>
            {avatarUri ? (
              <Image source={{uri: avatarUri}} style={styles.avatar} />
            ) : (
              <View style={styles.avatarPlaceholder}>
                <Text style={styles.avatarText}>{displayName.charAt(0).toUpperCase()}</Text>
              </View>
            )}
            <View style={styles.userDetail}>
              <Text style={styles.username}>{displayName}</Text>
              <Text style={styles.userId}>ID: {userProfile?.userId || user?.id || '未登录'}</Text>
              {(userProfile?.phone || user?.phone) ? (
                <Text style={styles.phone}>
                  {(userProfile?.phone || user?.phone || '').replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')}
                </Text>
              ) : null}
            </View>
          </View>

          {user ? (
            <View style={styles.memberInfo}>
              <Text style={[styles.memberLevel, {color: memberColor}]}>{memberLevel}</Text>
              <View style={styles.statsContainer}>
                <View style={styles.statItem}>
                  <Text style={styles.statValue}>{userProfile?.points || 0}</Text>
                  <Text style={styles.statLabel}>积分</Text>
                </View>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                  <Text style={styles.statValue}>¥{(userProfile?.totalOrderAmount || 0).toFixed(0)}</Text>
                  <Text style={styles.statLabel}>累计消费</Text>
                </View>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                  <Text style={styles.statValue}>{userProfile?.totalOrders || 0}</Text>
                  <Text style={styles.statLabel}>订单数</Text>
                </View>
              </View>
            </View>
          ) : (
            <TouchableOpacity
              style={styles.loginButton}
              onPress={() => navigation.navigate('Auth', {screen: 'Login'})}
            >
              <Text style={styles.loginButtonText}>登录 / 注册</Text>
            </TouchableOpacity>
          )}
        </View>

        <View style={[styles.sectionCard, Shadows.sm]}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>我的订单</Text>
            <TouchableOpacity onPress={() => handleGoToOrders()}>
              <Text style={styles.seeAll}>查看全部 ›</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.orderMenuGrid}>
            {orderMenus.map(item => (
              <TouchableOpacity key={item.id} style={styles.orderMenuItem} onPress={item.onPress}>
                <View style={styles.orderIconBox}>
                  <Text style={styles.orderIcon}>{item.icon}</Text>
                </View>
                <Text style={styles.orderTitle}>{item.title}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={[styles.sectionCard, Shadows.sm]}>
          <Text style={styles.sectionTitle}>常用服务</Text>
          <View style={styles.menuList}>
            {serviceMenus.map((item, index) => renderMenuItem(item, index === serviceMenus.length - 1))}
          </View>
        </View>

        {user ? (
          <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
            <Text style={styles.logoutText}>退出登录</Text>
          </TouchableOpacity>
        ) : null}

        <Text style={styles.version}>版本 1.0.0</Text>
      </ScrollView>
    </SafeAreaView>
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
  userCard: {
    backgroundColor: Colors.primary[500],
    margin: Spacing.md,
    borderRadius: BorderRadius.lg,
    padding: Spacing.lg,
  },
  userInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 2,
    borderColor: Colors.white,
    marginRight: Spacing.base,
  },
  avatarPlaceholder: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: Colors.primary[300],
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Spacing.base,
  },
  avatarText: {
    fontSize: 28,
    fontWeight: '700',
    color: Colors.white,
  },
  userDetail: {
    flex: 1,
  },
  username: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.white,
    marginBottom: 4,
  },
  userId: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[100],
  },
  phone: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.primary[100],
    marginTop: 2,
  },
  memberInfo: {
    marginTop: Spacing.base,
    paddingTop: Spacing.base,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255,255,255,0.22)',
  },
  memberLevel: {
    textAlign: 'center',
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    marginBottom: Spacing.sm,
  },
  statsContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
  },
  statValue: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '700',
    color: Colors.white,
  },
  statLabel: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[100],
    marginTop: 2,
  },
  statDivider: {
    width: 1,
    height: 30,
    backgroundColor: 'rgba(255,255,255,0.22)',
  },
  loginButton: {
    alignSelf: 'flex-start',
    marginTop: Spacing.md,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.sm,
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
  },
  loginButtonText: {
    color: Colors.primary[500],
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
  },
  sectionCard: {
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginBottom: Spacing.md,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.md,
  },
  sectionTitle: {
    fontSize: Typography.h4.fontSize,
    color: Colors.gray[900],
    fontWeight: '700',
  },
  seeAll: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  orderMenuGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  orderMenuItem: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: Spacing.sm,
  },
  orderIconBox: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.primary[50],
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.xs,
  },
  orderIcon: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.primary[500],
    fontWeight: '700',
  },
  orderTitle: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[700],
  },
  menuList: {
    marginTop: Spacing.sm,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.md,
  },
  menuIconBox: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: Colors.gray[100],
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Spacing.md,
  },
  menuIcon: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[700],
    fontWeight: '700',
  },
  menuTitle: {
    flex: 1,
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  badge: {
    minWidth: 20,
    height: 20,
    paddingHorizontal: 6,
    borderRadius: 10,
    backgroundColor: Colors.error[500],
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Spacing.sm,
  },
  badgeText: {
    fontSize: 11,
    color: Colors.white,
    fontWeight: '700',
  },
  menuArrow: {
    fontSize: 20,
    color: Colors.gray[400],
  },
  menuDivider: {
    height: 1,
    backgroundColor: Colors.gray[100],
    marginLeft: 46,
  },
  logoutButton: {
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.sm,
    paddingVertical: Spacing.base,
    alignItems: 'center',
  },
  logoutText: {
    color: Colors.error[500],
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
  },
  version: {
    textAlign: 'center',
    color: Colors.gray[400],
    fontSize: Typography.caption.fontSize,
    marginTop: Spacing.xl,
    marginBottom: Spacing.xxl,
  },
});

export default ProfileScreen;
