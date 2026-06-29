/**
 * 票品类型定义
 */
export type TicketType = 'concert' | 'drama' | 'exhibition' | 'sports' | 'activity';

export interface Ticket {
  id: string;
  name: string;
  subtitle?: string;
  type: TicketType;
  posterUrl: string;
  images?: string[];
  venue: Venue;
  showTime: string;
  endTime?: string;
  priceRange: {
    min: number;
    max: number;
  };
  priceLevels: PriceLevel[];
  description?: string;
  notice?: string;
  performers?: Performer[];
  status: TicketStatus;
  isHot?: boolean;
  isDiscount?: boolean;
  discountPrice?: number;
  salesStartTime?: string;
  salesEndTime?: string;
  totalStock?: number;
  remainStock?: number;
  createTime?: string;
}

export interface Venue {
  id: string;
  name: string;
  address: string;
  city?: string;
  latitude?: number;
  longitude?: number;
  seatingChart?: string;
}

export interface PriceLevel {
  id: string;
  name: string;
  price: number;
  stock: number;
  description?: string;
}

export interface Performer {
  id: string;
  name: string;
  avatar?: string;
  role?: string;
  description?: string;
}

export type TicketStatus = 'upcoming' | 'onsale' | 'soldout' | 'cancelled';

export interface TicketListParams {
  type?: TicketType;
  keyword?: string;
  city?: string;
  page?: number;
  pageSize?: number;
  sortBy?: 'time' | 'sales' | 'price';
  sortOrder?: 'asc' | 'desc';
}

export interface FavoriteTicket {
  id: string;
  ticketId: string;
  ticket: Ticket;
  createTime: string;
}
