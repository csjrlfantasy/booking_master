/**
 * CategoryScreen - 分类浏览页面
 * 显示所有分类，点击跳转到票务列表
 */
import React, {useState, useEffect} from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  Image,
} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {TicketType} from '../../types';
import type {RootStackParamList} from '../../navigation/types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import ticketMock from '../../services/mock/ticketMock';

// 导航类型
type CategoryNavigationProp = NativeStackNavigationProp<RootStackParamList>;

// 分类数据结构
interface CategoryData {
  id: string;
  name: string;
  icon: string;
  type: TicketType;
  description: string;
  bgColor: string;
  posterUrl: string;
}

// 分类数据
const categoryData: CategoryData[] = [
  {
    id: 'concert',
    name: '演唱会',
    icon: '🎤',
    type: 'concert',
    description: '歌手、音乐会',
    bgColor: '#FF6B6B',
    posterUrl: 'https://picsum.photos/300/200?random=10',
  },
  {
    id: 'drama',
    name: '话剧歌剧',
    icon: '🎭',
    type: 'drama',
    description: '戏剧、舞台剧',
    bgColor: '#4ECDC4',
    posterUrl: 'https://picsum.photos/300/200?random=11',
  },
  {
    id: 'exhibition',
    name: '展览',
    icon: '🎨',
    type: 'exhibition',
    description: '艺术展、设计展',
    bgColor: '#45B7D1',
    posterUrl: 'https://picsum.photos/300/200?random=12',
  },
  {
    id: 'sports',
    name: '体育',
    icon: '⚽',
    type: 'sports',
    description: '足球、篮球赛事',
    bgColor: '#96CEB4',
    posterUrl: 'https://picsum.photos/300/200?random=13',
  },
  {
    id: 'activity',
    name: '休闲',
    icon: '🎪',
    type: 'activity',
    description: '亲子、休闲活动',
    bgColor: '#FFEAA7',
    posterUrl: 'https://picsum.photos/300/200?random=14',
  },
];

const CategoryScreen: React.FC = () => {
  const navigation = useNavigation<CategoryNavigationProp>();
  const [categories, setCategories] = useState<CategoryData[]>([]);

  // 加载分类数据
  useEffect(() => {
    // 从Mock获取分类数据
    const mockCategories = ticketMock.getCategories();
    
    // 合并Mock数据和展示数据
    const mergedCategories = categoryData.map(cat => {
      const mockCat = mockCategories.find(m => m.id === cat.id);
      return {
        ...cat,
        name: mockCat?.name || cat.name,
      };
    });
    
    setCategories(mergedCategories);
  }, []);

  // 跳转到票务列表
  const handleCategoryPress = (category: CategoryData) => {
    navigation.navigate('Search', {screen: 'Search', params: {category: category.type}});
  };

  // 渲染分类项
  const renderCategoryItem = ({item}: {item: CategoryData}) => (
    <TouchableOpacity
      style={styles.categoryItem}
      onPress={() => handleCategoryPress(item)}
      activeOpacity={0.9}
    >
      <Image
        source={{uri: item.posterUrl}}
        style={styles.categoryImage}
        resizeMode="cover"
      />
      <View style={[styles.categoryOverlay, {backgroundColor: item.bgColor + 'E6'}]}>
        <Text style={styles.categoryIcon}>{item.icon}</Text>
        <Text style={styles.categoryName}>{item.name}</Text>
        <Text style={styles.categoryDescription}>{item.description}</Text>
      </View>
    </TouchableOpacity>
  );

  // 渲染头部
  const renderHeader = () => (
    <View style={styles.header}>
      <Text style={styles.headerTitle}>全部分类</Text>
      <Text style={styles.headerSubtitle}>
        发现 {categories.length} 个精彩分类
      </Text>
    </View>
  );

  return (
    <View style={styles.container}>
      <FlatList
        data={categories}
        keyExtractor={item => item.id}
        renderItem={renderCategoryItem}
        ListHeaderComponent={renderHeader}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
        numColumns={2}
        columnWrapperStyle={styles.columnWrapper}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  list: {
    padding: Spacing.md,
  },
  header: {
    marginBottom: Spacing.md,
  },
  headerTitle: {
    fontSize: Typography.h2.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  headerSubtitle: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.xs,
  },
  columnWrapper: {
    justifyContent: 'space-between',
  },
  categoryItem: {
    width: '48%',
    height: 160,
    borderRadius: BorderRadius.md,
    overflow: 'hidden',
    marginBottom: Spacing.md,
    ...Shadows.sm,
  },
  categoryImage: {
    width: '100%',
    height: '100%',
    backgroundColor: Colors.gray[200],
  },
  categoryOverlay: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: Spacing.sm,
  },
  categoryIcon: {
    fontSize: 24,
  },
  categoryName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.white,
    marginTop: Spacing.xs,
  },
  categoryDescription: {
    fontSize: Typography.caption.fontSize,
    color: 'rgba(255,255,255,0.8)',
    marginTop: 2,
  },
});

export default CategoryScreen;
