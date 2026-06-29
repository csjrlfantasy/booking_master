/**
 * 倒计时组件
 */
import React, {useState, useEffect, useCallback} from 'react';
import {View, Text, StyleSheet} from 'react-native';
import {Colors, Typography, Spacing, BorderRadius} from '../../constants';

interface CountDownProps {
  endTime: string | number;
  onEnd?: () => void;
  format?: 'mm:ss' | 'hh:mm:ss' | 'dd:hh:mm:ss';
  style?: any;
  textStyle?: any;
}

const CountDown: React.FC<CountDownProps> = ({
  endTime,
  onEnd,
  format = 'mm:ss',
  style,
  textStyle,
}) => {
  const [timeLeft, setTimeLeft] = useState<number>(0);

  useEffect(() => {
    const calculateTimeLeft = () => {
      const end = typeof endTime === 'string' ? new Date(endTime).getTime() : endTime;
      const now = Date.now();
      return Math.max(0, end - now);
    };

    setTimeLeft(calculateTimeLeft());

    const timer = setInterval(() => {
      const left = calculateTimeLeft();
      setTimeLeft(left);

      if (left <= 0) {
        clearInterval(timer);
        onEnd?.();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [endTime, onEnd]);

  const formatTime = useCallback((ms: number): string => {
    const totalSeconds = Math.floor(ms / 1000);
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    switch (format) {
      case 'mm:ss':
        return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      case 'hh:mm:ss':
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      case 'dd:hh:mm:ss':
        return `${days}:${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      default:
        return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }
  }, [format]);

  const isUrgent = timeLeft < 5 * 60 * 1000; // 少于5分钟变红

  return (
    <View style={[styles.container, style]}>
      <Text style={[styles.text, isUrgent && styles.urgent, textStyle]}>
        {formatTime(timeLeft)}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.error[500],
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xs,
    borderRadius: BorderRadius.sm,
  },
  text: {
    color: Colors.white,
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '600',
    fontVariant: ['tabular-nums'],
  },
  urgent: {
    color: Colors.white,
  },
});

export default CountDown;
