import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { USE_MOCK } from '../api/config';
import { eventWebSocket } from '../api/websocket';
import { EVENT_TYPES } from '../mocks/events';
import EventLog from '../components/EventLog';

const EventMonitor = () => {
  const navigate = useNavigate();
  const [events, setEvents] = useState([]);
  const [stats, setStats] = useState({
    total: 0,
    success: 0,
    failed: 0,
  });
  const [filter, setFilter] = useState('ALL');
  const [isConnected, setIsConnected] = useState(false);
  const [activeTab, setActiveTab] = useState('flow');

  useEffect(() => {
    eventWebSocket.connect();
    setIsConnected(true);

    const unsubscribe = eventWebSocket.subscribe((event) => {
      setEvents(prev => [event, ...prev].slice(0, 100));

      if (event.eventType === 'ORDER_COMPLETED') {
        setStats(prev => ({ ...prev, total: prev.total + 1, success: prev.success + 1 }));
      } else if (event.eventType === 'ORDER_CANCELLED') {
        setStats(prev => ({ ...prev, total: prev.total + 1, failed: prev.failed + 1 }));
      }
    });

    return () => {
      unsubscribe();
      eventWebSocket.disconnect();
    };
  }, []);

  const filteredEvents = filter === 'ALL' 
    ? events 
    : events.filter(e => e.eventType === filter);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b">
        <div className="container mx-auto px-4 py-4">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold text-gray-800">
                📊 주문 처리 모니터링
              </h1>
              <p className="text-gray-500 text-sm mt-1">
                Choreography Saga 패턴 기반 실시간 이벤트 스트리밍
              </p>
            </div>
            <div className="flex items-center gap-4">
              <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm ${
                USE_MOCK 
                  ? 'bg-yellow-100 text-yellow-700' 
                  : isConnected
                    ? 'bg-green-100 text-green-700'
                    : 'bg-red-100 text-red-700'
              }`}>
                <span className={`w-2 h-2 rounded-full ${
                  USE_MOCK 
                    ? 'bg-yellow-500' 
                    : isConnected 
                      ? 'bg-green-500 animate-pulse' 
                      : 'bg-red-500'
                }`}></span>
                {USE_MOCK ? 'Mock 모드' : isConnected ? 'WebSocket 연결됨' : '연결 끊김'}
              </div>
              <button
                onClick={() => navigate('/')}
                className="px-4 py-2 text-gray-600 hover:text-gray-800"
              >
                ← 목록으로
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* 탭 */}
      <div className="bg-white border-b">
        <div className="container mx-auto px-4">
          <div className="flex gap-1">
            {[
              { id: 'flow', label: '🔄 Saga 플로우' },
              { id: 'realtime', label: '⚡ 실시간 이벤트' },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-6 py-4 font-medium transition border-b-2 ${
                  activeTab === tab.id
                    ? 'border-primary-500 text-primary-600 bg-primary-50'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <main className="container mx-auto px-4 py-8">
        {activeTab === 'flow' && <SagaFlowTab />}
        {activeTab === 'realtime' && (
          <RealtimeTab 
            events={filteredEvents}
            stats={stats}
            filter={filter}
            setFilter={setFilter}
          />
        )}
      </main>
    </div>
  );
};

/* ─────────────────────────────────────────────────────────────────
   Saga 플로우 탭
───────────────────────────────────────────────────────────────── */
const SagaFlowTab = () => (
  <div className="space-y-8">
    <div className="bg-white rounded-xl p-6 shadow-sm border">
      <h2 className="text-lg font-bold text-gray-800 mb-4">
        🏗️ Choreography Saga 패턴
      </h2>
      <p className="text-gray-600 mb-6">
        각 서비스가 Kafka 이벤트를 발행/구독하여 분산 트랜잭션을 처리합니다.
        중앙 조정자 없이 서비스들이 자율적으로 협력합니다.
      </p>

      <div className="bg-gray-50 rounded-lg p-6 overflow-x-auto">
        <div className="min-w-[700px]">
          {/* 성공 플로우 */}
          <div className="mb-8">
            <div className="text-sm font-semibold text-green-600 mb-3">✅ 성공 플로우</div>
            <div className="flex items-center gap-2">
              <FlowStep service="Order" event="ORDER_CREATED" color="blue" icon="📝" />
              <FlowArrow />
              <FlowStep service="Stock" event="STOCK_RESERVED" color="green" icon="📦" />
              <FlowArrow />
              <FlowStep service="Payment" event="PAYMENT_COMPLETED" color="purple" icon="💳" />
              <FlowArrow />
              <FlowStep service="Order" event="ORDER_COMPLETED" color="green" icon="✅" />
            </div>
          </div>

          {/* 실패 플로우 */}
          <div>
            <div className="text-sm font-semibold text-red-600 mb-3">❌ 실패 플로우 (보상 트랜잭션)</div>
            <div className="flex items-center gap-2">
              <FlowStep service="Order" event="ORDER_CREATED" color="blue" icon="📝" />
              <FlowArrow />
              <FlowStep service="Stock" event="STOCK_RESERVED" color="green" icon="📦" />
              <FlowArrow />
              <FlowStep service="Payment" event="PAYMENT_FAILED" color="red" icon="💳" />
              <FlowArrow type="rollback" />
              <FlowStep service="Stock" event="STOCK_ROLLBACK" color="orange" icon="↩️" />
              <FlowArrow type="rollback" />
              <FlowStep service="Order" event="ORDER_CANCELLED" color="red" icon="❌" />
            </div>
          </div>
        </div>
      </div>
    </div>

    {/* 서비스 카드 */}
    <div className="grid md:grid-cols-3 gap-4">
      <ServiceCard
        name="Order Service"
        icon="📝"
        color="blue"
        responsibilities={["주문 생성/상태 관리", "ORDER_CREATED 발행", "최종 상태 업데이트"]}
      />
      <ServiceCard
        name="Stock Service"
        icon="📦"
        color="green"
        responsibilities={["재고 확인/차감", "동시성 제어 (락)", "보상: 재고 복구"]}
      />
      <ServiceCard
        name="Payment Service"
        icon="💳"
        color="purple"
        responsibilities={["결제 처리", "잔액 확인", "결제 실패 이벤트"]}
      />
    </div>

    {/* 기술 스택 */}
    <div className="bg-white rounded-xl p-6 shadow-sm border">
      <h2 className="text-lg font-bold text-gray-800 mb-4">🛠️ 기술 스택</h2>
      <div className="grid md:grid-cols-2 gap-6">
        <div>
          <h3 className="font-semibold text-gray-700 mb-2">Backend</h3>
          <div className="flex flex-wrap gap-2">
            {['Spring Boot', 'Kafka', 'Redis', 'MySQL'].map(tech => (
              <span key={tech} className="px-3 py-1 bg-gray-100 rounded-full text-sm">{tech}</span>
            ))}
          </div>
        </div>
        <div>
          <h3 className="font-semibold text-gray-700 mb-2">Infra</h3>
          <div className="flex flex-wrap gap-2">
            {['AWS EKS', 'MSK (Kafka)', 'ECR', 'ALB'].map(tech => (
              <span key={tech} className="px-3 py-1 bg-gray-100 rounded-full text-sm">{tech}</span>
            ))}
          </div>
        </div>
      </div>
    </div>
  </div>
);

/* ─────────────────────────────────────────────────────────────────
   실시간 이벤트 탭
───────────────────────────────────────────────────────────────── */
const RealtimeTab = ({ events, stats, filter, setFilter }) => (
  <div className="space-y-6">
    {/* 통계 */}
    <div className="grid grid-cols-4 gap-4">
      <StatCard label="총 주문" value={stats.total} color="gray" />
      <StatCard label="성공" value={stats.success} color="green" />
      <StatCard label="실패" value={stats.failed} color="red" />
      <StatCard 
        label="성공률" 
        value={stats.total > 0 ? `${Math.round((stats.success / stats.total) * 100)}%` : '-'} 
        color="blue" 
      />
    </div>

    {/* 필터 */}
    <div className="flex gap-2 flex-wrap">
      <button
        onClick={() => setFilter('ALL')}
        className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
          filter === 'ALL' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
        }`}
      >
        전체
      </button>
      {Object.entries(EVENT_TYPES).map(([type, info]) => (
        <button
          key={type}
          onClick={() => setFilter(type)}
          className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
            filter === type ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          {info.icon} {info.label}
        </button>
      ))}
    </div>

    {/* 이벤트 로그 */}
    <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
      <div className="px-6 py-4 border-b bg-gray-50 flex justify-between items-center">
        <h3 className="font-semibold text-gray-800">📜 실시간 Kafka 이벤트</h3>
        <span className="text-sm text-gray-500">{events.length}건</span>
      </div>
      
      <div className="max-h-[500px] overflow-y-auto">
        {events.length === 0 ? (
          <div className="py-12 text-center text-gray-400">
            <div className="text-4xl mb-3">📡</div>
            <p>이벤트 대기 중...</p>
            <p className="text-sm mt-1">주문이 발생하면 실시간으로 표시됩니다</p>
          </div>
        ) : (
          <div className="divide-y">
            {events.map(event => (
              <EventLog key={event.id} event={event} />
            ))}
          </div>
        )}
      </div>
    </div>
  </div>
);

