/**
 * Input 输入框组件
 * 基于UI设计规范文档的输入框样式
 */
import React, {useState, useRef} from 'react';
import {
  View,
  TextInput,
  Text,
  StyleSheet,
  ViewStyle,
  TextStyle,
  TextInputProps,
  TouchableOpacity,
} from 'react-native';
import {Colors, Typography, BorderRadius, Spacing} from '../../constants';

export type InputSize = 'large' | 'medium' | 'small';

interface InputProps extends Omit<TextInputProps, 'style'> {
  // 内容
  value: string;
  onChangeText: (text: string) => void;

  // 标签
  label?: string;
  placeholder?: string;
  error?: string;
  helperText?: string;

  // 图标
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  showClearButton?: boolean;

  // 样式
  size?: InputSize;
  secureTextEntry?: boolean;
  multiline?: boolean;

  // 状态
  disabled?: boolean;
  editable?: boolean;

  // 事件
  onClear?: () => void;
  onFocus?: () => void;
  onBlur?: () => void;

  // 样式覆盖
  containerStyle?: ViewStyle;
  inputStyle?: TextStyle;
  testID?: string;
}

const Input: React.FC<InputProps> = ({
  value,
  onChangeText,
  label,
  placeholder,
  error,
  helperText,
  leftIcon,
  rightIcon,
  showClearButton = false,
  size = 'medium',
  secureTextEntry = false,
  multiline = false,
  disabled = false,
  editable = true,
  onClear,
  onFocus,
  onBlur,
  containerStyle,
  inputStyle,
  testID,
  ...rest
}) => {
  const [isFocused, setIsFocused] = useState(false);
  const inputRef = useRef<TextInput>(null);

  // 获取输入框高度
  const getHeight = (): number => {
    switch (size) {
      case 'large':
        return 52;
      case 'medium':
        return 44;
      case 'small':
        return 36;
      default:
        return 44;
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
      default:
        return Typography.body.fontSize;
    }
  };

  // 获取边框颜色
  const getBorderColor = (): string => {
    if (error) return Colors.error[500];
    if (isFocused) return Colors.primary[500];
    if (disabled) return Colors.gray[200];
    return Colors.gray[300];
  };

  // 获取背景色
  const getBackgroundColor = (): string => {
    if (disabled) return Colors.gray[100];
    if (error) return Colors.error[100];
    return Colors.white;
  };

  const handleFocus = (e: any) => {
    setIsFocused(true);
    onFocus?.();
  };

  const handleBlur = (e: any) => {
    setIsFocused(false);
    onBlur?.();
  };

  const handleClear = () => {
    onChangeText('');
    onClear?.();
  };

  return (
    <View style={[styles.container, containerStyle]}>
      {label && <Text style={styles.label}>{label}</Text>}

      <TouchableOpacity
        activeOpacity={1}
        onPress={() => inputRef.current?.focus()}
        style={[
          styles.inputContainer,
          {
            height: getHeight(),
            backgroundColor: getBackgroundColor(),
            borderColor: getBorderColor(),
          },
          isFocused && styles.focused,
          error && styles.error,
          disabled && styles.disabled,
        ]}
      >
        {leftIcon && <View style={styles.leftIcon}>{leftIcon}</View>}

        <TextInput
          ref={inputRef}
          testID={testID}
          style={[
            styles.input,
            {fontSize: getFontSize()},
            multiline && styles.multiline,
            inputStyle,
          ]}
          value={value}
          onChangeText={onChangeText}
          placeholder={placeholder}
          placeholderTextColor={Colors.gray[500]}
          secureTextEntry={secureTextEntry}
          multiline={multiline}
          editable={!disabled && editable}
          onFocus={handleFocus}
          onBlur={handleBlur}
          {...rest}
        />

        {showClearButton && value.length > 0 && !disabled && (
          <TouchableOpacity onPress={handleClear} style={styles.clearButton}>
            <Text style={styles.clearText}>✕</Text>
          </TouchableOpacity>
        )}

        {rightIcon && !showClearButton && (
          <View style={styles.rightIcon}>{rightIcon}</View>
        )}
      </TouchableOpacity>

      {(error || helperText) && (
        <Text style={[styles.helperText, error && styles.errorText]}>
          {error || helperText}
        </Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginBottom: Spacing.base,
  },
  label: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '500',
    color: Colors.gray[700],
    marginBottom: Spacing.xs,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.base,
  },
  focused: {
    // 聚焦时添加阴影效果需要原生支持，这里用边框颜色代替
  },
  error: {},
  disabled: {
    opacity: 0.7,
  },
  input: {
    flex: 1,
    color: Colors.gray[900],
    padding: 0,
  },
  multiline: {
    textAlignVertical: 'top',
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.sm,
  },
  leftIcon: {
    marginRight: Spacing.sm,
  },
  rightIcon: {
    marginLeft: Spacing.sm,
  },
  clearButton: {
    marginLeft: Spacing.sm,
    padding: Spacing.xs,
  },
  clearText: {
    fontSize: 16,
    color: Colors.gray[400],
  },
  helperText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.xs,
  },
  errorText: {
    color: Colors.error[500],
  },
});

export default Input;
export type {InputProps};
