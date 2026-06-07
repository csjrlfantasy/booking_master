/**
 * TicketListScreen - 票务列表页面
 * 包含：分类筛选、列表展示、分页加载、下拉刷新
 */
import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  RefreshControl,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import {useNavigation, useRoute, RouteProp} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {TicketListParams, TicketType} from '../../types';
import type {RootStackParamList, TicketStackParamList} from '../../navigation/types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {ticketApi} from '../../services/api';
import ticketMock from '../../services/mock/ticketMock';
import {TicketCard} from '../../components/common';
import Loading from '../../components/common/Loading';

// 导航类型
type TicketListNavigationProp = NativeStackNavigationProp<
  RootStackParamList & TicketStackParamList,
  'TicketList'
>;
type TicketListRouteProp = RouteProp<TicketStackParamList, 'TicketList'>;

// 分类筛选项
interface FilterOption {
  value: TicketType | '';
  label: string;
}

interface SortOption {
  value: 'time' | 'sales' | 'price' | '';
  label: string;
}

const filterOptions: FilterOption[] = [
  {value: '', label: '全部'},
  {value: 'concert', label: '演唱会'},
  {value: 'drama', label: '话剧歌剧'},
  {value: 'exhibition', label: '展览'},
  {value: 'sports', label: '体育'},
  {value: 'activity', label: '休闲'},
];

// 排序筛选项
const sortOptions: SortOption[] = [
  {value: '', label: '默认'},
  {value: 'time', label: '时间'},
  {value: 'sales', label: '销量'},
  {value: 'price', label: '价格'},
];

