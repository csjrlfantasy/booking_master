/**
 * 导航入口文件
 * 根导航器配置
 */
import React, {useEffect, useState, useCallback} from 'react';
import {View, Text, ActivityIndicator} from 'react-native';
import {NavigationContainer, NavigationContainerRef} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {useAppDispatch, useAppSelector} from '../store/hooks';
import {AuthNavigator} from './AuthNavigator';
import {MainNavigator} from './MainNavigator';
import {TicketNavigator} from './TicketNavigator';
import {SearchNavigator} from './SearchNavigator';
import CartNavigator from './CartNavigator';
import OrderNavigator from './OrderNavigator';
import {ProfileNavigator} from './ProfileNavigator';
import PaymentScreen from '../screens/payment/PaymentScreen';
import PaymentResultScreen from '../screens/payment/PaymentResultScreen';
import {userStorage} from '../services/storage';
import {userApi} from '../services/api';
import {setCredentials, setLoading, logout} from '../store/slices/authSlice';
import {setNavigationRef} from '../services/http';
import type {RootStackParamList} from './types';
import {Colors, Typography} from '../constants';

const Stack = createNativeStackNavigator<RootStackParamList>();

// 启动页组件
const SplashScreen: React.FC = () => (
  <View style={splashStyles.container}>
    <Text style={splashStyles.title}>票多星</Text>
    <Text style={splashStyles.subtitle}>TicketMore</Text>
    <ActivityIndicator size="large" color={Colors.primary[500]} style={splashStyles.loader} />
  </View>
);

const splashStyles = {
  container: {
    flex: 1,
    alignItems: 'center' as const,
    justifyContent: 'center' as const,
    backgroundColor: Colors.primary[900],
  },
  title: {
    fontSize: 36,
    fontWeight: '700' as const,
    color: Colors.white,
  },
  subtitle: {
    fontSize: Typography.h4.fontSize,
    color: Colors.primary[300],
    marginTop: 8,
  },
  loader: {
    marginTop: 40,
  },
};

// 自动登录验证Hook
const useAutoLogin = () => {
  const dispatch = useAppDispatch();
  const {isLoading: authIsLoading} = useAppSelector(state => state.auth);
  const [showSplash, setShowSplash] = useState(true);

  const validateToken = useCallback(async () => {
    const token = userStorage.getToken();
    const refreshToken = userStorage.getRefreshToken();
    const cachedUser = userStorage.getUser<any>();

    if (token && cachedUser) {
      try {
        const userId = parseInt(cachedUser.id, 10);
        if (userId && !isNaN(userId)) {
          const userInfo = await userApi.getUserInfo(userId);
          dispatch(setCredentials({
            user: userInfo,
            token,
            refreshToken: refreshToken || token,
          }));
        } else {
          throw new Error('Invalid user ID');
        }
      } catch (error: any) {
        console.log('Token验证失败:', error?.message);
        userStorage.clearAuth();
        dispatch(logout());
      }
    } else {
      dispatch(setLoading(false));
    }

    setShowSplash(false);
  }, [dispatch]);

  useEffect(() => {
    const timer = setTimeout(() => {
      validateToken();
    }, 500);

    return () => clearTimeout(timer);
  }, [validateToken]);

  return {isLoading: authIsLoading, showSplash};
};

// 根导航器
export const AppNavigator: React.FC = () => {
  const {showSplash} = useAutoLogin();
  const {isAuthenticated} = useAppSelector(state => state.auth);
  const navigationRef = React.useRef<NavigationContainerRef<RootStackParamList>>(null);

  useEffect(() => {
    if (navigationRef.current) {
      setNavigationRef(navigationRef.current as any);
    }
    return () => {
      setNavigationRef(null);
    };
  }, []);

  if (showSplash) {
    return <SplashScreen />;
  }

  return (
    <NavigationContainer ref={navigationRef}>
      <Stack.Navigator screenOptions={{headerShown: false}}>
        {!isAuthenticated ? (
          <Stack.Screen name="Auth" component={AuthNavigator} />
        ) : (
          <React.Fragment>
            <Stack.Screen name="Main" component={MainNavigator} />
            <Stack.Screen name="Ticket" component={TicketNavigator} />
            <Stack.Screen name="Search" component={SearchNavigator} />
            <Stack.Screen name="Cart" component={CartNavigator} />
            <Stack.Screen name="Order" component={OrderNavigator} />
            <Stack.Screen name="Profile" component={ProfileNavigator} />
            <Stack.Screen
              name="Payment"
              component={PaymentScreen}
              options={{
                headerShown: true,
                headerTitle: '订单支付',
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
            />
            <Stack.Screen
              name="PaymentResult"
              component={PaymentResultScreen}
              options={{
                headerShown: false,
              }}
            />
          </React.Fragment>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default AppNavigator;
