/**
 * 购物车类型定义
 */
export interface CartItem {
  id: string;
  ticketId: string;
  ticketName: string;
  posterUrl?: string;
  venueName: string;
  showTime: string;
  price: number;
  priceLevel: string;
  priceLevelId: string;
  quantity: number;
  seatCode?: string;
  maxQuantity: number;
}

export interface Cart {
  items: CartItem[];
  selectedIds: string[];
  totalAmount: number;
  totalCount: number;
}
