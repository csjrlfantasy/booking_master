export type AdminOrderActionItem = {
  id: number;
  orderNo?: string;
  orderStatus: number;
};

export type BatchOrderAction = 'cancel' | 'refund';

export function getBatchCancelableOrderIds(orders: AdminOrderActionItem[]) {
  return orders.filter((order) => order.orderStatus === 1).map((order) => order.id);
}

export function getBatchRefundableOrderIds(orders: AdminOrderActionItem[]) {
  return orders.filter((order) => order.orderStatus === 2 || order.orderStatus === 6).map((order) => order.id);
}

export function getBatchActionConfirmText(action: BatchOrderAction, count: number) {
  if (action === 'cancel') {
    return `确认批量取消 ${count} 个订单吗？`;
  }
  return `确认批量全额退款 ${count} 个订单吗？`;
}
