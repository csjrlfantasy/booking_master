/**
 * HomeScreen - 首页
 * 包含：顶部轮播图、热门推荐、分类快捷入口、票务列表
 */
import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  FlatList,
  RefreshControl,
  TouchableOpacity,
  StatusBar,
} from 'react-native';
import {useNavigation, useFocusEffect} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {MainTabScreenProps} from '../../navigation/types';
import type {RootStackParamList, MainTabParamList} from '../../navigation/types';
import type {Ticket, TicketType} from '../../types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {ticketApi} from '../../services/api';
import ticketMock from '../../services/mock/ticketMock';
import Banner, {BannerItem} from '../../components/common/Banner';
import TicketCard from '../../components/common/TicketCard';
import Loading from '../../components/common/Loading';

// 导航类型
type HomeScreenNavigationProp = NativeStackNavigationProp<
  RootStackParamList & MainTabParamList,
  'HomeTab'
>;

// 轮播图Mock数据
const mockBanners: BannerItem[] = [
  {id: '1', title: '周杰伦演唱会', imageUrl: '', action: 'ticket/1'},
  {id: '2', title: 'teamLab艺术展', imageUrl: '', action: 'ticket/3'},
  {id: '3', title: 'CBA季后赛', imageUrl: '', action: 'ticket/4'},
];

// 分类入口Mock数据
interface CategoryItem {
  id: string;
  name: string;
  icon: string;
  type: TicketType;
}
const mockCategories: CategoryItem[] = [
  {id: 'concert', name: '演唱会', icon: '🎤', type: 'concert'},
  {id: 'drama', name: '话剧歌剧', icon: '🎭', type: 'drama'},
  {id: 'exhibition', name: '展览', icon: '🎨', type: 'exhibition'},
  {id: 'sports', name: '体育', icon: '⚽', type: 'sports'},
  {id: 'activity', name: '休闲', icon: '🎪', type: 'activity'},
];

