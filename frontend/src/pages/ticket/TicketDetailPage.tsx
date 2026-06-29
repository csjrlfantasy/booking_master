import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Calendar, MapPin, Clock, Ticket } from 'lucide-react';
import { formatMoney, formatDate, cn } from '@/utils';
import { getAdminTicketDetail } from '@/api/admin';

export function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const ticketId = Number(id);

  const { data: ticket, isLoading, error } = useQuery({
    queryKey: ['ticket', ticketId],
    queryFn: () => getAdminTicketDetail(ticketId),
    enabled: !isNaN(ticketId),
  });

  if (isLoading) {
    return (
      <div className="animate-pulse">
        <div className="h-8 w-32 bg-gray-200 rounded mb-4"></div>
        <div className="h-64 bg-gray-200 rounded-xl"></div>
      </div>
    );
  }

  if (error || !ticket) {
    return (
      <div className="card p-8 text-center">
        <p className="text-gray-500">加载票务信息失败</p>
        <button onClick={() => navigate('/tickets')} className="btn-primary mt-4">
          返回票务列表
        </button>
      </div>
    );
  }

  return (
    <div>
      <button
        onClick={() => navigate('/tickets')}
        className="flex items-center text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft className="w-4 h-4 mr-2" />
        返回票务列表
      </button>

      <div className="card">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-start">
            <img
              src={ticket.coverImage}
              alt={ticket.name}
              className="w-48 h-48 rounded-xl object-cover mr-6"
            />
            <div className="flex-1">
              <h1 className="text-2xl font-bold text-gray-900">{ticket.name}</h1>
              <p className="text-gray-600 mt-1">{ticket.subtitle}</p>
              <span className={cn(
                'inline-block mt-2 px-2 py-1 text-xs font-medium rounded-full',
                ticket.status === 1 ? 'bg-success-100 text-success-700' :
                ticket.status === 0 ? 'bg-gray-100 text-gray-700' :
                'bg-danger-100 text-danger-700'
              )}>
                {ticket.statusText}
              </span>
            </div>
          </div>
        </div>

        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">基本信息</h3>
          <dl className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="flex items-center">
              <MapPin className="w-5 h-5 text-gray-400 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">场馆</dt>
                <dd className="text-gray-900">{ticket.venueName}</dd>
              </div>
            </div>
            <div className="flex items-center">
              <Calendar className="w-5 h-5 text-gray-400 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">演出日期</dt>
                <dd className="text-gray-900">{ticket.showDate}</dd>
              </div>
            </div>
            <div className="flex items-center">
              <Clock className="w-5 h-5 text-gray-400 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">演出时间</dt>
                <dd className="text-gray-900">{ticket.showTime}</dd>
              </div>
            </div>
            <div className="flex items-center">
              <Ticket className="w-5 h-5 text-gray-400 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">价格区间</dt>
                <dd className="text-gray-900">
                  {formatMoney(ticket.priceMin)} - {formatMoney(ticket.priceMax)}
                </dd>
              </div>
            </div>
          </dl>
        </div>

        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">统计信息</h3>
          <dl className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-4 bg-gray-50 rounded-lg">
              <dt className="text-sm text-gray-500">已售数量</dt>
              <dd className="text-2xl font-bold text-gray-900">{ticket.salesCount || 0}</dd>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <dt className="text-sm text-gray-500">收藏数量</dt>
              <dd className="text-2xl font-bold text-gray-900">{ticket.favoriteCount || 0}</dd>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <dt className="text-sm text-gray-500">分类</dt>
              <dd className="text-2xl font-bold text-gray-900">{ticket.categoryName}</dd>
            </div>
          </dl>
        </div>

        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">详情描述</h3>
          <div className="prose max-w-none">
            <p className="text-gray-700 whitespace-pre-wrap">{ticket.description}</p>
          </div>
        </div>

        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">注意事项</h3>
          <div className="prose max-w-none">
            <p className="text-gray-700 whitespace-pre-wrap">{ticket.notice}</p>
          </div>
        </div>

        <div className="p-6 flex gap-4">
          <button
            onClick={() => navigate(`/tickets/${ticketId}/edit`)}
            className="btn-primary"
          >
            编辑票务
          </button>
        </div>
      </div>
    </div>
  );
}
