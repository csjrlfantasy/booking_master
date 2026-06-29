/**
 * CartScreen - 购物车页面
 * 功能：展示购物车商品列表、修改数量、删除商品、显示优惠信息、结算跳转
 */
import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  Image,
  TextInput,
  RefreshControl,
  Alert,
} from 'react-native';
import {useNavigation, useFocusEffect} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RootStackParamList, CartStackParamList} from '../../navigation/types';
import type {CartItem} from '../../types';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {cartApi} from '../../services/api';
import {Button} from '../../components';
import {useAppDispatch, useAppSelector} from '../../store/hooks';
import {
  setCartItems,
  removeFromCart,
  updateQuantity,
  toggleSelect,
  selectAll,
  clearSelection,
} from '../../store/slices/cartSlice';
import Loading from '../../components/common/Loading';
import {toast} from '../../components/common/Toast';

type CartScreenNavigationProp = NativeStackNavigationProp<CartStackParamList>;

// Mock数据
const mockCartItems: CartItem[] = [
  {
    id: 'cart-1',
    ticketId: 'ticket-1',
    ticketName: '周杰伦2026世界巡回演唱会',
    posterUrl: 'https://picsum.photos/200/300',
    venueName: '北京鸟巢体育馆',
    showTime: '2026-05-20 19:30:00',
    price: 580,
    priceLevel: '内场A区',
    priceLevelId: 'level-1',
    quantity: 2,
    maxQuantity: 4,
  },
  {
    id: 'cart-2',
    ticketId: 'ticket-2',
    ticketName: 'teamLab沉浸式艺术展',
    posterUrl: 'https://picsum.photos/201/301',
    venueName: '上海当代艺术中心',
    showTime: '2026-04-15 10:00:00',
    price: 198,
    priceLevel: '成人票',
    priceLevelId: 'level-2',
    quantity: 1,
    maxQuantity: 10,
  },
  {
    id: 'cart-3',
    ticketId: 'ticket-3',
    ticketName: 'CBA季后赛半决赛',
    posterUrl: 'https://picsum.photos/202/302',
    venueName: '广州国际体育中心',
    showTime: '2026-04-28 20:00:00',
    price: 380,
    priceLevel: 'VIP区',
    priceLevelId: 'level-3',
    quantity: 1,
    maxQuantity: 2,
  },
];

