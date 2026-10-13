import React from 'react';
import {
  Image,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import type {Ticket} from '../../types';
import {BorderRadius, Colors, Shadows, Spacing, Typography} from '../../constants';

interface TicketCardProps {
  ticket: Ticket;
  onPress?: (ticket: Ticket) => void;
  onShare?: (ticket: Ticket) => void;
  style?: 'vertical' | 'horizontal';
  showVenue?: boolean;
  showTime?: boolean;
}

function getStatusInfo(status: Ticket['status']) {
  switch (status) {
    case 'onsale':
      return {text: '售票中', color: Colors.success[500]};
    case 'upcoming':
      return {text: '即将开售', color: Colors.warning[500]};
    case 'soldout':
      return {text: '已售罄', color: Colors.error[500]};
    case 'cancelled':
      return {text: '已下架', color: Colors.gray[500]};
    default:
      return {text: '', color: Colors.gray[500]};
  }
}

function formatTime(timeStr: string) {
  const date = new Date(timeStr);
  return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(
    date.getMinutes(),
  ).padStart(2, '0')}`;
}

const TicketCard: React.FC<TicketCardProps> = ({
  ticket,
  onPress,
  onShare,
  style = 'vertical',
  showVenue = true,
  showTime = true,
}) => {
  const statusInfo = getStatusInfo(ticket.status);
  const hasDiscount = ticket.isDiscount && ticket.discountPrice;

  if (style === 'horizontal') {
    return (
      <TouchableOpacity style={[styles.horizontalCard, Shadows.sm]} activeOpacity={0.9} onPress={() => onPress?.(ticket)}>
        <Image source={{uri: ticket.posterUrl}} style={styles.horizontalPoster} resizeMode="cover" />
        <View style={styles.horizontalBody}>
          <View style={styles.headerRow}>
            <Text style={styles.title} numberOfLines={2}>
              {ticket.name}
            </Text>
            {onShare ? (
              <TouchableOpacity style={styles.shareButton} onPress={() => onShare(ticket)}>
                <Text style={styles.shareButtonText}>分享</Text>
              </TouchableOpacity>
            ) : null}
          </View>
          {ticket.subtitle ? (
            <Text style={styles.subtitle} numberOfLines={1}>
              {ticket.subtitle}
            </Text>
          ) : null}
          {showVenue ? <Text style={styles.metaText}>{ticket.venue.name}</Text> : null}
          {showTime ? <Text style={styles.metaText}>{formatTime(ticket.showTime)}</Text> : null}
          <View style={styles.bottomRow}>
            <View style={styles.priceBlock}>
              {hasDiscount ? (
                <>
                  <Text style={styles.priceText}>¥{ticket.discountPrice}</Text>
                  <Text style={styles.strikeText}>¥{ticket.priceRange.min}</Text>
                </>
              ) : (
                <Text style={styles.priceText}>¥{ticket.priceRange.min}-¥{ticket.priceRange.max}</Text>
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
    <TouchableOpacity style={[styles.verticalCard, Shadows.sm]} activeOpacity={0.9} onPress={() => onPress?.(ticket)}>
      <Image source={{uri: ticket.posterUrl}} style={styles.verticalPoster} resizeMode="cover" />
      {onShare ? (
        <TouchableOpacity style={styles.verticalShare} onPress={() => onShare(ticket)}>
          <Text style={styles.verticalShareText}>分享</Text>
        </TouchableOpacity>
      ) : null}
      <View style={styles.verticalBody}>
        <Text style={styles.title} numberOfLines={2}>
          {ticket.name}
        </Text>
        {showVenue ? (
          <Text style={styles.smallMeta} numberOfLines={1}>
            {ticket.venue.name}
          </Text>
        ) : null}
        {showTime ? <Text style={styles.smallMeta}>{formatTime(ticket.showTime)}</Text> : null}
        <View style={styles.verticalFooter}>
          {hasDiscount ? (
            <>
              <Text style={styles.priceText}>¥{ticket.discountPrice}</Text>
              <Text style={styles.strikeText}>¥{ticket.priceRange.min}</Text>
            </>
          ) : (
            <Text style={styles.priceText}>¥{ticket.priceRange.min}起</Text>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  verticalCard: {
    width: 160,
    marginRight: Spacing.sm,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    overflow: 'hidden',
  },
  verticalPoster: {
    width: '100%',
    height: 200,
    backgroundColor: Colors.gray[200],
  },
  verticalShare: {
    position: 'absolute',
    top: Spacing.sm,
    right: Spacing.sm,
    borderRadius: 999,
    backgroundColor: 'rgba(17,24,39,0.78)',
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  verticalShareText: {
    color: Colors.white,
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
  },
  verticalBody: {
    padding: Spacing.sm,
  },
  verticalFooter: {
    marginTop: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
  },
  horizontalCard: {
    flexDirection: 'row',
    marginBottom: Spacing.md,
    borderRadius: BorderRadius.md,
    backgroundColor: Colors.white,
    overflow: 'hidden',
  },
  horizontalPoster: {
    width: 100,
    height: 140,
    backgroundColor: Colors.gray[200],
  },
  horizontalBody: {
    flex: 1,
    padding: Spacing.sm,
    justifyContent: 'space-between',
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.xs,
  },
  title: {
    flex: 1,
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
    lineHeight: 20,
  },
  shareButton: {
    borderWidth: 1,
    borderColor: Colors.primary[200],
    borderRadius: BorderRadius.sm,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  shareButtonText: {
    color: Colors.primary[500],
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
  },
  subtitle: {
    marginTop: 4,
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  metaText: {
    marginTop: 4,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  smallMeta: {
    marginTop: 4,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  bottomRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: Spacing.sm,
  },
  priceBlock: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  priceText: {
    fontSize: Typography.body.fontSize,
    color: Colors.error[500],
    fontWeight: '700',
  },
  strikeText: {
    marginLeft: Spacing.xs,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    textDecorationLine: 'line-through',
  },
  statusBadge: {
    borderRadius: BorderRadius.sm,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  statusText: {
    color: Colors.white,
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
  },
});

export default TicketCard;
export type {TicketCardProps};
