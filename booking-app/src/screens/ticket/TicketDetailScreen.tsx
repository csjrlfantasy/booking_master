/**
 * TicketDetailScreen - 票务详情页面
 * 包含：票务信息展示、座位选择、立即购买按钮
 */
import React, {useState, useEffect} from 'react';
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  Image,
  TouchableOpacity,
  StatusBar,
  Alert,
} from 'react-native';
import {useNavigation, useRoute, RouteProp} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {Ticket, PriceLevel} from '../../types';
import type {RootStackParamList, TicketStackParamList} from '../../navigation/types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {ticketApi} from '../../services/api';
import ticketMock from '../../services/mock/ticketMock';
import {Button} from '../../components/common';
import Loading from '../../components/common/Loading';
import {shareTicket} from '../../utils/shareTicket';

// 导航类型
type TicketDetailNavigationProp = NativeStackNavigationProp<
  RootStackParamList & TicketStackParamList,
  'TicketDetail'
>;
type TicketDetailRouteProp = RouteProp<TicketStackParamList, 'TicketDetail'>;

const TicketDetailScreen: React.FC = () => {
  const navigation = useNavigation<TicketDetailNavigationProp>();
  const route = useRoute<TicketDetailRouteProp>();
  const {ticketId} = route.params;

  const [loading, setLoading] = useState(true);
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [selectedPriceLevel, setSelectedPriceLevel] = useState<PriceLevel | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [reminded, setReminded] = useState(false);
  const [reminderLoading, setReminderLoading] = useState(false);

  // 加载票务详情 - 使用真实API
  useEffect(() => {
    const loadDetail = async () => {
      try {
        // 使用真实API获取数据
        const data = await ticketApi.getTicketDetail(ticketId);
        setTicket(data as any);

        // 默认选中第一个价格等级
        if ((data as any)?.priceLevels && (data as any).priceLevels.length > 0) {
          setSelectedPriceLevel((data as any).priceLevels[0]);
        }
        try {
          const reminder = await ticketApi.getTicketReminder(ticketId);
          setReminded(Boolean(reminder?.reminded));
        } catch {
          setReminded(false);
        }

        // 旧代码保留用于API失败时的降级
        // const data = ticketMock.getTicketDetail(ticketId);
      } catch (error) {
        console.error('加载票务详情失败:', error);
        // API失败时降级使用Mock数据
        try {
          const data = ticketMock.getTicketDetail(ticketId);
          setTicket(data || null);
          if (data?.priceLevels && data.priceLevels.length > 0) {
            setSelectedPriceLevel(data.priceLevels[0]);
          }
        } catch (mockError) {
          console.error('Mock数据也加载失败:', mockError);
        }
      } finally {
        setLoading(false);
      }
    };

    loadDetail();
  }, [ticketId]);

  // 格式化时间
  const formatDateTime = (timeStr: string) => {
    const date = new Date(timeStr);
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()} ${weekDays[date.getDay()]} ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
  };

  // 获取状态信息
  const getStatusInfo = () => {
    if (!ticket) return {text: '', color: ''};
    const rawStatus = ticket.status as any;

    switch (rawStatus) {
      case 2:
      case 'onsale':
        return {text: '售票中', color: Colors.success[500]};
      case 1:
      case 'upcoming':
        return {text: '即将开售', color: Colors.warning[500]};
      case 'soldout':
        return {text: '已售罄', color: Colors.error[500]};
      case 'cancelled':
        return {text: '已取消', color: Colors.gray[500]};
      default:
        return {text: '', color: ''};
    }
  };

  const statusInfo = getStatusInfo();

  // 减少数量
  const handleDecrease = () => {
    if (quantity > 1) {
      setQuantity(prev => prev - 1);
    }
  };

  // 增加数量
  const handleIncrease = () => {
    if (selectedPriceLevel && quantity < selectedPriceLevel.stock) {
      setQuantity(prev => prev + 1);
    }
  };

  // 选择价格等级
  const handleSelectPriceLevel = (priceLevel: PriceLevel) => {
    setSelectedPriceLevel(priceLevel);
    setQuantity(1);
  };

  // 立即购买
  const handleBuyNow = () => {
    if (!selectedPriceLevel || !ticket) return;

    // 跳转到确认页或购物车
    navigation.navigate('Cart', {
      screen: 'Checkout',
      params: {
        cartIds: [`${ticket.id}:${selectedPriceLevel.id}:${quantity}`]
      }
    });
  };

  const handleShare = () => {
    if (!ticket) {
      return;
    }
    shareTicket(ticket.name, ticket.id).catch(error => {
      console.error('share ticket detail failed', error);
    });
  };

  const handleRemind = async () => {
    if (!ticket) return;
    setReminderLoading(true);
    try {
      const result = await ticketApi.remindTicket(String(ticket.id));
      setReminded(Boolean(result?.reminded));
      Alert.alert('提醒已设置', '开售前会提醒你，记得来看票多星。');
    } catch (error: any) {
      Alert.alert('设置失败', error?.message || '请先登录后再设置提醒');
    } finally {
      setReminderLoading(false);
    }
  };

  // 跳转到场馆地图
  const handleViewMap = () => {
    if (!ticket) return;
    navigation.navigate('VenueMap', {
      venueId: ticket.venue.id,
      ticketId: ticket.id
    });
  };

  if (loading) {
    return <Loading fullScreen text="加载中..." />;
  }

  if (!ticket) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>票务不存在</Text>
        <Button title="返回" onPress={() => navigation.goBack()} />
      </View>
    );
  }

  const totalPrice = selectedPriceLevel ? selectedPriceLevel.price * quantity : 0;
  const rawStatus = ticket.status as any;
  const isOnSale = rawStatus === 'onsale' || rawStatus === 2;
  const isUpcoming = rawStatus === 'upcoming' || rawStatus === 1;

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" translucent backgroundColor="transparent" />
      <ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
      >
        {/* 海报图 */}
        <Image
          source={{uri: ticket.posterUrl}}
          style={styles.poster}
          resizeMode="cover"
        />

        {/* 状态标签 */}
        <View style={[styles.statusBadge, {backgroundColor: statusInfo.color}]}>
          <Text style={styles.statusText}>{statusInfo.text}</Text>
        </View>

        {/* 基本信息 */}
        <View style={styles.infoSection}>
          <View style={styles.titleRow}>
            <Text style={styles.title}>{ticket.name}</Text>
            {ticket.isHot && (
              <View style={styles.hotBadge}>
                <Text style={styles.hotBadgeText}>HOT</Text>
              </View>
            )}
            <TouchableOpacity style={styles.shareAction} onPress={handleShare}>
              <Text style={styles.shareActionText}>分享</Text>
            </TouchableOpacity>
          </View>

          {ticket.subtitle && (
            <Text style={styles.subtitle}>{ticket.subtitle}</Text>
          )}

          <View style={styles.timeInfo}>
            <Text style={styles.timeLabel}>🕐 演出时间</Text>
            <Text style={styles.timeValue}>{formatDateTime(ticket.showTime)}</Text>
            {ticket.endTime && (
              <Text style={styles.timeValue}>至 {formatDateTime(ticket.endTime)}</Text>
            )}
          </View>

          <TouchableOpacity style={styles.venueRow} onPress={handleViewMap}>
            <Text style={styles.venueLabel}>📍 演出场馆</Text>
            <View style={styles.venueInfo}>
              <Text style={styles.venueName}>{ticket.venue.name}</Text>
              <Text style={styles.venueAddress}>{ticket.venue.address}</Text>
            </View>
            <Text style={styles.mapArrow}>›</Text>
          </TouchableOpacity>
        </View>

        {/* 座位/价格等级选择 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>选择座位</Text>
          <View style={styles.priceLevels}>
            {ticket.priceLevels.map(level => (
              <TouchableOpacity
                key={level.id}
                style={[
                  styles.priceLevelItem,
                  selectedPriceLevel?.id === level.id && styles.priceLevelItemActive,
                  level.stock === 0 && styles.priceLevelItemDisabled,
                ]}
                onPress={() => level.stock > 0 && handleSelectPriceLevel(level)}
                disabled={level.stock === 0}
              >
                <View style={styles.priceLevelInfo}>
                  <Text style={[
                    styles.priceLevelName,
                    selectedPriceLevel?.id === level.id && styles.priceLevelNameActive,
                  ]}>
                    {level.name}
                  </Text>
                  <Text style={styles.priceLevelStock}>
                    {level.stock > 0 ? `剩余${level.stock}张` : '已售罄'}
                  </Text>
                </View>
                <Text style={[
                  styles.priceLevelPrice,
                  selectedPriceLevel?.id === level.id && styles.priceLevelPriceActive,
                ]}>
                  ¥{level.price}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* 数量选择 */}
        {selectedPriceLevel && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>购买数量</Text>
            <View style={styles.quantitySelector}>
              <TouchableOpacity
                style={[styles.quantityButton, quantity <= 1 && styles.quantityButtonDisabled]}
                onPress={handleDecrease}
                disabled={quantity <= 1}
              >
                <Text style={styles.quantityButtonText}>-</Text>
              </TouchableOpacity>
              <Text style={styles.quantityValue}>{quantity}</Text>
              <TouchableOpacity
                style={[styles.quantityButton, quantity >= selectedPriceLevel.stock && styles.quantityButtonDisabled]}
                onPress={handleIncrease}
                disabled={quantity >= selectedPriceLevel.stock}
              >
                <Text style={styles.quantityButtonText}>+</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}

        {/* 购票须知 */}
        {ticket.notice && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>购票须知</Text>
            <Text style={styles.noticeText}>{ticket.notice}</Text>
          </View>
        )}

        {/* 艺人信息 */}
        {ticket.performers && ticket.performers.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>演出人员</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false}>
              {ticket.performers.map(performer => (
                <View key={performer.id} style={styles.performerItem}>
                  <Image
                    source={{uri: performer.avatar}}
                    style={styles.performerAvatar}
                  />
                  <Text style={styles.performerName}>{performer.name}</Text>
                  {performer.role && (
                    <Text style={styles.performerRole}>{performer.role}</Text>
                  )}
                </View>
              ))}
            </ScrollView>
          </View>
        )}

        {/* 简介 */}
        {ticket.description && (
          <View style={[styles.section, styles.lastSection]}>
            <Text style={styles.sectionTitle}>详细介绍</Text>
            <Text style={styles.description}>{ticket.description}</Text>
          </View>
        )}
      </ScrollView>

      {/* 底部购买栏 */}
      <View style={styles.bottomBar}>
        <View style={styles.bottomInfo}>
          <Text style={styles.totalLabel}>合计</Text>
          <Text style={styles.totalPrice}>¥{totalPrice}</Text>
        </View>
        <Button
          title={isUpcoming ? (reminded ? '已提醒' : '提醒我') : '立即购买'}
          onPress={isUpcoming ? handleRemind : handleBuyNow}
          disabled={isUpcoming ? reminded || reminderLoading : !selectedPriceLevel || !isOnSale}
          style={styles.buyButton}
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  scrollView: {
    flex: 1,
  },
  errorContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  errorText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
    marginBottom: Spacing.md,
  },
  // 海报
  poster: {
    width: '100%',
    height: 300,
    backgroundColor: Colors.gray[200],
  },
  statusBadge: {
    position: 'absolute',
    top: 50,
    right: Spacing.md,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xs,
    borderRadius: BorderRadius.sm,
  },
  statusText: {
    color: Colors.white,
    fontSize: Typography.caption.fontSize,
    fontWeight: '600',
  },
  // 信息区块
  infoSection: {
    backgroundColor: Colors.white,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  title: {
    flex: 1,
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
    lineHeight: 28,
  },
  hotBadge: {
    backgroundColor: Colors.error[500],
    paddingHorizontal: Spacing.xs,
    paddingVertical: 2,
    borderRadius: BorderRadius.sm,
    marginLeft: Spacing.sm,
  },
  hotBadgeText: {
    color: Colors.white,
    fontSize: 10,
    fontWeight: '700',
  },
  shareAction: {
    marginLeft: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.primary[200],
    borderRadius: BorderRadius.sm,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  shareActionText: {
    color: Colors.primary[500],
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
    marginTop: Spacing.xs,
  },
  timeInfo: {
    marginTop: Spacing.md,
  },
  timeLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
    fontWeight: '600',
  },
  timeValue: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
    marginTop: Spacing.xs,
  },
  venueRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: Spacing.md,
  },
  venueLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
    fontWeight: '600',
  },
  venueInfo: {
    flex: 1,
    marginLeft: Spacing.sm,
  },
  venueName: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
    fontWeight: '500',
  },
  venueAddress: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  mapArrow: {
    fontSize: 20,
    color: Colors.gray[400],
  },
  // 通用区块样式
  section: {
    backgroundColor: Colors.white,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
  },
  lastSection: {
    marginBottom: 100,
  },
  sectionTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
    marginBottom: Spacing.sm,
  },
  // 价格等级
  priceLevels: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  priceLevelItem: {
    width: '48%',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: Spacing.sm,
    marginRight: '2%',
    marginBottom: Spacing.sm,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    borderColor: Colors.gray[300],
  },
  priceLevelItemActive: {
    borderColor: Colors.primary[500],
    backgroundColor: Colors.primary[50],
  },
  priceLevelItemDisabled: {
    opacity: 0.5,
  },
  priceLevelInfo: {},
  priceLevelName: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  priceLevelNameActive: {
    color: Colors.primary[500],
    fontWeight: '600',
  },
  priceLevelStock: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  priceLevelPrice: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  priceLevelPriceActive: {
    color: Colors.primary[500],
  },
  // 数量选择
  quantitySelector: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
  },
  quantityButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: BorderRadius.sm,
    borderWidth: 1,
    borderColor: Colors.primary[500],
  },
  quantityButtonDisabled: {
    borderColor: Colors.gray[300],
    opacity: 0.5,
  },
  quantityButtonText: {
    fontSize: 20,
    color: Colors.primary[500],
    fontWeight: '600',
  },
  quantityValue: {
    fontSize: Typography.bodyLarge.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginHorizontal: Spacing.lg,
    minWidth: 30,
    textAlign: 'center',
  },
  // 须知
  noticeText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
    lineHeight: 22,
  },
  // 艺人
  performerItem: {
    alignItems: 'center',
    marginRight: Spacing.lg,
  },
  performerAvatar: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: Colors.gray[200],
  },
  performerName: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
    marginTop: Spacing.xs,
  },
  performerRole: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  // 简介
  description: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
    lineHeight: 24,
  },
  // 底部购买栏
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    paddingBottom: Spacing.lg,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[200],
    ...Shadows.md,
  },
  bottomInfo: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  totalLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
  },
  totalPrice: {
    fontSize: Typography.h2.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
    marginLeft: Spacing.sm,
  },
  buyButton: {
    width: 140,
  },
});

export default TicketDetailScreen;