const CartScreen: React.FC = () => {
  const navigation = useNavigation<CartScreenNavigationProp>();
  const dispatch = useAppDispatch();
  
  const {items, selectedIds, totalAmount, totalCount} = useAppSelector(
    state => state.cart
  );
  
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  // 加载购物车数据 - 使用真实API
  const loadCartData = useCallback(async () => {
    try {
      // 使用真实API获取购物车数据
      const res = await cartApi.getCartList();
      dispatch(setCartItems(res.items || []));
      
      // 旧代码保留用于API不可用时的降级处理
      // dispatch(setCartItems(mockCartItems));
    } catch (error) {
      console.error('加载购物车失败:', error);
      // API失败时降级使用Mock数据
      dispatch(setCartItems(mockCartItems));
    } finally {
      setLoading(false);
    }
  }, [dispatch]);

  // 首次加载
  useEffect(() => {
    loadCartData();
  }, [loadCartData]);

  // 页面聚焦时刷新
  useFocusEffect(
    useCallback(() => {
      // 可选：每次进入购物车时刷新
    }, [])
  );

  // 下拉刷新
  const handleRefresh = async () => {
    setRefreshing(true);
    await loadCartData();
    setRefreshing(false);
  };

  // 处理选中/取消选中
  const handleToggleSelect = (id: string) => {
    dispatch(toggleSelect(id));
  };

  // 全选/取消全选
  const handleSelectAll = () => {
    if (selectedIds.length === items.length) {
      dispatch(clearSelection());
    } else {
      dispatch(selectAll());
    }
  };

  // 修改数量
  const handleQuantityChange = (item: CartItem, delta: number) => {
    const newQuantity = item.quantity + delta;
    if (newQuantity < 1) {
      Alert.alert('提示', '数量不能小于1', [{text: '确定'}]);
      return;
    }
    if (newQuantity > item.maxQuantity) {
      Alert.alert('提示', `该票种最多购买${item.maxQuantity}张`, [{text: '确定'}]);
      return;
    }
    dispatch(updateQuantity({id: item.id, quantity: newQuantity}));
  };

  // 删除商品
  const handleDelete = (item: CartItem) => {
    Alert.alert(
      '确认删除',
      `确定要从购物车中删除「${item.ticketName}」吗？`,
      [
        {text: '取消', style: 'cancel'},
        {
          text: '删除',
          style: 'destructive',
          onPress: () => {
            dispatch(removeFromCart(item.id));
            toast.success('已删除');
          },
        },
      ]
    );
  };

  // 批量删除选中项
  const handleBatchDelete = () => {
    if (selectedIds.length === 0) {
      toast.info('请先选择要删除的商品');
      return;
    }
    Alert.alert(
      '确认删除',
      `确定要删除选中的${selectedIds.length}件商品吗？`,
      [
        {text: '取消', style: 'cancel'},
        {
          text: '删除',
          style: 'destructive',
          onPress: () => {
            selectedIds.forEach(id => dispatch(removeFromCart(id)));
            toast.success('已删除');
          },
        },
      ]
    );
  };

  // 结算
  const handleCheckout = () => {
    if (selectedIds.length === 0) {
      toast.info('请先选择要结算的商品');
      return;
    }
    navigation.navigate('Checkout', {cartIds: selectedIds});
  };

  // 跳转商品详情 (需要在CartStack中处理，使用pop后跳转到Ticket)
  const handleItemPress = (item: CartItem) => {
    // 跳转到票务详情页
    navigation.getParent()?.navigate('Ticket' as any, {
      screen: 'TicketDetail',
      params: {ticketId: item.ticketId},
    });
  };

  // 格式化时间
  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
  };

  // 渲染购物车项
  const renderCartItem = ({item}: {item: CartItem}) => {
    const isSelected = selectedIds.includes(item.id);
    
    return (
      <View style={[styles.cartItem, Shadows.sm]}>
        {/* 选择框 */}
        <TouchableOpacity
          style={styles.checkbox}
          onPress={() => handleToggleSelect(item.id)}
        >
          <View style={[styles.checkboxInner, isSelected && styles.checkboxChecked]}>
            {isSelected && <Text style={styles.checkmark}>✓</Text>}
          </View>
        </TouchableOpacity>

        {/* 商品图片 */}
        <TouchableOpacity onPress={() => handleItemPress(item)}>
          <Image source={{uri: item.posterUrl}} style={styles.poster} />
        </TouchableOpacity>

        {/* 商品信息 */}
        <View style={styles.itemInfo}>
          <TouchableOpacity onPress={() => handleItemPress(item)}>
            <Text style={styles.itemName} numberOfLines={2}>
              {item.ticketName}
            </Text>
          </TouchableOpacity>
          
          <Text style={styles.itemVenue}>📍 {item.venueName}</Text>
          <Text style={styles.itemTime}>🕐 {formatTime(item.showTime)}</Text>
          
          <View style={styles.itemBottom}>
            <Text style={styles.itemPrice}>¥{item.price}</Text>
            <View style={styles.quantityControl}>
              <TouchableOpacity
                style={styles.quantityBtn}
                onPress={() => handleQuantityChange(item, -1)}
              >
                <Text style={styles.quantityBtnText}>-</Text>
              </TouchableOpacity>
              <Text style={styles.quantity}>{item.quantity}</Text>
              <TouchableOpacity
                style={styles.quantityBtn}
                onPress={() => handleQuantityChange(item, 1)}
              >
                <Text style={styles.quantityBtnText}>+</Text>
              </TouchableOpacity>
            </View>
          </View>
          
          <Text style={styles.priceLevel}>{item.priceLevel}</Text>
        </View>

        {/* 删除按钮 */}
        <TouchableOpacity
          style={styles.deleteBtn}
          onPress={() => handleDelete(item)}
        >
          <Text style={styles.deleteBtnText}>×</Text>
        </TouchableOpacity>
      </View>
    );
  };

  // 渲染空状态
  const renderEmpty = () => (
    <View style={styles.emptyContainer}>
      <Text style={styles.emptyIcon}>🛒</Text>
      <Text style={styles.emptyTitle}>购物车是空的</Text>
      <Text style={styles.emptyDesc}>快去挑选心仪的演出吧</Text>
      <Button
        title="去逛逛"
        variant="primary"
        onPress={() => navigation.getParent()?.navigate('Main' as any)}
        style={{marginTop: Spacing.lg}}
      />
    </View>
  );

  if (loading) {
    return <Loading fullscreen text="加载购物车..." />;
  }

  return (
    <View style={styles.container}>
      {/* 顶部导航栏 */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>购物车</Text>
        <TouchableOpacity onPress={handleBatchDelete}>
          <Text style={styles.headerAction}>
            {selectedIds.length > 0 ? '删除' : ''}
          </Text>
        </TouchableOpacity>
      </View>

      {items.length === 0 ? (
        renderEmpty()
      ) : (
        <>
          {/* 购物车列表 */}
          <FlatList
            data={items}
            keyExtractor={item => item.id}
            renderItem={renderCartItem}
            contentContainerStyle={styles.listContent}
            refreshControl={
              <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
            }
            showsVerticalScrollIndicator={false}
          />

          {/* 底部结算栏 */}
          <View style={[styles.footer, Shadows.lg]}>
            {/* 全选 */}
            <TouchableOpacity style={styles.selectAll} onPress={handleSelectAll}>
              <View style={[styles.checkboxInner, selectedIds.length === items.length && items.length > 0 && styles.checkboxChecked]}>
                {selectedIds.length === items.length && items.length > 0 && (
                  <Text style={styles.checkmark}>✓</Text>
                )}
              </View>
              <Text style={styles.selectAllText}>全选</Text>
            </TouchableOpacity>

            {/* 合计 */}
            <View style={styles.totalContainer}>
              <Text style={styles.totalLabel}>合计:</Text>
              <Text style={styles.totalAmount}>¥{totalAmount.toFixed(2)}</Text>
            </View>

            {/* 结算按钮 */}
            <Button
              title={`结算${totalCount > 0 ? `(${totalCount})` : ''}`}
              variant="primary"
              size="large"
              disabled={selectedIds.length === 0}
              onPress={handleCheckout}
              style={styles.checkoutBtn}
            />
          </View>
        </>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[200],
  },
  headerTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  headerAction: {
    fontSize: Typography.body.fontSize,
    color: Colors.error[500],
  },
  listContent: {
    padding: Spacing.md,
  },
  cartItem: {
    flexDirection: 'row',
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    padding: Spacing.md,
    marginBottom: Spacing.md,
  },
  checkbox: {
    justifyContent: 'center',
    marginRight: Spacing.sm,
  },
  checkboxInner: {
    width: 22,
    height: 22,
    borderRadius: BorderRadius.full,
    borderWidth: 2,
    borderColor: Colors.gray[300],
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxChecked: {
    backgroundColor: Colors.primary[500],
    borderColor: Colors.primary[500],
  },
  checkmark: {
    color: Colors.white,
    fontSize: 14,
    fontWeight: '700',
  },
  poster: {
    width: 80,
    height: 100,
    borderRadius: BorderRadius.sm,
    backgroundColor: Colors.gray[200],
  },
  itemInfo: {
    flex: 1,
    marginLeft: Spacing.md,
  },
  itemName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    lineHeight: 20,
  },
  itemVenue: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.xs,
  },
  itemTime: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[500],
    marginTop: 2,
  },
  itemBottom: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: Spacing.sm,
  },
  itemPrice: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  quantityControl: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.gray[100],
    borderRadius: BorderRadius.sm,
  },
  quantityBtn: {
    width: 28,
    height: 28,
    justifyContent: 'center',
    alignItems: 'center',
  },
  quantityBtnText: {
    fontSize: 18,
    color: Colors.gray[700],
    fontWeight: '600',
  },
  quantity: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
    minWidth: 30,
    textAlign: 'center',
  },
  priceLevel: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  deleteBtn: {
    position: 'absolute',
    top: 0,
    right: 0,
    padding: Spacing.xs,
  },
  deleteBtnText: {
    fontSize: 24,
    color: Colors.gray[400],
    fontWeight: '300',
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[200],
  },
  selectAll: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  selectAllText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
    marginLeft: Spacing.xs,
  },
  totalContainer: {
    flex: 1,
    marginLeft: Spacing.md,
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'flex-end',
  },
  totalLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  totalAmount: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
    marginLeft: Spacing.xs,
  },
  checkoutBtn: {
    marginLeft: Spacing.md,
    paddingHorizontal: Spacing.xl,
  },
  // 空状态
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: Spacing['2xl'],
  },
  emptyIcon: {
    fontSize: 64,
    marginBottom: Spacing.md,
  },
  emptyTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.sm,
  },
  emptyDesc: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
  },
});

export default CartScreen;
