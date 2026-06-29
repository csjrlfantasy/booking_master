/**
 * 购物车项组件
 * 展示单个购物车商品信息
 */
import React, {useCallback} from 'react';
import {View, Text, Image, StyleSheet, TouchableOpacity} from 'react-native';
import {Colors, Typography, Spacing, BorderRadius} from '../../constants';
import type {CartItem as CartItemType} from '../../types';

interface CartItemProps {
  item: CartItemType;
  selected: boolean;
  onSelect: (id: string) => void;
  onQuantityChange: (id: string, quantity: number) => void;
  onRemove: (id: string) => void;
}

const CartItemComponent: React.FC<CartItemProps> = ({
  item,
  selected,
  onSelect,
  onQuantityChange,
  onRemove,
}) => {
  // 增加数量
  const handleIncrement = useCallback(() => {
    if (item.quantity < item.maxQuantity) {
      onQuantityChange(item.id, item.quantity + 1);
    }
  }, [item.id, item.quantity, item.maxQuantity, onQuantityChange]);

  // 减少数量
  const handleDecrement = useCallback(() => {
    if (item.quantity > 1) {
      onQuantityChange(item.id, item.quantity - 1);
    }
  }, [item.id, item.quantity, onQuantityChange]);

  // 计算小计
  const subtotal = item.price * item.quantity;

  return (
    <View style={styles.container}>
      {/* 选择框 */}
      <TouchableOpacity
        style={styles.checkboxContainer}
        onPress={() => onSelect(item.id)}
        activeOpacity={0.7}
      >
        <View style={[styles.checkbox, selected && styles.checkboxSelected]}>
          {selected && <Text style={styles.checkmark}>✓</Text>}
        </View>
      </TouchableOpacity>

      {/* 商品图片 */}
      <Image
        source={{uri: item.posterUrl || 'https://via.placeholder.com/80'}}
        style={styles.poster}
        resizeMode="cover"
      />

      {/* 商品信息 */}
      <View style={styles.info}>
        <Text style={styles.name} numberOfLines={2}>
          {item.ticketName}
        </Text>
        <Text style={styles.venue} numberOfLines={1}>
          📍 {item.venueName}
        </Text>
        <Text style={styles.time} numberOfLines={1}>
          📅 {item.showTime}
        </Text>
        <View style={styles.priceRow}>
          <Text style={styles.priceLevel}>{item.priceLevel}</Text>
          <Text style={styles.price}>¥{item.price.toFixed(2)}</Text>
        </View>

        {/* 座位信息 */}
        {item.seatCode && (
          <Text style={styles.seat}>座位: {item.seatCode}</Text>
        )}

        {/* 数量操作 */}
        <View style={styles.quantityContainer}>
          <TouchableOpacity
            style={[styles.quantityBtn, item.quantity <= 1 && styles.quantityBtnDisabled]}
            onPress={handleDecrement}
            disabled={item.quantity <= 1}
          >
            <Text style={styles.quantityBtnText}>−</Text>
          </TouchableOpacity>
          <Text style={styles.quantity}>{item.quantity}</Text>
          <TouchableOpacity
            style={[styles.quantityBtn, item.quantity >= item.maxQuantity && styles.quantityBtnDisabled]}
            onPress={handleIncrement}
            disabled={item.quantity >= item.maxQuantity}
          >
            <Text style={styles.quantityBtnText}>+</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.removeBtn}
            onPress={() => onRemove(item.id)}
          >
            <Text style={styles.removeText}>删除</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    marginHorizontal: Spacing.base,
    marginVertical: Spacing.sm,
  },
  checkboxContainer: {
    justifyContent: 'center',
    marginRight: Spacing.md,
  },
  checkbox: {
    width: 22,
    height: 22,
    borderRadius: BorderRadius.sm,
    borderWidth: 2,
    borderColor: Colors.gray[300],
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxSelected: {
    backgroundColor: Colors.primary[500],
    borderColor: Colors.primary[500],
  },
  checkmark: {
    color: Colors.white,
    fontSize: 14,
    fontWeight: 'bold',
  },
  poster: {
    width: 80,
    height: 80,
    borderRadius: BorderRadius.md,
    backgroundColor: Colors.gray[200],
  },
  info: {
    flex: 1,
    marginLeft: Spacing.md,
  },
  name: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    lineHeight: Typography.h4.lineHeight,
  },
  venue: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.xs,
  },
  time: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  priceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: Spacing.sm,
  },
  priceLevel: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[500],
    backgroundColor: Colors.primary[50],
    paddingHorizontal: Spacing.sm,
    paddingVertical: 2,
    borderRadius: BorderRadius.sm,
  },
  price: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  seat: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[700],
    marginTop: Spacing.xs,
  },
  quantityContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: Spacing.sm,
  },
  quantityBtn: {
    width: 28,
    height: 28,
    borderRadius: BorderRadius.sm,
    backgroundColor: Colors.gray[100],
    alignItems: 'center',
    justifyContent: 'center',
  },
  quantityBtnDisabled: {
    opacity: 0.5,
  },
  quantityBtnText: {
    fontSize: 18,
    color: Colors.gray[700],
    fontWeight: '500',
  },
  quantity: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginHorizontal: Spacing.base,
    minWidth: 20,
    textAlign: 'center',
  },
  removeBtn: {
    marginLeft: 'auto',
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xs,
  },
  removeText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.error[500],
  },
});

export default CartItemComponent;
