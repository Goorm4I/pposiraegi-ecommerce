import React from 'react';
import { EVENT_TYPES } from '../mocks/events';

const EventLog = ({ event }) => {
  const { eventType, orderId, timestamp, data } = event;
  const typeInfo = EVENT_TYPES[eventType] || { icon: '⚪', label: eventType, color: 'gray' };

  const formatTimestamp = (ts) => {
    const date = new Date(ts);
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      fractionalSecondDigits: 3
    });
  };

  const getColorClass = (color) => {
    const colors = {
      blue: 'bg-blue-100 border-blue-300 text-blue-800',
      green: 'bg-green-100 border-green-300 text-green-800',
      red: 'bg-red-100 border-red-300 text-red-800',
      orange: 'bg-orange-100 border-orange-300 text-orange-800',
      purple: 'bg-purple-100 border-purple-300 text-purple-800',
      gray: 'bg-gray-100 border-gray-300 text-gray-800'
    };
    return colors[color] || colors.gray;
  };

  return (
    <div className={`flex items-center p-3 border rounded-lg mb-2 ${getColorClass(typeInfo.color)}`}>
      <span className="text-2xl mr-3">{typeInfo.icon}</span>
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <span className="font-semibold">{typeInfo.label}</span>
          <span className="text-sm opacity-75">({eventType})</span>
        </div>
        <div className="text-sm opacity-75">
          주문: {orderId}
          {data?.reason && <span className="ml-2">| 사유: {data.reason}</span>}
          {data?.amount && <span className="ml-2">| 금액: {data.amount.toLocaleString()}원</span>}
        </div>
      </div>
      <span className="text-xs opacity-60 font-mono">
        {formatTimestamp(timestamp)}
      </span>
    </div>
  );
};

export default EventLog;
