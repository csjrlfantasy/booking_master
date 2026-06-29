/**
 * SearchScreen - 搜索页面
 * 包含：搜索框、搜索历史、搜索结果展示
 */
import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  TextInput,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  Keyboard,
} from 'react-native';
import {useNavigation, useRoute, RouteProp} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {Ticket, TicketType} from '../../types';
import type {RootStackParamList, SearchStackParamList} from '../../navigation/types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {ticketApi} from '../../services/api';
import ticketMock from '../../services/mock/ticketMock';
import {TicketCard} from '../../components/common';

// 导航类型
type SearchNavigationProp = NativeStackNavigationProp<RootStackParamList, 'Search'>;
type SearchRouteProp = RouteProp<SearchStackParamList, 'Search'>;

// 搜索历史记录
const SEARCH_HISTORY_KEY = 'search_history';
const MAX_HISTORY_COUNT = 10;

const SearchScreen: React.FC = () => {
  const navigation = useNavigation<SearchNavigationProp>();
  const route = useRoute<SearchRouteProp>();
  
  // 从路由参数获取初始关键词
  const initialKeyword = route.params?.keyword || '';
  const initialCategory = route.params?.category || '';

  const [keyword, setKeyword] = useState(initialKeyword);
  const [searchResults, setSearchResults] = useState<Ticket[]>([]);
  const [searchHistory, setSearchHistory] = useState<string[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  // 加载搜索历史
  useEffect(() => {
    // 从本地存储加载搜索历史（这里使用内存模拟）
    const history = loadSearchHistory();
    setSearchHistory(history);
  }, []);

  // 处理初始分类参数
  useEffect(() => {
    if (initialCategory) {
      // 根据分类搜索
      handleSearch(initialCategory, true);
    } else if (initialKeyword) {
      handleSearch(initialKeyword, true);
    }
  }, [initialKeyword, initialCategory]);

  // 模拟从存储加载搜索历史
  const loadSearchHistory = (): string[] => {
    // 实际项目中应从AsyncStorage或MMKV读取
    return ['周杰伦', '演唱会', 'teamLab', '篮球'];
  };

  // 模拟保存搜索历史
  const saveSearchHistory = (history: string[]) => {
    // 实际项目中应保存到AsyncStorage或MMKV
    setSearchHistory(history);
  };

  // 搜索 - 使用真实API
  const handleSearch = useCallback(async (searchKeyword: string, fromCategory = false) => {
    if (!searchKeyword.trim() && !fromCategory) {
      return;
    }
    
    Keyboard.dismiss();
    setIsSearching(true);
    setHasSearched(true);
    
    // 添加到搜索历史
    if (!fromCategory) {
      const newHistory = [searchKeyword, ...searchHistory.filter(h => h !== searchKeyword)].slice(0, MAX_HISTORY_COUNT);
      saveSearchHistory(newHistory);
    }
    
    try {
      // 使用真实API执行搜索
      let results;
      if (fromCategory) {
        // 按分类搜索
        results = await ticketApi.getTicketsByCategory(1, {keyword: searchKeyword});
      } else {
        // 按关键词搜索
        results = await ticketApi.searchTickets(searchKeyword, {pageNum: 1, pageSize: 20});
      }
      setSearchResults(results.list || []);
      
      // 旧代码保留用于API失败时的降级
      // const params = fromCategory 
      //   ? {type: searchKeyword as TicketType}
      //   : {keyword: searchKeyword};
      // const results = ticketMock.getTicketList(params);
      // setSearchResults(results.list);
    } catch (error) {
      console.error('搜索失败:', error);
      // API失败时降级使用Mock数据
      try {
        const params = fromCategory 
          ? {type: searchKeyword as TicketType}
          : {keyword: searchKeyword};
        const results = ticketMock.getTicketList(params);
        setSearchResults(results.list);
      } catch (mockError) {
        console.error('Mock搜索也失败:', mockError);
        setSearchResults([]);
      }
    } finally {
      setIsSearching(false);
      if (!fromCategory) {
        setKeyword(searchKeyword);
      }
    }
  }, [searchHistory]);

  // 清除搜索历史
  const clearSearchHistory = () => {
    saveSearchHistory([]);
  };

  // 删除单条历史记录
  const deleteHistoryItem = (item: string) => {
    const newHistory = searchHistory.filter(h => h !== item);
    saveSearchHistory(newHistory);
  };

  // 跳转到票务详情
  const handleTicketPress = (ticket: Ticket) => {
    navigation.navigate('Ticket', {screen: 'TicketDetail', params: {ticketId: ticket.id}});
  };

  // 关键词变化
  const handleKeywordChange = (text: string) => {
    setKeyword(text);
    if (!text) {
      setHasSearched(false);
      setSearchResults([]);
    }
  };

  // 提交搜索
  const handleSubmitSearch = () => {
    if (keyword.trim()) {
      handleSearch(keyword);
    }
  };

  // 清空输入
  const handleClear = () => {
    setKeyword('');
    setSearchResults([]);
    setHasSearched(false);
  };

  // 渲染搜索结果
  const renderSearchResult = ({item}: {item: Ticket}) => (
    <TicketCard
      ticket={item}
      onPress={handleTicketPress}
      style="horizontal"
    />
  );

  // 渲染搜索历史
  const renderHistoryItem = ({item}: {item: string}) => (
    <TouchableOpacity
      style={styles.historyItem}
      onPress={() => handleSearch(item)}
    >
      <Text style={styles.historyIcon}>🕐</Text>
      <Text style={styles.historyText}>{item}</Text>
      <TouchableOpacity
        style={styles.historyDelete}
        onPress={() => deleteHistoryItem(item)}
      >
        <Text style={styles.historyDeleteText}>✕</Text>
      </TouchableOpacity>
    </TouchableOpacity>
  );

  // 渲染空状态
  const renderEmpty = () => (
    <View style={styles.emptyContainer}>
      <Text style={styles.emptyIcon}>🔍</Text>
      <Text style={styles.emptyText}>
        {hasSearched ? '未找到相关票务' : '输入关键词搜索'}
      </Text>
      <Text style={styles.emptySubtext}>
        {hasSearched ? '试试其他关键词' : '可以搜索演出名称、艺人或场馆'}
      </Text>
    </View>
  );

  return (
    <View style={styles.container}>
      {/* 搜索栏 */}
      <View style={styles.searchBar}>
        <View style={styles.searchInputWrapper}>
          <Text style={styles.searchIcon}>🔍</Text>
          <TextInput
            style={styles.searchInput}
            value={keyword}
            onChangeText={handleKeywordChange}
            placeholder="搜索演出、赛事、展览..."
            placeholderTextColor={Colors.gray[400]}
            returnKeyType="search"
            onSubmitEditing={handleSubmitSearch}
            autoFocus
          />
          {keyword.length > 0 && (
            <TouchableOpacity onPress={handleClear} style={styles.clearButton}>
              <Text style={styles.clearButtonText}>✕</Text>
            </TouchableOpacity>
          )}
        </View>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.cancelText}>取消</Text>
        </TouchableOpacity>
      </View>

      {/* 内容区域 */}
      {!hasSearched ? (
        // 搜索历史
        <View style={styles.historySection}>
          <View style={styles.historyHeader}>
            <Text style={styles.historyTitle}>最近搜索</Text>
            {searchHistory.length > 0 && (
              <TouchableOpacity onPress={clearSearchHistory}>
                <Text style={styles.clearHistoryText}>清除</Text>
              </TouchableOpacity>
            )}
          </View>
          
          {searchHistory.length > 0 ? (
            <FlatList
              data={searchHistory}
              keyExtractor={(item, index) => `${item}-${index}`}
              renderItem={renderHistoryItem}
              showsVerticalScrollIndicator={false}
            />
          ) : (
            <View style={styles.noHistoryContainer}>
              <Text style={styles.noHistoryText}>暂无搜索历史</Text>
            </View>
          )}
          
          {/* 推荐搜索 */}
          <View style={styles.recommendSection}>
            <Text style={styles.recommendTitle}>推荐搜索</Text>
            <View style={styles.recommendTags}>
              {['周杰伦演唱会', '开心麻花', 'teamLab', 'CBA', '儿童剧'].map(tag => (
                <TouchableOpacity
                  key={tag}
                  style={styles.recommendTag}
                  onPress={() => handleSearch(tag)}
                >
                  <Text style={styles.recommendTagText}>{tag}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </View>
      ) : (
        // 搜索结果
        <FlatList
          data={searchResults}
          keyExtractor={item => item.id}
          renderItem={renderSearchResult}
          contentContainerStyle={styles.resultList}
          ListEmptyComponent={renderEmpty}
          showsVerticalScrollIndicator={false}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  // 搜索栏
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[200],
  },
  searchInputWrapper: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.gray[100],
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    marginRight: Spacing.sm,
  },
  searchIcon: {
    fontSize: 16,
    marginRight: Spacing.xs,
  },
  searchInput: {
    flex: 1,
    height: 36,
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  clearButton: {
    padding: Spacing.xs,
  },
  clearButtonText: {
    fontSize: 16,
    color: Colors.gray[400],
  },
  cancelText: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
  },
  // 搜索历史
  historySection: {
    flex: 1,
    padding: Spacing.md,
  },
  historyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.sm,
  },
  historyTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  clearHistoryText: {
    fontSize: Typography.body.fontSize,
    color: Colors.primary[500],
  },
  historyItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
  },
  historyIcon: {
    fontSize: 14,
    marginRight: Spacing.sm,
  },
  historyText: {
    flex: 1,
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  historyDelete: {
    padding: Spacing.xs,
  },
  historyDeleteText: {
    fontSize: 14,
    color: Colors.gray[400],
  },
  noHistoryContainer: {
    alignItems: 'center',
    paddingVertical: Spacing.xl,
  },
  noHistoryText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[400],
  },
  // 推荐搜索
  recommendSection: {
    marginTop: Spacing.lg,
  },
  recommendTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
    marginBottom: Spacing.sm,
  },
  recommendTags: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  recommendTag: {
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xs,
    borderRadius: BorderRadius.lg,
    marginRight: Spacing.sm,
    marginBottom: Spacing.sm,
    ...Shadows.sm,
  },
  recommendTagText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  // 搜索结果
  resultList: {
    padding: Spacing.md,
    flexGrow: 1,
  },
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

export default SearchScreen;