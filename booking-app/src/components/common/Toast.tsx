/**
 * Toast 轻提示组件
 * 基于UI设计规范文档的Toast样式
 */
import React, {useEffect, useRef} from 'react';
import {
  View,
  Text,
  StyleSheet,
  Animated,
  Dimensions,
  TouchableWithoutFeedback,
} from 'react-native';
import {Colors, Typography, BorderRadius, Spacing} from '../../constants';

export type ToastType = 'success' | 'error' | 'warning' | 'info' | 'loading';

interface ToastProps {
  // 内容
  message: string;
  type?: ToastType;
  duration?: number;

  // 位置
  position?: 'top' | 'center' | 'bottom';

  // 事件
  onHide?: () => void;

  // 测试
  testID?: string;
}

// Toast显示状态
let currentToast: {
  show: (options: ShowOptions) => void;
  hide: () => void;
} | null = null;

// Show选项
interface ShowOptions {
  message: string;
  type?: ToastType;
  duration?: number;
}

const Toast: React.FC<ToastProps> = ({
  message,
  type = 'info',
  duration = 2000,
  position = 'bottom',
  onHide,
  testID,
}) => {
  const opacity = useRef(new Animated.Value(0)).current;
  const translateY = useRef(new Animated.Value(50)).current;

  useEffect(() => {
    // 入场动画
    Animated.parallel([
      Animated.timing(opacity, {
        toValue: 1,
        duration: 200,
        useNativeDriver: true,
      }),
      Animated.timing(translateY, {
        toValue: 0,
        duration: 200,
        useNativeDriver: true,
      }),
    ]).start();

    // 自动隐藏
    const timer = setTimeout(() => {
      handleHide();
    }, duration);

    return () => clearTimeout(timer);
  }, []);

  const handleHide = () => {
    Animated.parallel([
      Animated.timing(opacity, {
        toValue: 0,
        duration: 200,
        useNativeDriver: true,
      }),
      Animated.timing(translateY, {
        toValue: 50,
        duration: 200,
        useNativeDriver: true,
      }),
    ]).start(() => {
      onHide?.();
    });
  };

  // 获取背景色
  const getBackgroundColor = (): string => {
    switch (type) {
      case 'success':
        return 'rgba(76, 175, 80, 0.95)';
      case 'error':
        return 'rgba(244, 67, 54, 0.95)';
      case 'warning':
        return 'rgba(255, 152, 0, 0.95)';
      case 'info':
        return 'rgba(33, 150, 243, 0.95)';
      case 'loading':
        return 'rgba(33, 33, 33, 0.9)';
      default:
        return 'rgba(33, 33, 33, 0.9)';
    }
  };

  // 获取图标
  const getIcon = (): string => {
    switch (type) {
      case 'success':
        return '✓';
      case 'error':
        return '✕';
      case 'warning':
        return '!';
      case 'info':
        return 'i';
      case 'loading':
        return '';
      default:
        return '';
    }
  };

  // 获取位置样式
  const getPositionStyle = (): any => {
    const {height} = Dimensions.get('window');
    switch (position) {
      case 'top':
        return {top: 60 + (Constants?.statusBarHeight || 44)};
      case 'center':
        return {top: height / 2 - 40};
      case 'bottom':
      default:
        return {bottom: 100 + (Constants?.safeAreaBottom || 20)};
    }
  };

  return (
    <TouchableWithoutFeedback>
      <Animated.View
        testID={testID}
        style={[
          styles.container,
          getPositionStyle(),
          {backgroundColor: getBackgroundColor(), opacity, transform: [{translateY}]},
        ]}
      >
        {type !== 'loading' && <Text style={styles.icon}>{getIcon()}</Text>}
        <Text style={styles.message}>{message}</Text>
      </Animated.View>
    </TouchableWithoutFeedback>
  );
};

// Toast管理器
class ToastManager {
  private visible = false;
  private options: ShowOptions = {message: '', type: 'info'};

  show(options: ShowOptions) {
    this.options = options;
    this.visible = true;
    // TODO: 通过Context或Portal实现
  }

  hide() {
    this.visible = false;
  }

  success(message: string, duration?: number) {
    this.show({message, type: 'success', duration});
  }

  error(message: string, duration?: number) {
    this.show({message, type: 'error', duration});
  }

  warning(message: string, duration?: number) {
    this.show({message, type: 'warning', duration});
  }

  info(message: string, duration?: number) {
    this.show({message, type: 'info', duration});
  }

  loading(message: string) {
    this.show({message, type: 'loading', duration: 0});
  }

  hideLoading() {
    this.hide();
  }
}

// 导出单例
export const toast = new ToastManager();
export {Toast};

// 模拟Constants（实际使用需要native模块）
const Constants = {
  statusBarHeight: 44,
  safeAreaBottom: 20,
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    left: 16,
    right: 16,
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.md,
    borderRadius: BorderRadius.md,
    maxWidth: Dimensions.get('window').width - 32,
    minHeight: 40,
  },
  icon: {
    fontSize: 16,
    color: Colors.white,
    marginRight: Spacing.sm,
    fontWeight: '700',
  },
  message: {
    flex: 1,
    fontSize: Typography.body.fontSize,
    fontWeight: '500',
    color: Colors.white,
  },
});

export default Toast;
export type {ToastProps};
