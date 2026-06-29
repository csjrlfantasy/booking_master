/**
 * CartNavigator - 购物车导航器
 */
import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import type {CartStackParamList} from './types';
import {Colors, Typography} from '../constants';

import CartScreen from '../screens/cart/CartScreen';
import CheckoutScreen from '../screens/cart/CheckoutScreen';

const Stack = createNativeStackNavigator<CartStackParamList>();

const CartNavigator: React.FC = () => {
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
        name="Cart"
        component={CartScreen}
        options={{
          headerShown: false,
        }}
      />
      <Stack.Screen
        name="Checkout"
        component={CheckoutScreen}
        options={{
          title: '确认订单',
          headerShown: true,
        }}
      />
    </Stack.Navigator>
  );
};

export default CartNavigator;
