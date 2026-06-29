/**
 * 注册页面
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

type Props = NativeStackScreenProps<AuthStackParamList, 'Register'>;

export const RegisterScreen: React.FC<Props> = ({navigation}) => {
  const dispatch = useAppDispatch();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [agreed, setAgreed] = useState(false);

  // 验证表单
  const validateForm = (): boolean => {
    if (!username.trim()) {
      setError('请输入用户名');
      return false;
    }
    if (username.length < 3) {
      setError('用户名至少3个字符');
      return false;
    }
    if (!password || password.length < 6) {
      setError('密码长度不能少于6位');
      return false;
    }
    if (password !== confirmPassword) {
      setError('两次输入的密码不一致');
      return false;
    }
    if (!agreed) {
      setError('请阅读并同意用户协议');
      return false;
    }
    return true;
  };

  // 跳转到首页
  const navigateToMain = () => {
    // 获取父级导航器（RootStack）的 navigation
    const parentNavigation = navigation.getParent<NativeStackNavigationProp<RootStackParamList>>();
    
    if (parentNavigation) {
      // 使用 reset 重置整个导航栈到 Main，防止用户按返回键回到注册页
      parentNavigation.dispatch(
        CommonActions.reset({
          index: 0,
          routes: [{name: 'Main'}],
        })
      );
    }
  };

  // 注册
  const handleRegister = async () => {
    if (!validateForm()) return;

    setLoading(true);
    setError('');

    try {
      // userApi.register 已经在内部实现了注册后自动登录
      const loginResponse = await userApi.register({
        username,
        password,
        phone: phone || undefined,
        email: email || undefined,
      });

      // 存储Token
      userStorage.setToken(loginResponse.token);
      userStorage.setRefreshToken(loginResponse.refreshToken);
      userStorage.setUser(loginResponse.user);

      // 更新Redux状态
      dispatch(setCredentials({
        user: loginResponse.user,
        token: loginResponse.token,
        refreshToken: loginResponse.refreshToken,
      }));

      // 注册成功后跳转到首页
      navigateToMain();
    } catch (err: any) {
      // 处理特定错误
      if (err.code === 1001) {
        setError('用户名已存在');
      } else if (err.needReLogin) {
        setError('登录已过期，请重新登录');
      } else {
        setError(err.message || '注册失败，请重试');
      }
    } finally {
      setLoading(false);
    }
  };

  // 返回登录页
  const handleGoBack = () => {
    navigation.goBack();
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
          {/* 标题区域 */}
          <View style={styles.headerContainer}>
            <TouchableOpacity style={styles.backBtn} onPress={handleGoBack}>
              <Text style={styles.backBtnText}>←</Text>
            </TouchableOpacity>
            <Text style={styles.title}>注册账号</Text>
            <View style={styles.placeholder} />
          </View>

          {/* 表单区域 */}
          <View style={styles.formContainer}>
            {/* 用户名输入 */}
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>用户名 <Text style={styles.required}>*</Text></Text>
              <View style={styles.inputWrapper}>
                <TextInput
                  style={styles.input}
                  value={username}
                  onChangeText={setUsername}
                  placeholder="请输入用户名（至少3个字符）"
                  placeholderTextColor={Colors.gray[400]}
                  autoCapitalize="none"
                  autoCorrect={false}
                />
              </View>
            </View>

            {/* 密码输入 */}
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>设置密码 <Text style={styles.required}>*</Text></Text>
              <View style={styles.inputWrapper}>
                <TextInput
                  style={styles.input}
                  value={password}
                  onChangeText={setPassword}
                  placeholder="请设置密码（至少6位）"
                  placeholderTextColor={Colors.gray[400]}
                  secureTextEntry
                  maxLength={20}
                />
              </View>
            </View>

            {/* 确认密码输入 */}
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>确认密码 <Text style={styles.required}>*</Text></Text>
              <View style={styles.inputWrapper}>
                <TextInput
                  style={styles.input}
                  value={confirmPassword}
                  onChangeText={setConfirmPassword}
                  placeholder="请再次输入密码"
                  placeholderTextColor={Colors.gray[400]}
                  secureTextEntry
                  maxLength={20}
                />
              </View>
            </View>

            {/* 手机号输入（可选） */}
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>手机号（可选）</Text>
              <View style={styles.inputWrapper}>
                <TextInput
                  style={styles.input}
                  value={phone}
                  onChangeText={setPhone}
                  placeholder="请输入手机号"
                  placeholderTextColor={Colors.gray[400]}
                  keyboardType="phone-pad"
                  maxLength={11}
                />
              </View>
            </View>

            {/* 邮箱输入（可选） */}
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>邮箱（可选）</Text>
              <View style={styles.inputWrapper}>
                <TextInput
                  style={styles.input}
                  value={email}
                  onChangeText={setEmail}
                  placeholder="请输入邮箱"
                  placeholderTextColor={Colors.gray[400]}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  autoCorrect={false}
                />
              </View>
            </View>

            {/* 错误提示 */}
            {error ? <Text style={styles.errorText}>{error}</Text> : null}

            {/* 用户协议 */}
            <TouchableOpacity
              style={styles.agreementContainer}
              onPress={() => setAgreed(!agreed)}
            >
              <View style={[styles.checkbox, agreed && styles.checkboxChecked]}>
                {agreed && <Text style={styles.checkmark}>✓</Text>}
              </View>
              <Text style={styles.agreementText}>
                我已阅读并同意
                <Text style={styles.linkText}>《用户协议》</Text>
                和
                <Text style={styles.linkText}>《隐私政策》</Text>
              </Text>
            </TouchableOpacity>

            {/* 注册按钮 */}
            <TouchableOpacity
              style={[styles.registerBtn, loading && styles.registerBtnDisabled]}
              onPress={handleRegister}
              disabled={loading}
            >
              <Text style={styles.registerBtnText}>
                {loading ? '注册中...' : '注册'}
              </Text>
            </TouchableOpacity>

            {/* 登录入口 */}
            <View style={styles.loginContainer}>
              <Text style={styles.loginText}>已有账号？</Text>
              <TouchableOpacity onPress={handleGoBack}>
                <Text style={styles.loginLink}>立即登录</Text>
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
    paddingTop: Spacing.lg,
    paddingBottom: Spacing.xxl,
  },
  headerContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.xl,
  },
  backBtn: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backBtnText: {
    fontSize: 24,
    color: Colors.gray[700],
  },
  title: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  placeholder: {
    width: 40,
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
  required: {
    color: Colors.error[500],
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
  errorText: {
    fontSize: Typography.body2.fontSize,
    color: Colors.error[500],
    marginBottom: Spacing.md,
  },
  agreementContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: Spacing.lg,
  },
  checkbox: {
    width: 20,
    height: 20,
    borderWidth: 1,
    borderColor: Colors.gray[400],
    borderRadius: 4,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Spacing.sm,
    marginTop: 2,
  },
  checkboxChecked: {
    backgroundColor: Colors.primary[500],
    borderColor: Colors.primary[500],
  },
  checkmark: {
    fontSize: 12,
    color: Colors.white,
    fontWeight: '700',
  },
  agreementText: {
    flex: 1,
    fontSize: Typography.body2.fontSize,
    color: Colors.gray[500],
    lineHeight: 20,
  },
  linkText: {
    color: Colors.primary[500],
  },
  registerBtn: {
    height: 48,
    backgroundColor: Colors.primary[500],
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: Spacing.md,
  },
  registerBtnDisabled: {
    backgroundColor: Colors.gray[400],
  },
  registerBtnText: {
    fontSize: Typography.body1.fontSize,
    fontWeight: '600',
    color: Colors.white,
  },
  loginContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: Spacing.xl,
  },
  loginText: {
    fontSize: Typography.body2.fontSize,
    color: Colors.gray[500],
  },
  loginLink: {
    fontSize: Typography.body2.fontSize,
    color: Colors.primary[500],
    fontWeight: '600',
    marginLeft: Spacing.xs,
  },
});

export default RegisterScreen;