/* ─────────────────────────────────────────────────────────────────
   서브 컴포넌트
───────────────────────────────────────────────────────────────── */
const FlowStep = ({ service, event, color, icon }) => {
  const colors = {
    blue: 'bg-blue-100 border-blue-300 text-blue-700',
    green: 'bg-green-100 border-green-300 text-green-700',
    purple: 'bg-purple-100 border-purple-300 text-purple-700',
    red: 'bg-red-100 border-red-300 text-red-700',
    orange: 'bg-orange-100 border-orange-300 text-orange-700',
  };

  return (
    <div className={`flex flex-col items-center px-4 py-3 rounded-lg border-2 ${colors[color]} min-w-[120px]`}>
      <span className="text-2xl mb-1">{icon}</span>
      <span className="text-xs font-semibold">{service}</span>
      <span className="text-[10px] mt-1 opacity-75">{event}</span>
    </div>
  );
};

const FlowArrow = ({ type = 'normal' }) => (
  <div className={`flex-shrink-0 ${type === 'rollback' ? 'text-orange-400' : 'text-gray-400'}`}>
    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
    </svg>
  </div>
);

const ServiceCard = ({ name, icon, color, responsibilities }) => {
  const colors = {
    blue: 'border-blue-200 bg-blue-50',
    green: 'border-green-200 bg-green-50',
    purple: 'border-purple-200 bg-purple-50',
  };

  return (
    <div className={`rounded-xl border-2 p-5 ${colors[color]}`}>
      <div className="flex items-center gap-2 mb-3">
        <span className="text-2xl">{icon}</span>
        <h3 className="font-bold text-gray-800">{name}</h3>
      </div>
      <div className="space-y-2">
        {responsibilities.map((r, i) => (
          <p key={i} className="text-sm text-gray-600 flex items-start gap-2">
            <span className="text-gray-400">•</span>{r}
          </p>
        ))}
      </div>
    </div>
  );
};

const StatCard = ({ label, value, color }) => {
  const colors = {
    gray: 'bg-gray-100 text-gray-800',
    blue: 'bg-blue-100 text-blue-800',
    green: 'bg-green-100 text-green-800',
    red: 'bg-red-100 text-red-800',
  };

  return (
    <div className={`rounded-xl p-4 ${colors[color]}`}>
      <p className="text-sm opacity-75">{label}</p>
      <p className="text-3xl font-bold">{value}</p>
    </div>
  );
};

export default EventMonitor;
