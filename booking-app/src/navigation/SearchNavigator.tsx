/**
 * 搜索栈导航器
 * 包含搜索、分类浏览页面
 */
import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import type {SearchStackParamList} from './types';
import {Colors} from '../constants';

// 页面组件
import SearchScreen from '../screens/search/SearchScreen';

const Stack = createNativeStackNavigator<SearchStackParamList>();

export const SearchNavigator: React.FC = () => {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: {
          backgroundColor: Colors.white,
        },
        headerTintColor: Colors.gray[900],
        headerTitleStyle: {
          fontWeight: '600',
        },
        headerShadowVisible: false,
      }}
    >
      <Stack.Screen
        name="Search"
        component={SearchScreen}
        options={{
          headerShown: false,
        }}
      />
    </Stack.Navigator>
  );
};

export default SearchNavigator;