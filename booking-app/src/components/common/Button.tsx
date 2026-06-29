/**
 * Button 按钮组件
 * 基于UI设计规范文档的按钮样式
 */
import React from 'react';
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ViewStyle,
  TextStyle,
  ActivityIndicator,
  View,
} from 'react-native';
import {Colors, Typography, BorderRadius, Spacing, Shadows} from '../../constants';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'text' | 'danger' | 'success';
export type ButtonSize = 'large' | 'medium' | 'small' | 'xs';

interface ButtonProps {
  // 内容
  title: string;
  icon?: React.ReactNode;

  // 样式
  variant?: ButtonVariant;
  size?: ButtonSize;
  fullWidth?: boolean;

  // 状态
  disabled?: boolean;
  loading?: boolean;

  // 事件
  onPress?: () => void;

  // 样式覆盖
  style?: ViewStyle;
  textStyle?: TextStyle;
  testID?: string;
}

const Button: React.FC<ButtonProps> = ({
  title,
  icon,
  variant = 'primary',
  size = 'medium',
  fullWidth = false,
  disabled = false,
  loading = false,
  onPress,
  style,
  textStyle,
  testID,
}) => {
  const isDisabled = disabled || loading;

  // 获取背景色
  const getBackgroundColor = (): string => {
    if (isDisabled) return Colors.gray[300];
    switch (variant) {
      case 'primary':
        return Colors.primary[500];
      case 'secondary':
        return Colors.white;
      case 'outline':
        return Colors.transparent;
      case 'text':
        return Colors.transparent;
      case 'danger':
        return Colors.error[500];
      case 'success':
        return Colors.success[500];
      default:
        return Colors.primary[500];
    }
  };

  // 获取文字颜色
  const getTextColor = (): string => {
    if (isDisabled) return Colors.gray[500];
    switch (variant) {
      case 'primary':
      case 'danger':
      case 'success':
        return Colors.white;
      case 'secondary':
        return Colors.primary[500];
      case 'outline':
      case 'text':
        return Colors.primary[500];
      default:
        return Colors.white;
    }
  };

  // 获取边框样式
  const getBorderStyle = (): ViewStyle => {
    if (variant === 'outline') {
      return {
        borderWidth: 1,
        borderColor: isDisabled ? Colors.gray[300] : Colors.primary[500],
      };
    }
    if (variant === 'secondary') {
      return {
        borderWidth: 1,
        borderColor: Colors.primary[500],
      };
    }
    return {};
  };

  // 获取尺寸样式
  const getSizeStyle = (): ViewStyle => {
    switch (size) {
      case 'large':
        return {height: 52, paddingHorizontal: Spacing.lg};
      case 'medium':
        return {height: 44, paddingHorizontal: Spacing.base};
      case 'small':
        return {height: 36, paddingHorizontal: Spacing.sm};
      case 'xs':
        return {height: 28, paddingHorizontal: Spacing.sm};
      default:
        return {height: 44, paddingHorizontal: Spacing.base};
    }
  };

  // 获取字号
  const getFontSize = (): number => {
    switch (size) {
      case 'large':
        return Typography.bodyLarge.fontSize;
      case 'medium':
        return Typography.body.fontSize;
      case 'small':
        return Typography.bodySmall.fontSize;
      case 'xs':
        return Typography.caption.fontSize;
      default:
        return Typography.body.fontSize;
    }
  };

  const handlePress = () => {
    if (!isDisabled && onPress) {
      onPress();
    }
  };

  return (
    <TouchableOpacity
      testID={testID}
      style={[
        styles.button,
        getSizeStyle(),
        getBorderStyle(),
        {backgroundColor: getBackgroundColor()},
        fullWidth && styles.fullWidth,
        variant === 'primary' && !isDisabled && Shadows.sm,
        style,
      ]}
      onPress={handlePress}
      disabled={isDisabled}
      activeOpacity={0.8}
    >
      {loading ? (
        <ActivityIndicator size="small" color={getTextColor()} />
      ) : (
        <View style={styles.content}>
          {icon && <View style={styles.icon}>{icon}</View>}
          <Text
            style={[
              styles.text,
              {color: getTextColor(), fontSize: getFontSize()},
              textStyle,
            ]}
          >
            {title}
          </Text>
        </View>
      )}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  button: {
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
  },
  fullWidth: {
    width: '100%',
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  icon: {
    marginRight: Spacing.sm,
  },
  text: {
    fontWeight: '600',
    textAlign: 'center',
  },
});

export default Button;
export type {ButtonProps};
