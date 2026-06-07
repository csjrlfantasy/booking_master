/**
 * ProfileNavigator - 用户中心导航器
 */
import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import type {ProfileStackParamList} from './types';
import ProfileScreen from '../screens/profile/ProfileScreen';
import SettingsScreen from '../screens/profile/SettingsScreen';
import {Colors, Typography} from '../constants';

// 临时占位页面
const PlaceholderScreen: React.FC<{title: string}> = ({title}) => {
  const React = require('react');
  const {View, Text, StyleSheet} = require('react-native');
  return (
    <View style={styles.placeholder}>
      <Text style={styles.placeholderText}>{title}</Text>
      <Text style={styles.placeholderHint}>功能开发中</Text>
    </View>
  );
};

const Stack = createNativeStackNavigator<ProfileStackParamList>();

export const ProfileNavigator: React.FC = () => {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: true,
        headerStyle: {
          backgroundColor: Colors.white,
        },
        headerTitleStyle: {
          fontSize: Typography.h4.fontSize,
          fontWeight: '600',
          color: Colors.gray[900],
        },
        headerTintColor: Colors.gray[900],
        headerShadowVisible: false,
        headerBackTitleVisible: false,
      }}
    >
      <Stack.Screen
        name="Profile"
        component={ProfileScreen}
        options={{headerShown: false}}
      />
      <Stack.Screen
        name="Settings"
        component={SettingsScreen}
        options={{
          headerTitle: '设置',
        }}
      />
      <Stack.Screen
        name="Coupon"
        component={() => <PlaceholderScreen title="我的优惠券" />}
        options={({route}: any) => ({
          headerTitle: '我的优惠券',
        })}
      />
      <Stack.Screen
        name="Favorite"
        component={() => <PlaceholderScreen title="我的收藏" />}
        options={{
          headerTitle: '我的收藏',
        }}
      />
      <Stack.Screen
        name="AddressEdit"
        component={() => <PlaceholderScreen title="地址编辑" />}
        options={({route}: any) => ({
          headerTitle: route.params?.address ? '编辑地址' : '新增地址',
        })}
      />
    </Stack.Navigator>
  );
};

const styles = {
  placeholder: {
    flex: 1,
    alignItems: 'center' as const,
    justifyContent: 'center' as const,
    backgroundColor: Colors.gray[100],
  },
  placeholderText: {
    fontSize: Typography.h4.fontSize,
    color: Colors.gray[700],
  },
  placeholderHint: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[400],
    marginTop: 8,
  },
};

export default ProfileNavigator;
