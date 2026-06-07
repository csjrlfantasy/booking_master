/**
 * ProfileScreen - 用户中心页面
 */
import React, {useCallback, useEffect, useState} from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Image,
  Alert,
} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import {CommonActions} from '@react-navigation/native';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {useAppSelector, useAppDispatch} from '../../store/hooks';
import {logout, setUser} from '../../store/slices/authSlice';
import {userStorage} from '../../services/storage';
import type {RootStackParamList} from '../../navigation/types';
import {userApi, type UserProfileResponse} from '../../services/api/userApi';
import Loading from '../../components/common/Loading';

type ProfileScreenNavigationProp = NativeStackNavigationProp<RootStackParamList>;

// 会员等级配置
const MEMBER_LEVEL_CONFIG: Record<string, {color: string; icon: string}> = {
  '普通会员': {color: '#999', icon: '⭐'},
  '白银会员': {color: '#C0C0C0', icon: '🥈'},
  '黄金会员': {color: '#FFD700', icon: '🏆'},
  '铂金会员': {color: '#E5E4E2', icon: '💎'},
  '钻石会员': {color: '#B9F2FF', icon: '💠'},
  'VIP会员': {color: '#FF6B35', icon: '👑'},
  '企业用户': {color: '#4A90D9', icon: '🏢'},
};

// 菜单项配置
interface MenuItem {
  id: string;
  icon: string;
  title: string;
  badge?: number;
  onPress?: () => void;
}

