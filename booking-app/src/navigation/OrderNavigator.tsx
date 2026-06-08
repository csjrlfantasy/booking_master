/**
 * OrderNavigator - 订单导航器
 */
import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import type {OrderStackParamList} from './types';
import {Colors, Typography} from '../constants';

import OrderListScreen from '../screens/order/OrderListScreen';
import OrderDetailScreen from '../screens/order/OrderDetailScreen';
import RefundApplyScreen from '../screens/order/RefundApplyScreen';

const Stack = createNativeStackNavigator<OrderStackParamList>();

const OrderNavigator: React.FC = () => {
  return (
    <Stack.Navigator
      screenOptions={{
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
        name="OrderList"
        component={OrderListScreen}
        options={{
          title: '我的订单',
          headerShown: true,
        }}
      />
      <Stack.Screen
        name="OrderDetail"
        component={OrderDetailScreen}
        options={{
          title: '订单详情',
          headerShown: true,
        }}
      />
      <Stack.Screen
        name="RefundApply"
        component={RefundApplyScreen}
        options={{
          title: '申请退款',
          headerShown: true,
        }}
      />
    </Stack.Navigator>
  );
};

export default OrderNavigator;
