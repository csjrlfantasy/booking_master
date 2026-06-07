/**
 * Loading 加载组件
 * 基于UI设计规范文档的加载指示器样式
 */
import React from 'react';
import {View, ActivityIndicator, Text, StyleSheet, Modal, ViewStyle} from 'react-native';
import {Colors, Typography, BorderRadius, Spacing, Shadows} from '../../constants';

export type LoadingSize = 'small' | 'medium' | 'large';

interface LoadingProps {
  // 内容
  size?: LoadingSize;
  color?: string;
  text?: string;

  // 全屏模式
  fullscreen?: boolean;
  fullScreen?: boolean;
  transparent?: boolean;

  // 样式覆盖
  style?: ViewStyle;
  textStyle?: ViewStyle;
}

const Loading: React.FC<LoadingProps> = ({
  size = 'medium',
  color = Colors.primary[500],
  text,
  fullscreen = false,
  fullScreen = false,
  transparent = false,
  style,
  textStyle,
}) => {
  // 获取指示器尺寸
  const getIndicatorSize = (): number | 'small' => {
    switch (size) {
      case 'small':
        return 'small';
      case 'large':
        return 36;
      default:
        return 24;
    }
  };

  const content = (
    <View style={[styles.container, style]}>
      <ActivityIndicator size={getIndicatorSize()} color={color} />
      {text && <Text style={[styles.text, textStyle]}>{text}</Text>}
    </View>
  );

  if (fullscreen || fullScreen) {
    return (
      <Modal visible transparent={transparent} animationType="fade">
        <View style={[styles.overlay, transparent && styles.transparentOverlay]}>
          <View style={[styles.fullscreenContent, Shadows.lg]}>
            {content}
          </View>
        </View>
      </Modal>
    );
  }

  return content;
};

// 独立的全屏Loading组件
interface FullScreenLoadingProps {
  visible: boolean;
  text?: string;
}

export const FullScreenLoading: React.FC<FullScreenLoadingProps> = ({visible, text}) => {
  if (!visible) return null;

  return (
    <Modal visible transparent animationType="fade">
      <View style={styles.overlay}>
        <View style={[styles.fullscreenContent, Shadows.lg]}>
          <ActivityIndicator size="large" color={Colors.primary[500]} />
          {text && <Text style={styles.text}>{text}</Text>}
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.base,
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  transparentOverlay: {
    backgroundColor: 'transparent',
  },
  fullscreenContent: {
    backgroundColor: Colors.white,
    padding: Spacing.xl,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    minWidth: 120,
  },
  text: {
    marginTop: Spacing.base,
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
});

export default Loading;
export type {LoadingProps};
