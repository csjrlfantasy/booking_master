/**
 * 票务栈导航器
 * 包含票务详情、场馆地图、座位确认等页面
 */
import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {View, Text, StyleSheet} from 'react-native';
import type {TicketStackParamList} from './types';
import {Colors} from '../constants';

// 页面组件
import TicketDetailScreen from '../screens/ticket/TicketDetailScreen';

const Stack = createNativeStackNavigator<TicketStackParamList>();

// 临时场馆地图页面
const VenueMapScreen = () => (
  <View style={styles.placeholder}>
    <Text style={styles.placeholderText}>场馆地图</Text>
  </View>
);

// 临时座位确认页面
const SeatConfirmScreen = () => (
  <View style={styles.placeholder}>
    <Text style={styles.placeholderText}>座位确认</Text>
  </View>
);

const styles = StyleSheet.create({
  placeholder: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f5f5f5',
  },
  placeholderText: {
    fontSize: 16,
    color: '#666',
  },
});

export const TicketNavigator: React.FC = () => {
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
        name="TicketDetail"
        component={TicketDetailScreen}
        options={{
          headerShown: false,
        }}
      />
      <Stack.Screen
        name="VenueMap"
        component={VenueMapScreen}
        options={{
          title: '场馆位置',
        }}
      />
      <Stack.Screen
        name="SeatConfirm"
        component={SeatConfirmScreen}
        options={{
          title: '确认座位',
        }}
      />
    </Stack.Navigator>
  );
};

export default TicketNavigator;