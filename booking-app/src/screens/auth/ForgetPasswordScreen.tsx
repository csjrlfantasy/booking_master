/**
 * 忘记密码页面
 */
import React, {useState, useEffect} from 'react';
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
import {Colors, Typography, Spacing} from '../../constants';
import {userApi} from '../../services/api';
import type {AuthStackParamList} from '../../navigation/types';

type Props = NativeStackScreenProps<AuthStackParamList, 'ForgetPassword'>;

// 验证码倒计时
const COUNTDOWN_TIME = 60;

export const ForgetPasswordScreen: React.FC<Props> = ({navigation}) => {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  // 倒计时定时器
  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [countdown]);

  // 发送验证码
  const handleSendCode = async () => {
    if (!phone || phone.length !== 11) {
      setError('请输入正确的手机号');
      return;
    }

    try {
      await userApi.sendCode({phone, type: 'resetPassword'});
      setCountdown(COUNTDOWN_TIME);
      setError('');
    } catch (err: any) {
      setError(err.message || '发送失败，请重试');
    }
  };

  // 验证表单
  const validateForm = (): boolean => {
    if (!phone || phone.length !== 11) {
      setError('请输入正确的手机号');
      return false;
    }
    if (!code || code.length < 4) {
      setError('请输入验证码');
      return false;
    }
    if (!newPassword || newPassword.length < 6) {
      setError('密码长度不能少于6位');
      return false;
    }
    if (newPassword !== confirmPassword) {
      setError('两次输入的密码不一致');
      return false;
    }
    return true;
  };

  // 重置密码
  const handleResetPassword = async () => {
    if (!validateForm()) return;

    setLoading(true);
    setError('');

    try {
      await userApi.resetPassword({
        phone,
        code,
        newPassword,
      });

      setSuccess(true);
      // 延迟返回登录页
      setTimeout(() => {
        navigation.navigate('Login');
      }, 2000);
    } catch (err: any) {
      setError(err.message || '重置失败，请重试');
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
            <Text style={styles.title}>找回密码</Text>
            <View style={styles.placeholder} />
          </View>

          {/* 成功提示 */}
          {success ? (
            <View style={styles.successContainer}>
              <Text style={styles.successIcon}>✓</Text>
              <Text style={styles.successText}>密码重置成功</Text>
              <Text style={styles.successSubtext}>即将返回登录页...</Text>
            </View>
          ) : (
            /* 表单区域 */
            <View style={styles.formContainer}>
              {/* 手机号输入 */}
              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>手机号</Text>
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

              {/* 验证码输入 */}
              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>验证码</Text>
                <View style={styles.codeInputWrapper}>
                  <TextInput
                    style={styles.codeInput}
                    value={code}
                    onChangeText={setCode}
                    placeholder="请输入验证码"
                    placeholderTextColor={Colors.gray[400]}
                    keyboardType="number-pad"
                    maxLength={6}
                  />
                  <TouchableOpacity
                    style={[styles.sendCodeBtn, countdown > 0 && styles.sendCodeBtnDisabled]}
                    onPress={handleSendCode}
                    disabled={countdown > 0 || !phone || phone.length !== 11}
                  >
                    <Text style={styles.sendCodeText}>
                      {countdown > 0 ? `${countdown}s` : '获取验证码'}
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>

              {/* 新密码输入 */}
              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>新密码</Text>
                <View style={styles.inputWrapper}>
                  <TextInput
                    style={styles.input}
                    value={newPassword}
                    onChangeText={setNewPassword}
                    placeholder="请设置新密码（至少6位）"
                    placeholderTextColor={Colors.gray[400]}
                    secureTextEntry
                    maxLength={20}
                  />
                </View>
              </View>

              {/* 确认新密码输入 */}
              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>确认新密码</Text>
                <View style={styles.inputWrapper}>
                  <TextInput
                    style={styles.input}
                    value={confirmPassword}
                    onChangeText={setConfirmPassword}
                    placeholder="请再次输入新密码"
                    placeholderTextColor={Colors.gray[400]}
                    secureTextEntry
                    maxLength={20}
                  />
                </View>
              </View>

              {/* 错误提示 */}
              {error ? <Text style={styles.errorText}>{error}</Text> : null}

              {/* 重置密码按钮 */}
              <TouchableOpacity
                style={[styles.resetBtn, loading && styles.resetBtnDisabled]}
                onPress={handleResetPassword}
                disabled={loading}
              >
                <Text style={styles.resetBtnText}>
                  {loading ? '重置中...' : '重置密码'}
                </Text>
              </TouchableOpacity>
            </View>
          )}
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
  successContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: Spacing.xxl * 2,
  },
  successIcon: {
    fontSize: 60,
    color: Colors.success[500],
    marginBottom: Spacing.lg,
  },
  successText: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.sm,
  },
  successSubtext: {
    fontSize: Typography.body2.fontSize,
    color: Colors.gray[500],
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
  codeInputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.gray[300],
    borderRadius: 8,
    backgroundColor: Colors.white,
  },
  codeInput: {
    flex: 1,
    height: 48,
    paddingHorizontal: Spacing.md,
    fontSize: Typography.body1.fontSize,
    color: Colors.gray[900],
  },
  sendCodeBtn: {
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    backgroundColor: Colors.primary[500],
    borderRadius: 6,
    marginRight: Spacing.sm,
  },
  sendCodeBtnDisabled: {
    backgroundColor: Colors.gray[300],
  },
  sendCodeText: {
    fontSize: Typography.body2.fontSize,
    color: Colors.white,
    fontWeight: '500',
  },
  errorText: {
    fontSize: Typography.body2.fontSize,
    color: Colors.error[500],
    marginBottom: Spacing.md,
  },
  resetBtn: {
    height: 48,
    backgroundColor: Colors.primary[500],
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: Spacing.md,
  },
  resetBtnDisabled: {
    backgroundColor: Colors.gray[400],
  },
  resetBtnText: {
    fontSize: Typography.body1.fontSize,
    fontWeight: '600',
    color: Colors.white,
  },
});

export default ForgetPasswordScreen;
