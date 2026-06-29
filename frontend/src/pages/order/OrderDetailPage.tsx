import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, ShoppingCart, Calendar, User, Phone, Mail } from 'lucide-react';
import { getAdminOrderDetail } from '@/api/admin';
import { ORDER_STATUS_TEXT } from '@/types';
import { formatMoney, formatDate, cn } from '@/utils';

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const orderId = Number(id);

  const { data: order, isLoading, error } = useQuery({
    queryKey: ['adminOrder', orderId],
    queryFn: () => getAdminOrderDetail(orderId),
    enabled: !isNaN(orderId),
  });

  if (isLoading) {
    return (
      <div className="animate-pulse">
        <div className="h-8 w-32 bg-gray-200 rounded mb-4"></div>
        <div className="h-64 bg-gray-200 rounded-xl"></div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="card p-8 text-center">
        <p className="text-gray-500">加载订单信息失败</p>
        <button onClick={() => navigate('/orders')} className="btn-primary mt-4">
          返回订单列表
        </button>
      </div>
    );
  }

  return (
    <div>
      <button
        onClick={() => navigate('/orders')}
        className="flex items-center text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft className="w-4 h-4 mr-2" />
        返回订单列表
      </button>

      {/* 订单信息 */}
      <div className="card">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 font-mono">{order.orderNo}</h1>
              <p className="text-gray-600 mt-1">下单时间: {formatDate(order.createdAt, 'YYYY-MM-DD HH:mm:ss')}</p>
            </div>
            <span className={cn(
              'px-3 py-1 text-sm font-medium rounded-full',
              order.orderStatus === 1 ? 'bg-warning-100 text-warning-700' :
              order.orderStatus === 2 ? 'bg-success-100 text-success-700' :
              order.orderStatus === 3 ? 'bg-primary-100 text-primary-700' :
              order.orderStatus === 4 ? 'bg-gray-100 text-gray-700' :
              order.orderStatus === 5 ? 'bg-danger-100 text-danger-700' :
              'bg-warning-100 text-warning-700'
            )}>
              {ORDER_STATUS_TEXT[order.orderStatus] || '未知'}
            </span>
          </div>
        </div>

        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">票务信息</h3>
          <dl className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <dt className="text-sm text-gray-500">票务名称</dt>
              <dd className="text-gray-900 font-medium">{order.ticketName}</dd>
            </div>
            <div>
              <dt className="text-sm text-gray-500">演出场馆</dt>
              <dd className="text-gray-900">{order.venueName}</dd>
            </div>
            <div>
              <dt className="text-sm text-gray-500">演出日期</dt>
              <dd className="text-gray-900">{order.showDate} {order.showTime}</dd>
            </div>
            <div>
              <dt className="text-sm text-gray-500">数量</dt>
              <dd className="text-gray-900">{order.totalQuantity}张</dd>
            </div>
          </dl>
        </div>

        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">费用信息</h3>
          <dl className="space-y-2">
            <div className="flex justify-between">
              <dt className="text-gray-500">订单金额</dt>
              <dd className="text-gray-900">{formatMoney(order.totalAmount)}</dd>
            </div>
            {order.discountAmount && order.discountAmount > 0 && (
              <div className="flex justify-between">
                <dt className="text-gray-500">优惠金额</dt>
                <dd className="text-success-600">-{formatMoney(order.discountAmount)}</dd>
              </div>
            )}
            {order.couponAmount && order.couponAmount > 0 && (
              <div className="flex justify-between">
                <dt className="text-gray-500">优惠券</dt>
                <dd className="text-success-600">-{formatMoney(order.couponAmount)}</dd>
              </div>
            )}
            <div className="flex justify-between pt-2 border-t border-gray-200">
              <dt className="font-medium">实付金额</dt>
              <dd className="text-xl font-bold text-primary-600">{formatMoney(order.payAmount)}</dd>
            </div>
          </dl>
        </div>

        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">联系人信息</h3>
          <dl className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <dt className="text-sm text-gray-500">姓名</dt>
              <dd className="text-gray-900">{order.contactName || '-'}</dd>
            </div>
            <div>
              <dt className="text-sm text-gray-500">手机号</dt>
              <dd className="text-gray-900">{order.contactPhone || '-'}</dd>
            </div>
            <div>
              <dt className="text-sm text-gray-500">身份证</dt>
              <dd className="text-gray-900">{order.contactIdCard || '-'}</dd>
            </div>
            <div>
              <dt className="text-sm text-gray-500">支付渠道</dt>
              <dd className="text-gray-900 uppercase">{order.payChannel || '-'}</dd>
            </div>
          </dl>
        </div>

        {/* 订单项详情 */}
        {order.items && order.items.length > 0 && (
          <div className="p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">订单项</h3>
            <table className="table">
              <thead>
                <tr>
                  <th>票价等级</th>
                  <th>单价</th>
                  <th>数量</th>
                  <th>小计</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.priceLevelName}</td>
                    <td>{formatMoney(item.unitPrice)}</td>
                    <td>{item.quantity}</td>
                    <td>{formatMoney(item.unitPrice * item.quantity)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}