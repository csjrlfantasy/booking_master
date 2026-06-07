export type CustomerOrderState = {
  orderStatus?: number;
  payStatus?: number;
  orderStatusText?: string;
};

export function customerOrderStatusText(order: CustomerOrderState) {
  if (order.orderStatusText) return order.orderStatusText;
  if (order.orderStatus === 1 && order.payStatus === 0) return '待支付';
  if (order.orderStatus === 6) return '退款中';
  if (order.orderStatus === 2 || order.payStatus === 1) return '已支付';
  if (order.orderStatus === 4) return '已取消';
  if (order.orderStatus === 5) return '已退款';
  return '处理中';
}

export function canPayCustomerOrder(order: CustomerOrderState) {
  return order.orderStatus === 1 && order.payStatus === 0;
}

export function canCancelCustomerOrder(order: CustomerOrderState) {
  return order.orderStatus === 1 && order.payStatus === 0;
}

export function canRefundCustomerOrder(order: CustomerOrderState) {
  return order.orderStatus === 2 && order.payStatus === 1;
}

export function customerOrderPrimaryAction(order: CustomerOrderState) {
  if (canPayCustomerOrder(order)) return '去支付';
  if (canRefundCustomerOrder(order)) return '申请退款';
  return null;
}
