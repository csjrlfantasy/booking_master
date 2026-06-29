/**
 * 登录页面
 */
import React, {useState} from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import type {NativeStackScreenProps} from '@react-navigation/native-stack';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import {CommonActions} from '@react-navigation/native';
import {Colors, Typography, Spacing} from '../../constants';
import {userApi} from '../../services/api';
import {userStorage} from '../../services/storage';
import {useAppDispatch} from '../../store/hooks';
import {setCredentials} from '../../store/slices/authSlice';
import type {AuthStackParamList, RootStackParamList} from '../../navigation/types';

type Props = NativeStackScreenProps<AuthStackParamList, 'Login'>;

export const LoginScreen: React.FC<Props> = ({navigation}) => {
  const dispatch = useAppDispatch();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 跳转到首页
  const navigateToMain = () => {
    // 获取父级导航器（RootStack）的 navigation
    const parentNavigation = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();
    
    if (parentNavigation) {
      // 使用 reset 重置整个导航栈到 Main，防止用户按返回键回到登录页
      parentNavigation.dispatch(
        CommonActions.reset({
          index: 0,
          routes: [{name: 'Main'}],
        })
      );
    }
  };

  // 登录
  const handleLogin = async () => {
    if (!username.trim()) {
      setError('请输入用户名');
      return;
    }
    if (!password) {
      setError('请输入密码');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await userApi.login({username, password});
      
      // 存储Token
      userStorage.setToken(response.token);
      userStorage.setRefreshToken(response.refreshToken);
      userStorage.setUser(response.user);

      // 更新Redux状态
      dispatch(setCredentials({
        user: response.user,
        token: response.token,
        refreshToken: response.refreshToken,
      }));

      // 登录成功后跳转到首页
      navigateToMain();
    } catch (err: any) {
      // 处理401需要重新登录的情况
      if (err.needReLogin) {
        setError('登录已过期，请重新登录');
      } else {
        setError(err.message || '登录失败，请重试');
      }
    } finally {
      setLoading(false);
    }
  };

  // 跳转到注册页
  const handleGoToRegister = () => {
    navigation.navigate('Register');
  };

  // 跳转到忘记密码页
  const handleGoToForgetPassword = () => {
    navigation.navigate('ForgetPassword');
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          {/* Logo区域 */}
          <View style={styles.logoContainer}>
            <View style={styles.logoPlaceholder}>
              <Text style={styles.logoText}>票多星</Text>
            </View>
            <Text style={styles.subtitle}>TicketMore</Text>
            <Text style={styles.welcomeText}>欢迎登录</Text>
          </View>

          {/* 表单区域 */}
          <View style={styles.formContainer}>
            {/* 用户名输入 */}
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>用户名</Text>
              <View style={styles.inputWrapper}>
                <TextInput
                  style={styles.input}
                  value={username}
                  onChangeText={setUsername}
                  placeholder="请输入用户名"
                  placeholderTextColor={Colors.gray[400]}
                  autoCapitalize="none"
                  autoCorrect={false}
                />
              </View>
            </View>

            {/* 密码输入 */}
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>密码</Text>
              <View style={styles.inputWrapper}>
                <TextInput
                  style={styles.input}
                  value={password}
                  onChangeText={setPassword}
                  placeholder="请输入密码"
                  placeholderTextColor={Colors.gray[400]}
                  secureTextEntry
                />
              </View>
            </View>

            {/* 忘记密码 */}
            <TouchableOpacity style={styles.forgotPassword} onPress={handleGoToForgetPassword}>
              <Text style={styles.forgotPasswordText}>忘记密码？</Text>
            </TouchableOpacity>

            {/* 错误提示 */}
            {error ? <Text style={styles.errorText}>{error}</Text> : null}

            {/* 登录按钮 */}
            <TouchableOpacity
              style={[styles.loginBtn, loading && styles.loginBtnDisabled]}
              onPress={handleLogin}
              disabled={loading}
            >
              <Text style={styles.loginBtnText}>
                {loading ? '登录中...' : '登录'}
              </Text>
            </TouchableOpacity>

            {/* 注册入口 */}
            <View style={styles.registerContainer}>
              <Text style={styles.registerText}>还没有账号？</Text>
              <TouchableOpacity onPress={handleGoToRegister}>
                <Text style={styles.registerLink}>立即注册</Text>
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.white,
  },
  keyboardView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.xl,
    paddingBottom: Spacing.xxl,
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: Spacing.xxl,
    paddingTop: Spacing.xl,
  },
  logoPlaceholder: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: Colors.primary[500],
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.md,
  },
  logoText: {
    fontSize: 28,
    fontWeight: '700',
    color: Colors.white,
  },
  subtitle: {
    fontSize: Typography.h4.fontSize,
    color: Colors.primary[300],
    marginBottom: Spacing.lg,
  },
  welcomeText: {
    fontSize: Typography.h2.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  formContainer: {
    flex: 1,
  },
  inputGroup: {
    marginBottom: Spacing.lg,
  },
  inputLabel: {
    fontSize: Typography.body2.fontSize,
    fontWeight: '500',
    color: Colors.gray[700],
    marginBottom: Spacing.sm,
  },
  inputWrapper: {
    borderWidth: 1,
    borderColor: Colors.gray[300],
    borderRadius: 8,
    backgroundColor: Colors.white,
  },
  input: {
    height: 48,
    paddingHorizontal: Spacing.md,
    fontSize: Typography.body1.fontSize,
    color: Colors.gray[900],
  },
  forgotPassword: {
    alignSelf: 'flex-end',
    marginBottom: Spacing.lg,
  },
  forgotPasswordText: {
    fontSize: Typography.body2.fontSize,
    color: Colors.primary[500],
  },
  errorText: {
    fontSize: Typography.body2.fontSize,
    color: Colors.error[500],
    marginBottom: Spacing.md,
  },
  loginBtn: {
    height: 48,
    backgroundColor: Colors.primary[500],
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: Spacing.md,
  },
  loginBtnDisabled: {
    backgroundColor: Colors.gray[400],
  },
  loginBtnText: {
    fontSize: Typography.body1.fontSize,
    fontWeight: '600',
    color: Colors.white,
  },
  registerContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: Spacing.xl,
  },
  registerText: {
    fontSize: Typography.body2.fontSize,
    color: Colors.gray[500],
  },
  registerLink: {
    fontSize: Typography.body2.fontSize,
    color: Colors.primary[500],
    fontWeight: '600',
    marginLeft: Spacing.xs,
  },
});

export default LoginScreen;