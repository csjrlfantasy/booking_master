/**
 * SettingsScreen - 设置页面
 */
import React, {useCallback, useState} from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Switch,
  Alert,
} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import {CommonActions} from '@react-navigation/native';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {useAppDispatch, useAppSelector} from '../../store/hooks';
import {logout} from '../../store/slices/authSlice';
import {userStorage} from '../../services/storage';
import {toast} from '../../components/common/Toast';

type SettingsNavigationProp = NativeStackNavigationProp<any>;

// 设置项配置
interface SettingItem {
  id: string;
  icon: string;
  title: string;
  type: 'navigation' | 'switch' | 'action';
  badge?: string;
  onPress?: () => void;
  onSwitchChange?: (value: boolean) => void;
  switchValue?: boolean;
}

const SettingsScreen: React.FC = () => {
  const navigation = useNavigation<SettingsNavigationProp>();
  const dispatch = useAppDispatch();
  const {user} = useAppSelector(state => state.auth);

  // 通知开关状态
  const [pushEnabled, setPushEnabled] = useState(true);
  const [orderNotifyEnabled, setOrderNotifyEnabled] = useState(true);
  const [promotionNotifyEnabled, setPromotionNotifyEnabled] = useState(false);

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
            userStorage.clearAuth();
            dispatch(logout());
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

  // 清除缓存
  const handleClearCache = useCallback(() => {
    Alert.alert(
      '清除缓存',
      '确定要清除本地缓存吗？',
      [
        {text: '取消', style: 'cancel'},
        {
          text: '确定',
          onPress: () => {
            toast.success('缓存已清除');
          },
        },
      ]
    );
  }, []);

  // 关于我们
  const handleAbout = useCallback(() => {
    Alert.alert(
      '关于我们',
      '票多星 V1.0.0\n\n专业的票务服务平台\n提供演唱会、体育赛事、戏剧等票务服务',
      [{text: '确定'}]
    );
  }, []);

  // 隐私政策
  const handlePrivacy = useCallback(() => {
    Alert.alert(
      '隐私政策',
      '我们承诺保护您的个人信息安全...\n\n(详细内容请访问官网)',
      [{text: '确定'}]
    );
  }, []);

  // 用户服务协议
  const handleAgreement = useCallback(() => {
    Alert.alert(
      '用户服务协议',
      '在使用票多星服务前，请仔细阅读以下条款...\n\n(详细内容请访问官网)',
      [{text: '确定'}]
    );
  }, []);

  // 设置项分组
  const accountItems: SettingItem[] = [
    {
      id: 'profile',
      icon: '👤',
      title: '个人资料',
      type: 'navigation',
      onPress: () => Alert.alert('提示', '个人资料编辑功能开发中'),
    },
    {
      id: 'phone',
      icon: '📱',
      title: '手机号',
      type: 'navigation',
      badge: user?.phone ? '已绑定' : '未绑定',
      onPress: () => Alert.alert('提示', '手机绑定功能开发中'),
    },
    {
      id: 'password',
      icon: '🔐',
      title: '登录密码',
      type: 'navigation',
      badge: '修改',
      onPress: () => Alert.alert('提示', '密码修改功能开发中'),
    },
  ];

  const notificationItems: SettingItem[] = [
    {
      id: 'push',
      icon: '🔔',
      title: '推送通知',
      type: 'switch',
      switchValue: pushEnabled,
      onSwitchChange: (value) => setPushEnabled(value),
    },
    {
      id: 'orderNotify',
      icon: '📨',
      title: '订单通知',
      type: 'switch',
      switchValue: orderNotifyEnabled,
      onSwitchChange: (value) => setOrderNotifyEnabled(value),
    },
    {
      id: 'promotionNotify',
      icon: '🎁',
      title: '优惠活动通知',
      type: 'switch',
      switchValue: promotionNotifyEnabled,
      onSwitchChange: (value) => setPromotionNotifyEnabled(value),
    },
  ];

  const otherItems: SettingItem[] = [
    {
      id: 'clearCache',
      icon: '🗑️',
      title: '清除缓存',
      type: 'action',
      onPress: handleClearCache,
    },
    {
      id: 'about',
      icon: 'ℹ️',
      title: '关于我们',
      type: 'navigation',
      onPress: handleAbout,
    },
    {
      id: 'privacy',
      icon: '📜',
      title: '隐私政策',
      type: 'navigation',
      onPress: handlePrivacy,
    },
    {
      id: 'agreement',
      icon: '📝',
      title: '用户服务协议',
      type: 'navigation',
      onPress: handleAgreement,
    },
  ];

  // 渲染设置项
  const renderSettingItem = (item: SettingItem) => (
    <View key={item.id} style={styles.settingItem}>
      <View style={styles.settingLeft}>
        <Text style={styles.settingIcon}>{item.icon}</Text>
        <Text style={styles.settingTitle}>{item.title}</Text>
      </View>
      <View style={styles.settingRight}>
        {item.type === 'switch' && (
          <Switch
            value={item.switchValue}
            onValueChange={item.onSwitchChange}
            trackColor={{false: Colors.gray[300], true: Colors.primary[300]}}
            thumbColor={item.switchValue ? Colors.primary[500] : Colors.gray[100]}
          />
        )}
        {item.type === 'navigation' && (
          <>
            {item.badge && <Text style={styles.badge}>{item.badge}</Text>}
            <Text style={styles.arrow}>›</Text>
          </>
        )}
        {item.type === 'action' && (
          <Text style={styles.arrow}>›</Text>
        )}
      </View>
    </View>
  );

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
      >
        {/* 账号设置 */}
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>账号设置</Text>
          {accountItems.map((item, index) => (
            <View key={item.id}>
              {renderSettingItem(item)}
              {index < accountItems.length - 1 && <View style={styles.divider} />}
            </View>
          ))}
        </View>

        {/* 消息通知 */}
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>消息通知</Text>
          {notificationItems.map((item, index) => (
            <View key={item.id}>
              {renderSettingItem(item)}
              {index < notificationItems.length - 1 && <View style={styles.divider} />}
            </View>
          ))}
        </View>

        {/* 其他设置 */}
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>其他</Text>
          {otherItems.map((item, index) => (
            <View key={item.id}>
              {renderSettingItem(item)}
              {index < otherItems.length - 1 && <View style={styles.divider} />}
            </View>
          ))}
        </View>

        {/* 退出登录 */}
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
        <Text style={styles.version}>票多星 V1.0.0</Text>
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
  section: {
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
  },
  sectionTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.md,
  },
  settingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    minHeight: 44,
  },
  settingLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  settingIcon: {
    fontSize: 18,
    marginRight: Spacing.md,
  },
  settingTitle: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  settingRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  badge: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginRight: Spacing.xs,
  },
  arrow: {
    fontSize: 18,
    color: Colors.gray[400],
  },
  divider: {
    height: 1,
    backgroundColor: Colors.gray[100],
    marginLeft: 34,
  },
  logoutBtn: {
    marginHorizontal: Spacing.md,
    marginTop: Spacing.xl,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.sm,
    paddingVertical: Spacing.base,
    alignItems: 'center',
  },
  logoutText: {
    fontSize: Typography.body.fontSize,
    color: Colors.error[500],
  },
  version: {
    textAlign: 'center',
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    marginTop: Spacing.xl,
    marginBottom: Spacing.xxl,
  },
});

export default SettingsScreen;
