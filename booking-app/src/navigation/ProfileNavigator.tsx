import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import type {ProfileStackParamList} from './types';
import {Colors, Typography} from '../constants';
import ProfileScreen from '../screens/profile/ProfileScreen';
import SettingsScreen from '../screens/profile/SettingsScreen';
import IdentityScreen from '../screens/profile/IdentityScreen';
import CouponScreen from '../screens/profile/CouponScreen';

const Stack = createNativeStackNavigator<ProfileStackParamList>();

const PlaceholderScreen: React.FC<{title: string}> = ({title}) => {
  const {Text, View} = require('react-native');
  return (
    <View
      style={{
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: Colors.gray[100],
      }}
    >
      <Text style={{fontSize: Typography.h4.fontSize, color: Colors.gray[700]}}>{title}</Text>
      <Text style={{fontSize: Typography.bodySmall.fontSize, color: Colors.gray[400], marginTop: 8}}>
        功能开发中
      </Text>
    </View>
  );
};

export const ProfileNavigator: React.FC = () => {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: true,
        headerStyle: {backgroundColor: Colors.white},
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
      <Stack.Screen name="Profile" component={ProfileScreen} options={{headerShown: false}} />
      <Stack.Screen name="Settings" component={SettingsScreen} options={{headerTitle: '设置'}} />
      <Stack.Screen name="Coupon" component={CouponScreen} options={{headerTitle: '我的优惠券'}} />
      <Stack.Screen
        name="Favorite"
        component={() => <PlaceholderScreen title="我的收藏" />}
        options={{headerTitle: '我的收藏'}}
      />
      <Stack.Screen name="Identity" component={IdentityScreen} options={{headerTitle: '常用观演人'}} />
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

export default ProfileNavigator;
