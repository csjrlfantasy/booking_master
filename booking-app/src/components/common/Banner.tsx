/**
 * Banner 轮播图组件
 */
import React, {useState, useRef} from 'react';
import {
  View,
  Text,
  Image,
  ScrollView,
  StyleSheet,
  Dimensions,
  TouchableOpacity,
} from 'react-native';
import {Colors, BorderRadius, Spacing} from '../../constants';

const {width: SCREEN_WIDTH} = Dimensions.get('window');

export interface BannerItem {
  id: string;
  imageUrl: string;
  title?: string;
  action?: string;
}

interface BannerProps {
  items: BannerItem[];
  height?: number;
  autoPlay?: boolean;
  loop?: boolean;
  onPress?: (item: BannerItem) => void;
}

const Banner: React.FC<BannerProps> = ({
  items,
  height = 180,
  autoPlay = true,
  loop = true,
  onPress,
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const scrollRef = useRef<ScrollView>(null);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  // 自动轮播
  React.useEffect(() => {
    if (autoPlay && items.length > 1) {
      intervalRef.current = setInterval(() => {
        const nextIndex = (currentIndex + 1) % items.length;
        scrollToIndex(nextIndex);
      }, 3000);
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [autoPlay, currentIndex, items.length]);

  const scrollToIndex = (index: number) => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({x: index * SCREEN_WIDTH, animated: true});
      setCurrentIndex(index);
    }
  };

  const handleScroll = (event: any) => {
    const offsetX = event.nativeEvent.contentOffset.x;
    const index = Math.round(offsetX / SCREEN_WIDTH);
    setCurrentIndex(index);
  };

  const handlePress = (item: BannerItem) => {
    onPress?.(item);
  };

  if (items.length === 0) {
    return (
      <View style={[styles.container, {height}]}>
        <Text style={styles.emptyText}>暂无轮播图</Text>
      </View>
    );
  }

  return (
    <View style={styles.wrapper}>
      <ScrollView
        ref={scrollRef}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onScroll={handleScroll}
        scrollEventThrottle={16}
        bounces={loop}
        style={styles.scrollView}
      >
        {items.map((item, index) => (
          <TouchableOpacity
            key={item.id}
            activeOpacity={0.9}
            onPress={() => handlePress(item)}
            style={[styles.slide, {width: SCREEN_WIDTH, height}]}
          >
            {item.imageUrl ? (
              <Image source={{uri: item.imageUrl}} style={styles.image} resizeMode="cover" />
            ) : (
              <View style={styles.imagePlaceholder}>
                <Text style={styles.placeholderText}>{item.title || `Banner ${index + 1}`}</Text>
              </View>
            )}
            {item.title ? (
              <View style={styles.titleOverlay}>
                <Text numberOfLines={1} style={styles.titleText}>{item.title}</Text>
              </View>
            ) : null}
          </TouchableOpacity>
        ))}
      </ScrollView>

      {/* 指示器 */}
      {items.length > 1 && (
        <View style={styles.indicatorContainer}>
          {items.map((_, index) => (
            <View
              key={index}
              style={[
                styles.indicator,
                index === currentIndex && styles.indicatorActive,
              ]}
            />
          ))}
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  wrapper: {
    position: 'relative',
  },
  container: {
    backgroundColor: Colors.gray[200],
    alignItems: 'center',
    justifyContent: 'center',
  },
  scrollView: {
    flexGrow: 0,
  },
  slide: {},
  image: {
    width: '100%',
    height: '100%',
  },
  imagePlaceholder: {
    flex: 1,
    backgroundColor: Colors.primary[500],
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeholderText: {
    color: Colors.white,
    fontSize: 18,
    fontWeight: '600',
  },
  titleOverlay: {
    position: 'absolute',
    left: Spacing.md,
    right: Spacing.md,
    bottom: Spacing.xl,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xs,
    borderRadius: BorderRadius.sm,
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
  },
  titleText: {
    color: Colors.white,
    fontSize: 16,
    fontWeight: '700',
  },
  emptyText: {
    color: Colors.gray[500],
    fontSize: 14,
  },
  indicatorContainer: {
    position: 'absolute',
    bottom: Spacing.sm,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
  indicator: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: 'rgba(255, 255, 255, 0.5)',
    marginHorizontal: 3,
  },
  indicatorActive: {
    backgroundColor: Colors.white,
    width: 18,
  },
});

export default Banner;
export type {BannerProps};