const HomeScreen: React.FC = () => {
  const navigation = useNavigation<HomeScreenNavigationProp>();

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // 数据状态
  const [banners, setBanners] = useState<BannerItem[]>(mockBanners);
  const [hotTickets, setHotTickets] = useState<Ticket[]>([]);
  const [ticketList, setTicketList] = useState<Ticket[]>([]);
  const [categories] = useState<CategoryItem[]>(mockCategories);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);

  // 加载数据 - 使用真实API
  const loadData = useCallback(async () => {
    try {
      // 使用真实API获取数据
      const [bannerRes, hotRes, listRes] = await Promise.all([
        ticketApi.getBanners('app'),
        ticketApi.getHotTickets(5),
        ticketApi.getTicketList({pageNum: 1, pageSize: 10}),
      ]);

      const nextBanners = (bannerRes || []).map(item => ({
        id: String(item.id),
        title: item.title,
        imageUrl: item.imageUrl,
        action: item.linkType === 'ticket' && item.linkTarget ? `ticket/${item.linkTarget}` : item.linkTarget || '',
      }));
      setBanners(nextBanners.length > 0 ? nextBanners : mockBanners);
      setHotTickets(hotRes as any[]);
      setTicketList(listRes.list as any[]);
      setHasMore(listRes.hasNext);
      setPage(1);

      // 旧代码保留用于API不可用时的降级处理
      // const hotData = ticketMock.getHotTickets(5);
      // const listData = ticketMock.getTicketList({page: 1, pageSize: 10});
      // setHotTickets(hotData);
      // setTicketList(listData.list);
      // setHasMore(listData.hasMore);
    } catch (error) {
      console.error('加载首页数据失败:', error);
      // API失败时降级使用Mock数据
      try {
        const hotData = ticketMock.getHotTickets(5);
        const listData = ticketMock.getTicketList({page: 1, pageSize: 10});
        setBanners(mockBanners);
        setHotTickets(hotData);
        setTicketList(listData.list);
        setHasMore(listData.hasMore);
      } catch (mockError) {
        console.error('Mock数据也加载失败:', mockError);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  // 首次加载
  useEffect(() => {
    loadData();
  }, [loadData]);

  // 页面聚焦时刷新
  useFocusEffect(
    useCallback(() => {
      // 可选：每次进入首页时刷新数据
    }, [])
  );

  // 下拉刷新
  const handleRefresh = async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
  };

  // 加载更多 - 使用真实API
  const handleLoadMore = async () => {
    if (!hasMore) return;

    try {
      const nextPage = page + 1;
      // 使用真实API
      const listRes = await ticketApi.getTicketList({pageNum: nextPage, pageSize: 10});

      setTicketList(prev => [...prev, ...(listRes.list as any[])]);
      setHasMore(listRes.hasNext);
      setPage(nextPage);

      // 旧代码保留用于API不可用时的降级处理
      // const listData = ticketMock.getTicketList({page: nextPage, pageSize: 10});
      // setTicketList(prev => [...prev, ...listData.list]);
      // setHasMore(listData.hasMore);
    } catch (error) {
      console.error('加载更多数据失败:', error);
      // 静默失败，不影响用户
    }
  };

  // 跳转到搜索页面
  const handleSearchPress = () => {
    navigation.navigate('Search', {screen: 'Search', params: {keyword: ''}});
  };

  // 跳转到票务详情
  const handleTicketPress = (ticket: Ticket) => {
    navigation.navigate('Ticket', {screen: 'TicketDetail', params: {ticketId: ticket.id}});
  };

  // 跳转到分类页面
  const handleCategoryPress = (category: CategoryItem) => {
    navigation.navigate('Search', {screen: 'Search', params: {category: category.type}});
  };

  // 轮播图点击
  const handleBannerPress = (item: BannerItem) => {
    if (item.action) {
      const [screen, params] = item.action.split('/');
      if (screen === 'ticket' && params) {
        handleTicketPress({id: params} as Ticket);
      }
    }
  };

  if (loading) {
    return <Loading fullScreen text="加载中..." />;
  }

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor={Colors.white} />
      <ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
        }
      >
        {/* 搜索框 */}
        <TouchableOpacity
          style={styles.searchBar}
          onPress={handleSearchPress}
          activeOpacity={0.9}
        >
          <Text style={styles.searchIcon}>🔍</Text>
          <Text style={styles.searchText}>搜索演出、赛事、展览...</Text>
        </TouchableOpacity>

        {/* 轮播图 */}
        <View style={styles.section}>
          <Banner
            items={banners}
            height={180}
            autoPlay
            onPress={handleBannerPress}
          />
        </View>

        {/* 分类快捷入口 */}
        <View style={styles.categoryContainer}>
          {categories.map(category => (
            <TouchableOpacity
              key={category.id}
              style={styles.categoryItem}
              onPress={() => handleCategoryPress(category)}
            >
              <Text style={styles.categoryIcon}>{category.icon}</Text>
              <Text style={styles.categoryName}>{category.name}</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* 热门推荐 */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>热门推荐</Text>
            <TouchableOpacity onPress={() => navigation.navigate('Search', {screen: 'Search', params: {keyword: ''}})}>
              <Text style={styles.sectionMore}>更多 ›</Text>
            </TouchableOpacity>
          </View>
          <FlatList
            horizontal
            showsHorizontalScrollIndicator={false}
            data={hotTickets}
            keyExtractor={item => item.id}
            renderItem={({item}) => (
              <TicketCard
                ticket={item}
                onPress={handleTicketPress}
                style="vertical"
              />
            )}
            contentContainerStyle={styles.horizontalList}
          />
        </View>

        {/* 全部票务 */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>全部票务</Text>
          </View>
          <View style={styles.ticketList}>
            {ticketList.map(ticket => (
              <TicketCard
                key={ticket.id}
                ticket={ticket}
                onPress={handleTicketPress}
                style="horizontal"
              />
            ))}
          </View>

          {/* 加载更多 */}
          {hasMore && (
            <TouchableOpacity
              style={styles.loadMoreButton}
              onPress={handleLoadMore}
            >
              <Text style={styles.loadMoreText}>点击加载更多</Text>
            </TouchableOpacity>
          )}

          {!hasMore && ticketList.length > 0 && (
            <Text style={styles.noMoreText}>— 已经到底啦 —</Text>
          )}
        </View>
      </ScrollView>
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
  // 搜索栏
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginTop: Spacing.sm,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.lg,
    ...Shadows.sm,
  },
  searchIcon: {
    fontSize: 16,
    marginRight: Spacing.sm,
  },
  searchText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[400],
  },
  // 区块样式
  section: {
    marginTop: Spacing.md,
    paddingHorizontal: Spacing.md,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.sm,
  },
  sectionTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  sectionMore: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
  },
  // 分类入口
  categoryContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    paddingVertical: Spacing.md,
    borderRadius: BorderRadius.md,
    ...Shadows.sm,
  },
  categoryItem: {
    alignItems: 'center',
  },
  categoryIcon: {
    fontSize: 28,
    marginBottom: Spacing.xs,
  },
  categoryName: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[700],
  },
  // 列表样式
  horizontalList: {
    paddingVertical: Spacing.sm,
  },
  ticketList: {
    paddingTop: Spacing.sm,
  },
  loadMoreButton: {
    alignItems: 'center',
    paddingVertical: Spacing.lg,
  },
  loadMoreText: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
  },
  noMoreText: {
    textAlign: 'center',
    paddingVertical: Spacing.lg,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
  },
});

export default HomeScreen;
