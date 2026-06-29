/**
 * 主导航器
 * 包含底部Tab导航和主页面
 */
import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import {View, Text, StyleSheet} from 'react-native';
import type {MainTabParamList} from './types';
import {Colors, Typography} from '../constants';

// 页面组件
import HomeScreen from '../screens/home/HomeScreen';
import CategoryScreen from '../screens/category/CategoryScreen';
import OrderListScreen from '../screens/order/OrderListScreen';
import ProfileScreen from '../screens/profile/ProfileScreen';

const Tab = createBottomTabNavigator<MainTabParamList>();

// Tab图标组件
const TabIcon: React.FC<{name: string; focused: boolean}> = ({name, focused}) => {
  const iconMap: Record<string, string> = {
    home: '🏠',
    category: '📋',
    orders: '📄',
    profile: '👤',
  };

  return (
    <View style={styles.iconContainer}>
      <Text style={[styles.icon, focused && styles.iconFocused]}>
        {iconMap[name] || '●'}
      </Text>
    </View>
  );
};

export const MainTabs: React.FC = () => {
  return (
    <Tab.Navigator
      screenOptions={{
        tabBarActiveTintColor: Colors.primary[500],
        tabBarInactiveTintColor: Colors.gray[500],
        tabBarStyle: styles.tabBar,
        tabBarLabelStyle: styles.tabLabel,
        headerShown: false,
      }}
    >
      <Tab.Screen
        name="HomeTab"
        component={HomeScreen}
        options={{
          tabBarLabel: '首页',
          tabBarIcon: ({focused}) => <TabIcon name="home" focused={focused} />,
        }}
      />
      <Tab.Screen
        name="CategoryTab"
        component={CategoryScreen}
        options={{
          tabBarLabel: '分类',
          tabBarIcon: ({focused}) => <TabIcon name="category" focused={focused} />,
        }}
      />
      <Tab.Screen
        name="OrdersTab"
        component={OrderListScreen}
        options={{
          tabBarLabel: '订单',
          tabBarIcon: ({focused}) => <TabIcon name="orders" focused={focused} />,
          headerShown: true,
          headerTitle: '我的订单',
          headerStyle: {
            backgroundColor: Colors.white,
          },
          headerTitleStyle: {
            fontSize: Typography.h4.fontSize,
            fontWeight: '600',
            color: Colors.gray[900],
          },
        }}
      />
      <Tab.Screen
        name="ProfileTab"
        component={ProfileScreen}
        options={{
          tabBarLabel: '我的',
          tabBarIcon: ({focused}) => <TabIcon name="profile" focused={focused} />,
          headerShown: false,
        }}
      />
    </Tab.Navigator>
  );
};

const styles = StyleSheet.create({
  tabBar: {
    backgroundColor: Colors.white,
    borderTopColor: Colors.gray[300],
    borderTopWidth: 1,
    height: 60,
    paddingBottom: 8,
    paddingTop: 8,
  },
  tabLabel: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '500',
  },
  iconContainer: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  icon: {
    fontSize: 22,
    opacity: 0.6,
  },
  iconFocused: {
    opacity: 1,
  },
});

export const MainNavigator: React.FC = () => {
  return <MainTabs />;
};

export default MainNavigator;
