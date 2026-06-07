/**
 * TicketCard 票品卡片组件
 */
import React from 'react';
import {View, Text, Image, TouchableOpacity, StyleSheet} from 'react-native';
import type {Ticket} from '../../types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';

interface TicketCardProps {
  ticket: Ticket;
  onPress?: (ticket: Ticket) => void;
  style?: 'vertical' | 'horizontal';
  showVenue?: boolean;
  showTime?: boolean;
}

const TicketCard: React.FC<TicketCardProps> = ({
  ticket,
  onPress,
  style = 'vertical',
  showVenue = true,
  showTime = true,
}) => {
  const handlePress = () => {
    onPress?.(ticket);
  };

  // 获取状态文字和颜色
  const getStatusInfo = () => {
    switch (ticket.status) {
      case 'onsale':
        return {text: '售票中', color: Colors.success[500]};
      case 'upcoming':
        return {text: '即将开售', color: Colors.warning[500]};
      case 'soldout':
        return {text: '已售罄', color: Colors.error[500]};
      case 'cancelled':
        return {text: '已取消', color: Colors.gray[500]};
      default:
        return {text: '', color: Colors.gray[500]};
    }
  };

  const statusInfo = getStatusInfo();

  // 格式化时间
  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
  };

  // 判断是否有折扣价
  const hasDiscount = ticket.isDiscount && ticket.discountPrice;

  if (style === 'horizontal') {
    return (
      <TouchableOpacity
        style={[styles.cardHorizontal, Shadows.sm]}
        onPress={handlePress}
        activeOpacity={0.9}
      >
        <Image
          source={{uri: ticket.posterUrl}}
          style={styles.posterHorizontal}
          resizeMode="cover"
        />
        <View style={styles.contentHorizontal}>
          <View style={styles.titleRow}>
            <Text style={styles.title} numberOfLines={2}>
              {ticket.name}
            </Text>
            {ticket.isHot && (
              <View style={styles.hotTag}>
                <Text style={styles.hotTagText}>热</Text>
              </View>
            )}
          </View>
          {ticket.subtitle && (
            <Text style={styles.subtitle} numberOfLines={1}>
              {ticket.subtitle}
            </Text>
          )}
          {showVenue && (
            <Text style={styles.venue} numberOfLines={1}>
              📍 {ticket.venue.name}
            </Text>
          )}
          {showTime && (
            <Text style={styles.time}>🕐 {formatTime(ticket.showTime)}</Text>
          )}
          <View style={styles.priceRow}>
            <View style={styles.priceContainer}>
              {hasDiscount ? (
                <>
                  <Text style={styles.discountPrice}>¥{ticket.discountPrice}</Text>
                  <Text style={styles.originalPrice}>¥{ticket.priceRange.min}</Text>
                </>
              ) : (
                <Text style={styles.price}>
                  ¥{ticket.priceRange.min}-{ticket.priceRange.max}
                </Text>
              )}
            </View>
            <View style={[styles.statusBadge, {backgroundColor: statusInfo.color}]}>
              <Text style={styles.statusText}>{statusInfo.text}</Text>
            </View>
          </View>
        </View>
      </TouchableOpacity>
    );
  }

  return (
    <TouchableOpacity
      style={[styles.card, Shadows.sm, {width: 160}]}
      onPress={handlePress}
      activeOpacity={0.9}
    >
      <Image
        source={{uri: ticket.posterUrl}}
        style={styles.poster}
        resizeMode="cover"
      />
      {ticket.isHot && (
        <View style={styles.hotBadge}>
          <Text style={styles.hotBadgeText}>HOT</Text>
        </View>
      )}
      {hasDiscount && (
        <View style={styles.discountBadge}>
          <Text style={styles.discountBadgeText}>惠</Text>
        </View>
      )}
      <View style={styles.info}>
        <Text style={styles.title} numberOfLines={2}>
          {ticket.name}
        </Text>
        {showVenue && (
          <Text style={styles.venueName} numberOfLines={1}>
            {ticket.venue.name}
          </Text>
        )}
        {showTime && (
          <Text style={styles.timeText}>{formatTime(ticket.showTime)}</Text>
        )}
        <View style={styles.priceRowBottom}>
          {hasDiscount ? (
            <>
              <Text style={styles.discountPriceSmall}>¥{ticket.discountPrice}</Text>
              <Text style={styles.originalPriceSmall}>¥{ticket.priceRange.min}</Text>
            </>
          ) : (
            <Text style={styles.priceSmall}>
              ¥{ticket.priceRange.min}起
            </Text>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  // 垂直布局样式
  card: {
    width: 160,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    overflow: 'hidden',
    marginRight: Spacing.sm,
  },
  poster: {
    width: '100%',
    height: 200,
    backgroundColor: Colors.gray[200],
  },
  info: {
    padding: Spacing.sm,
  },
  title: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    lineHeight: 20,
    height: 40,
  },
  venueName: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.xs,
  },
  timeText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[500],
    marginTop: Spacing.xs,
  },
  priceRowBottom: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: Spacing.sm,
  },
  priceSmall: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  // 折扣价格样式
  discountPriceSmall: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  originalPriceSmall: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    textDecorationLine: 'line-through',
    marginLeft: Spacing.xs,
  },
  // 标签样式
  hotBadge: {
    position: 'absolute',
    top: 0,
    left: 0,
    backgroundColor: Colors.error[500],
    paddingHorizontal: Spacing.xs,
    paddingVertical: 2,
    borderBottomRightRadius: BorderRadius.sm,
  },
  hotBadgeText: {
    color: Colors.white,
    fontSize: 10,
    fontWeight: '700',
  },
  discountBadge: {
    position: 'absolute',
    top: 0,
    right: 0,
    backgroundColor: Colors.warning[500],
    paddingHorizontal: Spacing.xs,
    paddingVertical: 2,
    borderBottomLeftRadius: BorderRadius.sm,
  },
  discountBadgeText: {
    color: Colors.white,
    fontSize: 10,
    fontWeight: '700',
  },
  // 水平布局样式
  cardHorizontal: {
    flexDirection: 'row',
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    overflow: 'hidden',
    marginBottom: Spacing.md,
  },
  posterHorizontal: {
    width: 100,
    height: 140,
    backgroundColor: Colors.gray[200],
  },
  contentHorizontal: {
    flex: 1,
    padding: Spacing.sm,
    justifyContent: 'space-between',
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  subtitle: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.xs,
  },
  venue: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.xs,
  },
  time: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[500],
    marginTop: Spacing.xs,
  },
  priceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: Spacing.sm,
  },
  priceContainer: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  price: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  discountPrice: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  originalPrice: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    textDecorationLine: 'line-through',
    marginLeft: Spacing.xs,
  },
  statusBadge: {
    paddingHorizontal: Spacing.xs,
    paddingVertical: 2,
    borderRadius: BorderRadius.sm,
  },
  statusText: {
    color: Colors.white,
    fontSize: 10,
    fontWeight: '600',
  },
  hotTag: {
    backgroundColor: Colors.error[500],
    paddingHorizontal: Spacing.xs,
    paddingVertical: 1,
    borderRadius: 2,
    marginLeft: Spacing.xs,
  },
  hotTagText: {
    color: Colors.white,
    fontSize: 10,
    fontWeight: '600',
  },
});

export default TicketCard;
export type {TicketCardProps};