const ProfileScreen: React.FC = () => {
  const navigation = useNavigation<ProfileScreenNavigationProp>();
  const dispatch = useAppDispatch();
  const {user} = useAppSelector(state => state.auth);
  const {coupons, favoriteTickets} = useAppSelector(state => state.user);

  // 用户资料状态 (Sprint 4 新增)
  const [userProfile, setUserProfile] = useState<UserProfileResponse | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);

  // 计算可用优惠券数量
  const availableCouponCount = coupons.filter(c => c.status === 'available').length;
  // 计算收藏数量
  const favoriteCount = favoriteTickets.length;

  // 加载用户资料 (Sprint 4 新增)
  useEffect(() => {
    if (user) {
      loadUserProfile();
    }
  }, [user]);

  const loadUserProfile = async () => {
    setProfileLoading(true);
    try {
      const profile = await userApi.getUserProfile();
      setUserProfile(profile);
    } catch (error) {
      console.error('加载用户资料失败:', error);
      // 使用本地存储的用户信息作为后备
      if (user) {
        setUserProfile({
          userId: parseInt(user.id, 10) || 0,
          username: user.username,
          phone: user.phone,
          email: user.email,
          nickname: user.nickname,
          avatar: user.avatar,
          memberLevel: user.memberLevel || '普通会员',
          points: 0,
          totalOrderAmount: 0,
          totalOrders: 0,
        });
      }
    } finally {
      setProfileLoading(false);
    }
  };

  // 获取会员等级配置
  const getMemberLevelConfig = (levelName?: string) => {
    const key = levelName || '普通会员';
    return MEMBER_LEVEL_CONFIG[key] || MEMBER_LEVEL_CONFIG['普通会员'];
  };

  // 退出登录
  const handleLogout = useCallback(() => {
    Alert.alert(
      '退出登录',
      '确定要退出登录吗？',
      [
        {text: '取消', style: 'cancel'},
        {
          text: '确定',
          style: 'destructive',
          onPress: () => {
            // 清除本地存储
            userStorage.clearAuth();
            // 清除Redux状态
            dispatch(logout());
            // 跳转到登录页
            navigation.dispatch(
              CommonActions.reset({
                index: 0,
                routes: [{name: 'Auth'}],
              })
            );
          },
        },
      ]
    );
  }, [navigation, dispatch]);

  // 跳转到订单列表
  const handleGoToOrders = useCallback((status?: string) => {
    navigation.navigate('Order', {
      screen: 'OrderList',
      params: {filter: status},
    });
  }, [navigation]);

  // 跳转到优惠券
  const handleGoToCoupons = useCallback(() => {
    navigation.navigate('Profile', {
      screen: 'Coupon',
      params: {mode: 'available'},
    });
  }, [navigation]);

  // 跳转到收藏
  const handleGoToFavorites = useCallback(() => {
    navigation.navigate('Profile', {
      screen: 'Favorite',
    });
  }, [navigation]);

  // 跳转到设置
  const handleGoToSettings = useCallback(() => {
    navigation.navigate('Profile', {
      screen: 'Settings',
    });
  }, [navigation]);

  // 订单菜单
  const orderMenus: MenuItem[] = [
    {id: 'pending', icon: '⏳', title: '待付款', onPress: () => handleGoToOrders('PENDING_PAYMENT')},
    {id: 'paid', icon: '🎫', title: '待使用', onPress: () => handleGoToOrders('TICKET_ISSUED')},
    {id: 'completed', icon: '✅', title: '已完成', onPress: () => handleGoToOrders('COMPLETED')},
    {id: 'refund', icon: '💰', title: '退款/售后', onPress: () => handleGoToOrders('REFUNDED')},
  ];

  // 服务菜单
  const serviceMenus: MenuItem[] = [
    {id: 'coupon', icon: '🎫', title: '我的优惠券', badge: availableCouponCount, onPress: handleGoToCoupons},
    {id: 'favorite', icon: '❤️', title: '我的收藏', badge: favoriteCount, onPress: handleGoToFavorites},
    {id: 'address', icon: '📍', title: '收货地址', onPress: () => Alert.alert('提示', '地址管理功能开发中')},
    {id: 'settings', icon: '⚙️', title: '设置', onPress: handleGoToSettings},
  ];

  // 渲染菜单项
  const renderMenuItem = (item: MenuItem) => (
    <TouchableOpacity
      key={item.id}
      style={styles.menuItem}
      onPress={item.onPress}
      activeOpacity={0.7}
    >
      <Text style={styles.menuIcon}>{item.icon}</Text>
      <Text style={styles.menuTitle}>{item.title}</Text>
      {item.badge !== undefined && item.badge > 0 && (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{item.badge > 99 ? '99+' : item.badge}</Text>
        </View>
      )}
      <Text style={styles.menuArrow}>›</Text>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {profileLoading && <Loading fullscreen text="加载中..." />}
      <ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
        bounces={false}
      >
        {/* 用户信息卡片 */}
        <View style={[styles.userCard, Shadows.md]}>
          <View style={styles.userInfo}>
            <View style={styles.avatarContainer}>
              {userProfile?.avatar || user?.avatar ? (
                <Image
                  source={{uri: userProfile?.avatar || user?.avatar}}
                  style={styles.avatar}
                />
              ) : (
                <View style={styles.avatarPlaceholder}>
                  <Text style={styles.avatarText}>
                    {userProfile?.nickname?.charAt(0)?.toUpperCase() ||
                     userProfile?.username?.charAt(0)?.toUpperCase() ||
                     'U'}
                  </Text>
                </View>
              )}
            </View>
            <View style={styles.userDetail}>
              <Text style={styles.username}>
                {userProfile?.nickname || userProfile?.username || user?.username || '游客用户'}
              </Text>
              <Text style={styles.userId}>
                ID: {userProfile?.userId || user?.id || '未登录'}
              </Text>
              {(userProfile?.phone || user?.phone) && (
                <Text style={styles.phone}>
                  {userProfile?.phone?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') || user?.phone}
                </Text>
              )}
            </View>
          </View>

          {/* Sprint 4 新增：会员等级和积分 */}
          {userProfile && (
            <View style={styles.memberInfo}>
              <View style={styles.memberLevel}>
                <Text style={styles.memberIcon}>
                  {getMemberLevelConfig(userProfile.memberLevel).icon}
                </Text>
                <Text style={[
                  styles.memberLevelText,
                  {color: getMemberLevelConfig(userProfile.memberLevel).color}
                ]}>
                  {userProfile.memberLevel || '普通会员'}
                </Text>
              </View>
              <View style={styles.statsContainer}>
                <View style={styles.statItem}>
                  <Text style={styles.statValue}>{userProfile.points || 0}</Text>
                  <Text style={styles.statLabel}>积分</Text>
                </View>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                  <Text style={styles.statValue}>
                    ¥{(userProfile.totalOrderAmount || 0).toFixed(0)}
                  </Text>
                  <Text style={styles.statLabel}>累计消费</Text>
                </View>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                  <Text style={styles.statValue}>{userProfile.totalOrders || 0}</Text>
                  <Text style={styles.statLabel}>订单数</Text>
                </View>
              </View>
            </View>
          )}

          {!user && (
            <TouchableOpacity
              style={styles.loginBtn}
              onPress={() => navigation.navigate('Auth', {screen: 'Login'})}
            >
              <Text style={styles.loginBtnText}>登录/注册</Text>
            </TouchableOpacity>
          )}
        </View>

        {/* 订单入口 */}
        <View style={[styles.orderSection, Shadows.sm]}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>我的订单</Text>
            <TouchableOpacity onPress={() => handleGoToOrders()}>
              <Text style={styles.seeAll}>查看全部 ›</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.orderMenuGrid}>
            {orderMenus.map(item => (
              <TouchableOpacity
                key={item.id}
                style={styles.orderMenuItem}
                onPress={item.onPress}
                activeOpacity={0.7}
              >
                <Text style={styles.orderIcon}>{item.icon}</Text>
                <Text style={styles.orderTitle}>{item.title}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* 服务菜单 */}
        <View style={[styles.serviceSection, Shadows.sm]}>
          <Text style={styles.sectionTitle}>常用服务</Text>
          {serviceMenus.map((item, index) => (
            <View key={item.id}>
              {renderMenuItem(item)}
              {index < serviceMenus.length - 1 && <View style={styles.menuDivider} />}
            </View>
          ))}
        </View>

        {/* 退出登录按钮 */}
        {user && (
          <TouchableOpacity
            style={styles.logoutBtn}
            onPress={handleLogout}
            activeOpacity={0.7}
          >
            <Text style={styles.logoutText}>退出登录</Text>
          </TouchableOpacity>
        )}

        {/* 版本信息 */}
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
  // 用户信息卡片
  userCard: {
    backgroundColor: Colors.primary[500],
    margin: Spacing.md,
    borderRadius: BorderRadius.lg,
    padding: Spacing.xl,
    paddingTop: Spacing.lg,
  },
  userInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatarContainer: {
    marginRight: Spacing.base,
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 2,
    borderColor: Colors.white,
  },
  avatarPlaceholder: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: Colors.primary[300],
    alignItems: 'center',
    justifyContent: 'center',
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
    color: Colors.primary[200],
  },
  phone: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.primary[200],
    marginTop: 2,
  },
  // Sprint 4 新增：会员信息
  memberInfo: {
    marginTop: Spacing.base,
    paddingTop: Spacing.base,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.2)',
  },
  memberLevel: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.sm,
  },
  memberIcon: {
    fontSize: 16,
    marginRight: Spacing.xs,
  },
  memberLevelText: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
  },
  statsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  statItem: {
    alignItems: 'center',
    flex: 1,
  },
  statValue: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '700',
    color: Colors.white,
  },
  statLabel: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[200],
    marginTop: 2,
  },
  statDivider: {
    width: 1,
    height: 30,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
  },
  loginBtn: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.sm,
    paddingVertical: Spacing.sm,
    paddingHorizontal: Spacing.lg,
    alignSelf: 'flex-start',
    marginTop: Spacing.md,
  },
  loginBtnText: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.primary[500],
  },
  // 订单区块
  orderSection: {
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginBottom: Spacing.md,
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
  seeAll: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  orderMenuGrid: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  orderMenuItem: {
    alignItems: 'center',
    paddingVertical: Spacing.sm,
  },
  orderIcon: {
    fontSize: 28,
    marginBottom: Spacing.xs,
  },
  orderTitle: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[700],
  },
  // 服务菜单
  serviceSection: {
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginBottom: Spacing.md,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.md,
  },
  menuIcon: {
    fontSize: 20,
    marginRight: Spacing.md,
  },
  menuTitle: {
    flex: 1,
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  menuDivider: {
    height: 1,
    backgroundColor: Colors.gray[100],
    marginLeft: 36,
  },
  badge: {
    backgroundColor: Colors.error[500],
    borderRadius: 10,
    minWidth: 20,
    height: 20,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 6,
    marginRight: Spacing.sm,
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '600',
    color: Colors.white,
  },
  menuArrow: {
    fontSize: 18,
    color: Colors.gray[400],
  },
  // 退出按钮
  logoutBtn: {
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.sm,
    paddingVertical: Spacing.base,
    alignItems: 'center',
  },
  logoutText: {
    fontSize: Typography.body.fontSize,
    color: Colors.error[500],
  },
  // 版本信息
  version: {
    textAlign: 'center',
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    marginTop: Spacing.xl,
    marginBottom: Spacing.xxl,
  },
});

export default ProfileScreen;