const TicketListScreen: React.FC = () => {
  const navigation = useNavigation<TicketListNavigationProp>();
  const route = useRoute<TicketListRouteProp>();
  
  // 从路由参数获取初始筛选条件
  const initialType = route.params?.type || '';
  const initialCategory = route.params?.category || '';

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  
  // 筛选状态
  const [typeFilter, setTypeFilter] = useState<TicketType | ''>(initialType as TicketType | '');
  const [sortBy, setSortBy] = useState<'time' | 'sales' | 'price' | ''>('');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  
  // 列表数据
  const [ticketList, setTicketList] = useState<any[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [total, setTotal] = useState(0);

  // 构建请求参数
  const buildParams = useCallback((pageNum: number): TicketListParams => {
    const params: TicketListParams = {
      page: pageNum,
      pageSize: 10,
    };
    
    if (typeFilter) {
      params.type = typeFilter as TicketType;
    }
    
    if (sortBy) {
      params.sortBy = sortBy as 'time' | 'sales' | 'price';
      params.sortOrder = sortOrder;
    }
    
    return params;
  }, [typeFilter, sortBy, sortOrder]);

  // 加载数据 - 使用真实API
  const loadData = useCallback(async (pageNum: number = 1, reset: boolean = true) => {
    const params = buildParams(pageNum);
    try {
      // 使用真实API
      const result = await ticketApi.getTicketList({
        pageNum: params.page,
        pageSize: params.pageSize,
      });
      
      if (reset) {
        setTicketList(result.list);
      } else {
        setTicketList(prev => [...prev, ...result.list]);
      }
      
      setHasMore(result.hasNext);
      setTotal(result.total);
      setPage(pageNum);
      
      // 旧代码保留用于API不可用时的降级处理
      // const result = ticketMock.getTicketList(params);
      // setTicketList(result.list);
      // setHasMore(result.hasMore);
    } catch (error) {
      console.error('加载票务列表失败:', error);
      // API失败时降级使用Mock数据
      try {
        const result = ticketMock.getTicketList(params);
        if (reset) {
          setTicketList(result.list);
        } else {
          setTicketList(prev => [...prev, ...result.list]);
        }
        setHasMore(result.hasMore);
        setTotal(result.total);
      } catch (mockError) {
        console.error('Mock数据加载也失败:', mockError);
      }
    } finally {
      setLoading(false);
    }
  }, [buildParams]);

  // 首次加载
  useEffect(() => {
    loadData();
  }, []);

  // 处理路由参数变化
  useEffect(() => {
    if (initialType) {
      setTypeFilter(initialType as TicketType);
    }
  }, [initialType]);

  // 筛选条件变化时重新加载
  useEffect(() => {
    setLoading(true);
    loadData(1, true);
  }, [typeFilter, sortBy, sortOrder]);

  // 下拉刷新
  const handleRefresh = async () => {
    setRefreshing(true);
    await loadData(1, true);
    setRefreshing(false);
  };

  // 加载更多
  const handleLoadMore = async () => {
    if (!hasMore) return;
    await loadData(page + 1, false);
  };

  // 跳转到票务详情
  const handleTicketPress = (ticket: any) => {
    navigation.navigate('TicketDetail', {ticketId: ticket.id});
  };

  // 切换分类筛选
  const handleTypeFilterChange = (value: TicketType | '') => {
    setTypeFilter(value);
  };

  // 切换排序
  const handleSortChange = (value: 'time' | 'sales' | 'price' | '') => {
    if (sortBy === value) {
      // 再次点击切换排序方向
      setSortOrder(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(value);
      setSortOrder('desc');
    }
  };

  // 渲染票务项
  const renderTicketItem = ({item}: {item: any}) => (
    <TicketCard
      ticket={item}
      onPress={handleTicketPress}
      style="horizontal"
      showVenue={true}
      showTime={true}
    />
  );

  // 渲染空状态
  const renderEmpty = () => (
    <View style={styles.emptyContainer}>
      <Text style={styles.emptyIcon}>🎫</Text>
      <Text style={styles.emptyText}>暂无票务</Text>
      <Text style={styles.emptySubtext}>试试其他筛选条件</Text>
    </View>
  );

  // 渲染底部
  const renderFooter = () => {
    if (!hasMore) {
      return (
        <Text style={styles.noMoreText}>— 已经到底啦 —</Text>
      );
    }
    return null;
  };

  if (loading && page === 1) {
    return <Loading fullScreen text="加载中..." />;
  }

  return (
    <View style={styles.container}>
      {/* 筛选栏 */}
      <View style={styles.filterBar}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filterScroll}
        >
          {filterOptions.map(option => (
            <TouchableOpacity
              key={option.value || 'all'}
              style={[
                styles.filterItem,
                typeFilter === option.value && styles.filterItemActive,
              ]}
              onPress={() => handleTypeFilterChange(option.value)}
            >
              <Text
                style={[
                  styles.filterText,
                  typeFilter === option.value && styles.filterTextActive,
                ]}
              >
                {option.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* 排序栏 */}
      <View style={styles.sortBar}>
        <Text style={styles.totalText}>
          共 {total} 个票务
        </Text>
        <View style={styles.sortOptions}>
          {sortOptions.slice(1).map(option => (
            <TouchableOpacity
              key={option.value}
              style={[
                styles.sortItem,
                sortBy === option.value && styles.sortItemActive,
              ]}
              onPress={() => handleSortChange(option.value as 'time' | 'sales' | 'price')}
            >
              <Text
                style={[
                  styles.sortText,
                  sortBy === option.value && styles.sortTextActive,
                ]}
              >
                {option.label}
              </Text>
              {sortBy === option.value && (
                <Text style={styles.sortArrow}>
                  {sortOrder === 'asc' ? '↑' : '↓'}
                </Text>
              )}
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* 票务列表 */}
      <FlatList
        data={ticketList}
        keyExtractor={item => item.id}
        renderItem={renderTicketItem}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={renderEmpty}
        ListFooterComponent={renderFooter}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
        }
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.5}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  // 筛选栏
  filterBar: {
    backgroundColor: Colors.white,
    ...Shadows.sm,
  },
  filterScroll: {
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
  },
  filterItem: {
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.xs,
    marginRight: Spacing.sm,
    borderRadius: BorderRadius.lg,
    backgroundColor: Colors.gray[100],
  },
  filterItemActive: {
    backgroundColor: Colors.primary[500],
  },
  filterText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
  },
  filterTextActive: {
    color: Colors.white,
    fontWeight: '600',
  },
  // 排序栏
  sortBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[200],
  },
  totalText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  sortOptions: {
    flexDirection: 'row',
  },
  sortItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.sm,
    marginLeft: Spacing.sm,
  },
  sortItemActive: {},
  sortText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  sortTextActive: {
    color: Colors.primary[500],
    fontWeight: '600',
  },
  sortArrow: {
    fontSize: 10,
    color: Colors.primary[500],
    marginLeft: 2,
  },
  // 列表
  listContent: {
    padding: Spacing.md,
    flexGrow: 1,
  },
  noMoreText: {
    textAlign: 'center',
    paddingVertical: Spacing.lg,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
  },
  // 空状态
  emptyContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 100,
  },
  emptyIcon: {
    fontSize: 48,
    marginBottom: Spacing.md,
  },
  emptyText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[600],
    fontWeight: '600',
  },
  emptySubtext: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    marginTop: Spacing.xs,
  },
});

export default TicketListScreen;
