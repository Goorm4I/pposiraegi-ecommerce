import React, { useMemo, useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { API_BASE_URL, USE_MOCK } from '../api/config';
import { eventWebSocket } from '../api/websocket';
import { EVENT_TYPES } from '../mocks/events';
import EventLog from '../components/EventLog';

const isLocalBrowser = typeof window !== 'undefined'
  && ['localhost', '127.0.0.1'].includes(window.location.hostname);
const localOnlyUrl = (envValue, localDefault = '') => envValue || (isLocalBrowser ? localDefault : '');

const grafanaUrl = localOnlyUrl(process.env.REACT_APP_GRAFANA_URL, 'http://localhost:3000');
const prometheusUrl = localOnlyUrl(process.env.REACT_APP_PROMETHEUS_URL, 'http://localhost:9090');
const lokiUrl = localOnlyUrl(process.env.REACT_APP_LOKI_URL);
const tempoUrl = localOnlyUrl(process.env.REACT_APP_TEMPO_URL);

const quickLinks = [
  {
    id: 'grafana',
    name: 'Grafana',
    role: 'Dashboard',
    url: grafanaUrl,
    description: 'Prometheus, Loki, Alert를 한 화면에서 확인합니다.',
    command: 'kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80',
  },
  {
    id: 'prometheus',
    name: 'Prometheus',
    role: 'Metrics',
    url: prometheusUrl,
    description: 'Latency, RPS, Hikari, JVM, scrape target을 직접 쿼리합니다.',
    command: 'kubectl port-forward -n monitoring svc/kube-prometheus-stack-prometheus 9090:9090',
  },
  {
    id: 'loki',
    name: 'Loki',
    role: 'Logs',
    url: lokiUrl,
    description: 'Grafana Explore에서 production 로그와 timeout/error 로그를 확인합니다.',
    command: 'kubectl get pods -n monitoring -l app.kubernetes.io/name=loki',
  },
  {
    id: 'tempo',
    name: 'Tempo',
    role: 'Traces',
    url: tempoUrl,
    description: '추후 OpenTelemetry 도입 후 요청 단위 병목을 추적합니다.',
    command: 'Tempo 미설치 상태면 Prometheus + Loki로 1차 진단',
    disabled: !tempoUrl,
  },
].map(item => ({ ...item, disabled: item.disabled || !item.url }));

const serviceHealth = [
  {
    name: 'api-gateway',
    owner: 'Ingress / Routing',
    metric: 'HTTP latency, 5xx, route saturation',
    risk: 'ALB target health, route timeout',
    query: 'histogram_quantile(0.95, sum by (le, uri, status) (rate(http_server_requests_seconds_bucket{namespace="production", service="api-gateway"}[5m])))',
  },
  {
    name: 'order-service',
    owner: 'Order / Checkout',
    metric: 'Hikari pending, order p95, error rate',
    risk: 'RDS connection budget 초과',
    query: 'hikaricp_connections_pending{namespace="production"}',
  },
  {
    name: 'product-service',
    owner: 'Stock / Product',
    metric: 'Redis gate, stock update, product query',
    risk: '재고 검증 지연 또는 Redis/RDS 경합',
    query: 'rate(http_server_requests_seconds_count{namespace="production", service="product-service"}[5m])',
  },
  {
    name: 'user-service',
    owner: 'Auth / User',
    metric: 'login latency, token failure, DB pool',
    risk: '인증 지연이 gateway 전체 지연으로 전파',
    query: 'rate(http_server_requests_seconds_count{namespace="production", service="user-service"}[5m])',
  },
];

const operatingSignals = [
  {
    label: '주문 지연',
    value: 'p95 / p99',
    tone: 'red',
    description: '사용자가 느끼는 속도. 부하테스트의 1차 판정 기준.',
  },
  {
    label: 'DB 대기',
    value: 'Hikari pending',
    tone: 'amber',
    description: 'Pod를 늘려도 해결되지 않는 RDS 커넥션 병목 신호.',
  },
  {
    label: '수집 상태',
    value: 'Targets UP',
    tone: 'blue',
    description: '앱이 정상이어도 scrape가 비면 판단 근거가 사라짐.',
  },
  {
    label: '로그 사건',
    value: 'timeout / error',
    tone: 'gray',
    description: 'Prometheus가 숫자를 보여주면 Loki가 사건을 설명.',
  },
];

const runbookItems = [
  {
    title: 'Grafana가 안 열릴 때',
    checks: [
      'port-forward가 살아있는지 확인',
      'monitoring namespace의 grafana pod가 Running인지 확인',
      '브라우저 URL이 REACT_APP_GRAFANA_URL과 맞는지 확인',
    ],
  },
  {
    title: 'Prometheus target이 비었을 때',
    checks: [
      'service/deployment annotation의 prometheus.io/port=8081 확인',
      '/actuator/prometheus exposure 확인',
      'Istio AuthorizationPolicy가 Prometheus SA를 허용하는지 확인',
    ],
  },
  {
    title: '주문은 느린데 노드는 여유로울 때',
    checks: [
      'Hikari pending과 active connection 확인',
      'RDS DatabaseConnections / CPU / latency 확인',
      'HPA replica 증설이 DB connection budget을 넘는지 계산',
    ],
  },
  {
    title: '로그가 안 보일 때',
    checks: [
      'promtail daemonset이 Running인지 확인',
      'Loki single binary pod와 gateway 상태 확인',
      'Grafana datasource Loki URL이 http://loki:3100인지 확인',
    ],
  },
];

const promqlSnippets = [
  {
    title: 'HTTP p95 latency',
    description: '서비스/URI별 느린 구간을 먼저 찾습니다.',
    query: 'histogram_quantile(0.95, sum by (le, service, uri, method, status) (rate(http_server_requests_seconds_bucket{namespace="production"}[5m])))',
  },
  {
    title: 'Hikari pending',
    description: 'DB 커넥션 풀이 고갈되는 순간을 봅니다.',
    query: 'hikaricp_connections_pending{namespace="production"}',
  },
  {
    title: 'Error rate',
    description: '5xx 비율이 지연과 같이 튀는지 확인합니다.',
    query: 'sum by (service, status) (rate(http_server_requests_seconds_count{namespace="production", status=~"5.."}[5m]))',
  },
  {
    title: 'Scrape target health',
    description: '관측 파이프라인이 살아있는지 먼저 봅니다.',
    query: 'up{namespace="production"}',
  },
];

const logqlSnippets = [
  {
    title: 'Order timeout',
    query: '{namespace="production", app="order-service"} |= "timeout"',
  },
  {
    title: 'Hikari connection failure',
    query: '{namespace="production"} |= "HikariPool" |= "Connection is not available"',
  },
  {
    title: 'Exception timeline',
    query: '{namespace="production"} |~ "ERROR|Exception|Caused by"',
  },
];

const toneStyle = {
  red: 'bg-red-50 text-red-700 border-red-100',
  amber: 'bg-amber-50 text-amber-700 border-amber-100',
  blue: 'bg-blue-50 text-blue-700 border-blue-100',
  gray: 'bg-gray-100 text-gray-700 border-gray-200',
  green: 'bg-green-50 text-green-700 border-green-100',
};

const EventMonitor = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('overview');
  const [events, setEvents] = useState([]);
  const [stats, setStats] = useState({ total: 0, success: 0, failed: 0 });
  const [filter, setFilter] = useState('ALL');
  const [isConnected, setIsConnected] = useState(false);
  const [resourceSummary, setResourceSummary] = useState(null);
  const [resourceError, setResourceError] = useState('');
  const [copied, setCopied] = useState('');

  useEffect(() => {
    eventWebSocket.connect();
    setIsConnected(!USE_MOCK);

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

  useEffect(() => {
    let cancelled = false;

    const loadSummary = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/v1/monitoring/summary`);
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        const body = await response.json();
        if (!cancelled) {
          setResourceSummary(body.data);
          setResourceError(body.data?.error || '');
        }
      } catch (error) {
        if (!cancelled) {
          setResourceError(error.message || '요약 지표를 불러오지 못했습니다.');
        }
      }
    };

    loadSummary();
    const timer = setInterval(loadSummary, 10000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  const filteredEvents = useMemo(() => (
    filter === 'ALL' ? events : events.filter(e => e.eventType === filter)
  ), [events, filter]);

  const handleCopy = async (text, label) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(label);
      setTimeout(() => setCopied(''), 1600);
    } catch {
      setCopied('복사 실패');
      setTimeout(() => setCopied(''), 1600);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 py-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="flex items-center gap-2 text-xs font-bold text-slate-500 mb-1">
                <Link to="/admin" className="hover:text-slate-800">관리자 콘솔</Link>
                <span>/</span>
                <span>운영 모니터링</span>
              </div>
              <h1 className="text-2xl font-black tracking-tight text-slate-950">운영 모니터링 허브</h1>
              <p className="text-sm text-slate-500 mt-1">
                Prometheus, Grafana, Loki, 이벤트 로그를 한 화면에서 연결합니다. 네트워크가 달라도 화면은 안전하게 동작하도록 링크와 쿼리 중심으로 구성했습니다.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <StateBadge label={USE_MOCK ? 'Mock mode' : isConnected ? 'WebSocket ready' : 'WebSocket check'} tone={USE_MOCK ? 'amber' : isConnected ? 'green' : 'red'} />
              {copied && <StateBadge label={copied} tone="blue" />}
              <button
                onClick={() => navigate('/')}
                className="px-4 py-2 rounded-lg border border-slate-200 bg-white text-sm font-bold text-slate-600 hover:bg-slate-50"
              >
                목록으로
              </button>
            </div>
          </div>
        </div>
      </header>

      <nav className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex gap-1 overflow-x-auto">
            {[
              { id: 'overview', label: '개요' },
              { id: 'metrics', label: '지표/쿼리' },
              { id: 'logs', label: '로그' },
              { id: 'runbook', label: '런북' },
              { id: 'events', label: '이벤트' },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-5 py-3 text-sm font-black border-b-2 whitespace-nowrap ${
                  activeTab === tab.id
                    ? 'border-slate-950 text-slate-950'
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 py-6">
        {activeTab === 'overview' && (
          <OverviewTab
            onCopy={handleCopy}
            resourceSummary={resourceSummary}
            resourceError={resourceError}
          />
        )}
        {activeTab === 'metrics' && <MetricsTab onCopy={handleCopy} />}
        {activeTab === 'logs' && <LogsTab onCopy={handleCopy} />}
        {activeTab === 'runbook' && <RunbookTab onCopy={handleCopy} />}
        {activeTab === 'events' && (
          <EventsTab
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

const OverviewTab = ({ onCopy, resourceSummary, resourceError }) => (
  <div className="space-y-6">
    <section className="grid lg:grid-cols-[1.35fr_.65fr] gap-4">
      <div className="bg-slate-950 text-white rounded-xl p-6">
        <div className="text-sm font-bold text-slate-300 mb-2">현재 운영 판단 기준</div>
        <h2 className="text-3xl font-black leading-tight">EKS가 모든 병목을 해결하는 것이 아니라, 확장해도 되는 선을 지표로 관리합니다.</h2>
        <p className="text-slate-300 text-sm leading-6 mt-4 max-w-3xl">
          주문 지연이 발생하면 먼저 HTTP latency와 Hikari pending을 보고, DB 커넥션 병목인지 Pod/Node 확장 문제인지 분리합니다.
          Grafana는 대시보드, Prometheus는 숫자, Loki는 사건 로그를 맡습니다.
        </p>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-5">
        <div className="text-sm font-black text-slate-500 mb-3">연결 전제</div>
        <div className="space-y-3">
          <CheckLine text="bootstrap-platform.sh로 monitoring stack 설치" />
          <CheckLine text="배포 환경은 REACT_APP_GRAFANA_URL / REACT_APP_PROMETHEUS_URL 값이 있어야 버튼이 활성화됨" />
          <CheckLine text="앱 서비스는 8081 /actuator/prometheus scrape" />
          <CheckLine text="Loki는 Grafana Explore에서 확인" />
        </div>
      </div>
    </section>

    <ResourceSummary summary={resourceSummary} error={resourceError} />

    <section className="grid md:grid-cols-2 xl:grid-cols-4 gap-3">
      {operatingSignals.map(signal => (
        <div key={signal.label} className={`border rounded-xl p-4 ${toneStyle[signal.tone]}`}>
          <div className="text-xs font-black opacity-70">{signal.label}</div>
          <div className="text-2xl font-black mt-1">{signal.value}</div>
          <p className="text-sm leading-5 mt-2 opacity-80">{signal.description}</p>
        </div>
      ))}
    </section>

    <section className="grid lg:grid-cols-4 gap-3">
      {quickLinks.map(link => (
        <ToolLinkCard key={link.id} item={link} onCopy={onCopy} />
      ))}
    </section>

    <section className="bg-white rounded-xl border border-slate-200 overflow-hidden">
      <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
        <div>
          <h2 className="font-black text-slate-900">서비스별 관측 포인트</h2>
          <p className="text-sm text-slate-500 mt-1">처음부터 모든 지표를 보지 말고, 서비스별 병목 후보를 좁혀 봅니다.</p>
        </div>
      </div>
      <div className="grid lg:grid-cols-4 divide-y lg:divide-y-0 lg:divide-x divide-slate-200">
        {serviceHealth.map(service => (
          <ServiceSignal key={service.name} service={service} onCopy={onCopy} />
        ))}
      </div>
    </section>
  </div>
);

const ResourceSummary = ({ summary, error }) => {
  const cluster = summary?.cluster;
  const nodes = summary?.nodes || [];
  const services = summary?.services || [];
  const updatedAt = summary?.timestamp
    ? new Date(summary.timestamp).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '-';

  return (
    <section className="bg-white rounded-xl border border-slate-200 p-5">
      <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between mb-4">
        <div>
          <h2 className="font-black text-slate-900">클러스터 리소스 실시간 요약</h2>
          <p className="text-sm text-slate-500 mt-1">
            Prometheus에서 node-exporter 지표를 읽어 10초마다 갱신합니다.
          </p>
        </div>
        <StateBadge
          label={summary?.available ? `Updated ${updatedAt}` : error ? 'Prometheus check' : 'Loading'}
          tone={summary?.available ? 'green' : error ? 'amber' : 'gray'}
        />
      </div>

      <div className="grid lg:grid-cols-[.9fr_1.1fr] gap-4">
        <div className="grid grid-cols-3 gap-3">
          <MetricTile label="Node" value={cluster?.nodeCount ?? '-'} suffix="대" />
          <MetricTile label="CPU avg" value={cluster ? cluster.cpuPercent : '-'} suffix="%" tone="blue" />
          <MetricTile label="Memory avg" value={cluster ? cluster.memoryPercent : '-'} suffix="%" tone="amber" />
        </div>

        <div className="space-y-2">
          {nodes.length === 0 ? (
            <div className="rounded-lg border border-dashed border-slate-200 p-4 text-sm text-slate-500">
              {error || 'Prometheus 요약 지표를 기다리는 중입니다.'}
            </div>
          ) : nodes.map(node => (
            <div key={node.instance} className="grid md:grid-cols-[160px_1fr_1fr] gap-2 items-center text-sm">
              <div className="font-bold text-slate-600 truncate">{node.instance}</div>
              <UsageBar label="CPU" value={node.cpuPercent} tone="blue" />
              <UsageBar label="MEM" value={node.memoryPercent} tone="amber" />
            </div>
          ))}
        </div>
      </div>

      <div className="mt-5 border-t border-slate-100 pt-4">
        <div className="flex flex-col gap-1 md:flex-row md:items-end md:justify-between mb-3">
          <div>
            <h3 className="font-black text-slate-900">서비스별 CPU / Memory</h3>
            <p className="text-sm text-slate-500">production Pod 지표를 서비스 단위로 묶어 어떤 서비스가 리소스를 쓰는지 봅니다.</p>
          </div>
          <span className="text-xs font-bold text-slate-400">CPU는 mCPU, Memory는 MiB 기준</span>
        </div>
        <ServiceResourceList services={services} error={error} />
      </div>
    </section>
  );
};

const ServiceResourceList = ({ services, error }) => {
  if (!services.length) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 p-4 text-sm text-slate-500">
        {error || '서비스별 리소스 지표를 기다리는 중입니다.'}
      </div>
    );
  }

  const maxCpu = Math.max(...services.map(service => service.cpuMilliCores), 1);
  const maxMemory = Math.max(...services.map(service => service.memoryMiB), 1);

  return (
    <div className="grid lg:grid-cols-2 gap-3">
      {services.map(service => (
        <div key={service.service} className="rounded-lg border border-slate-200 bg-slate-50 p-4">
          <div className="flex items-start justify-between gap-3 mb-3">
            <div>
              <div className="text-sm font-black text-slate-900">{service.service}</div>
              <div className="text-xs font-bold text-slate-400">{service.podCount} pods</div>
            </div>
            <div className="text-right">
              <div className="text-sm font-black text-blue-700">{service.cpuMilliCores.toFixed(1)} mCPU</div>
              <div className="text-xs font-bold text-amber-700">{service.memoryMiB.toFixed(1)} MiB</div>
            </div>
          </div>
          <RelativeBar label="CPU" value={service.cpuMilliCores} max={maxCpu} tone="blue" />
          <div className="mt-2">
            <RelativeBar label="MEM" value={service.memoryMiB} max={maxMemory} tone="amber" />
          </div>
        </div>
      ))}
    </div>
  );
};

const MetricTile = ({ label, value, suffix, tone = 'slate' }) => {
  const toneClass = tone === 'blue'
    ? 'bg-blue-50 text-blue-700 border-blue-100'
    : tone === 'amber'
      ? 'bg-amber-50 text-amber-700 border-amber-100'
      : 'bg-slate-50 text-slate-700 border-slate-200';

  return (
    <div className={`rounded-lg border p-4 ${toneClass}`}>
      <div className="text-xs font-black opacity-70">{label}</div>
      <div className="text-2xl font-black mt-1">
        {value}
        <span className="text-sm ml-1">{value === '-' ? '' : suffix}</span>
      </div>
    </div>
  );
};

const UsageBar = ({ label, value, tone }) => {
  const safeValue = Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : 0;
  const color = tone === 'blue' ? 'bg-blue-500' : 'bg-amber-500';

  return (
    <div>
      <div className="flex justify-between text-xs font-bold text-slate-500 mb-1">
        <span>{label}</span>
        <span>{safeValue.toFixed(1)}%</span>
      </div>
      <div className="h-2 rounded-full bg-slate-100 overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${safeValue}%` }} />
      </div>
    </div>
  );
};

const RelativeBar = ({ label, value, max, tone }) => {
  const percent = max > 0 ? Math.max(0, Math.min(100, (value / max) * 100)) : 0;
  const color = tone === 'blue' ? 'bg-blue-500' : 'bg-amber-500';

  return (
    <div>
      <div className="flex justify-between text-xs font-bold text-slate-500 mb-1">
        <span>{label}</span>
        <span>{percent.toFixed(0)}% of top</span>
      </div>
      <div className="h-2 rounded-full bg-white overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
};

const MetricsTab = ({ onCopy }) => (
  <div className="space-y-6">
    <SectionHeader
      title="Prometheus 쿼리"
      description="속도, 에러율, DB 커넥션 대기처럼 숫자로 증명해야 하는 항목입니다."
    />
    <div className="grid lg:grid-cols-2 gap-4">
      {promqlSnippets.map(item => (
        <QueryCard key={item.title} type="PromQL" item={item} onCopy={onCopy} />
      ))}
    </div>

    <div className="bg-white rounded-xl border border-slate-200 p-5">
      <h3 className="text-lg font-black mb-3">부하테스트 판정 흐름</h3>
      <div className="grid md:grid-cols-5 gap-3">
        {['주문 요청 증가', 'HTTP p95 상승', 'Hikari pending 확인', 'RDS 지표 대조', 'HPA/NodeClaim 확인'].map((step, idx) => (
          <div key={step} className="bg-slate-50 border border-slate-200 rounded-lg p-3">
            <div className="w-7 h-7 rounded-full bg-slate-900 text-white flex items-center justify-center text-xs font-black mb-2">{idx + 1}</div>
            <div className="text-sm font-black text-slate-800">{step}</div>
          </div>
        ))}
      </div>
    </div>
  </div>
);

const LogsTab = ({ onCopy }) => (
  <div className="space-y-6">
    <SectionHeader
      title="Loki 로그 탐색"
      description="Prometheus가 느려진 시간을 알려주면, Loki는 그 시간대에 어떤 사건이 있었는지 확인합니다."
    />
    <div className="grid lg:grid-cols-3 gap-4">
      {logqlSnippets.map(item => (
        <QueryCard key={item.title} type="LogQL" item={item} onCopy={onCopy} />
      ))}
    </div>

    <div className="bg-white rounded-xl border border-slate-200 p-5">
      <h3 className="font-black text-slate-900 mb-2">Tempo 도입 전/후</h3>
      <div className="grid md:grid-cols-3 gap-3">
        <InfoBlock title="Prometheus" body="느려졌다는 사실과 범위를 숫자로 확인합니다." />
        <InfoBlock title="Loki" body="timeout, exception, pool exhaustion 같은 사건 로그를 확인합니다." />
        <InfoBlock title="Tempo" body="요청 하나가 gateway, order, product, DB 중 어디서 느렸는지 추적합니다." />
      </div>
    </div>
  </div>
);

const RunbookTab = ({ onCopy }) => (
  <div className="space-y-6">
    <SectionHeader
      title="연결 문제 방어 런북"
      description="네트워크, 포트, API 불일치가 터질 가능성이 높아서, 화면 안에 바로 확인할 순서를 남깁니다."
    />
    <div className="grid lg:grid-cols-2 gap-4">
      {runbookItems.map(item => (
        <div key={item.title} className="bg-white rounded-xl border border-slate-200 p-5">
          <h3 className="font-black text-slate-900 mb-3">{item.title}</h3>
          <div className="space-y-2">
            {item.checks.map(check => <CheckLine key={check} text={check} />)}
          </div>
        </div>
      ))}
    </div>

    <div className="bg-slate-950 text-white rounded-xl p-5">
      <div className="text-sm font-black text-slate-300 mb-2">권장 실행 순서</div>
      <code className="block text-sm leading-7 whitespace-pre-wrap">
        {'AWS_PROFILE=goorm ./scripts/bootstrap-platform.sh --only monitoring --skip-argocd-sync\nkubectl get pods -n monitoring\nkubectl get svc -n monitoring\nkubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80'}
      </code>
      <button
        onClick={() => onCopy('AWS_PROFILE=goorm ./scripts/bootstrap-platform.sh --only monitoring --skip-argocd-sync\nkubectl get pods -n monitoring\nkubectl get svc -n monitoring\nkubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80', '런북 복사됨')}
        className="mt-4 px-4 py-2 rounded-lg bg-white text-slate-950 text-sm font-black hover:bg-slate-100"
      >
        명령 복사
      </button>
    </div>
  </div>
);

const EventsTab = ({ events, stats, filter, setFilter }) => (
  <div className="space-y-6">
    <SectionHeader
      title="주문 이벤트"
      description="기존 실시간 이벤트 뷰는 운영 콘솔의 하위 탭으로 유지합니다."
    />
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <EventStat label="총 주문" value={stats.total} />
      <EventStat label="성공" value={stats.success} tone="green" />
      <EventStat label="실패" value={stats.failed} tone="red" />
      <EventStat label="성공률" value={stats.total > 0 ? `${Math.round((stats.success / stats.total) * 100)}%` : '-'} tone="blue" />
    </div>

    <div className="bg-white rounded-xl border border-slate-200 p-4">
      <div className="flex gap-2 flex-wrap">
        <FilterButton active={filter === 'ALL'} onClick={() => setFilter('ALL')}>전체</FilterButton>
        {Object.entries(EVENT_TYPES).map(([type, info]) => (
          <FilterButton key={type} active={filter === type} onClick={() => setFilter(type)}>
            {info.label}
          </FilterButton>
        ))}
      </div>
    </div>

    <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
      <div className="px-5 py-4 border-b border-slate-200 flex justify-between items-center">
        <h3 className="font-black text-slate-900">실시간 이벤트 로그</h3>
        <span className="text-sm text-slate-500">{events.length}건</span>
      </div>
      <div className="max-h-[520px] overflow-y-auto">
        {events.length === 0 ? (
          <div className="py-14 text-center text-slate-400">
            <p className="font-bold">이벤트 대기 중</p>
            <p className="text-sm mt-1">주문 이벤트가 들어오면 여기에 표시됩니다.</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {events.map(event => <EventLog key={event.id} event={event} />)}
          </div>
        )}
      </div>
    </div>
  </div>
);

const ToolLinkCard = ({ item, onCopy }) => (
  <div className={`bg-white rounded-xl border border-slate-200 p-4 ${item.disabled ? 'opacity-65' : ''}`}>
    <div className="flex items-start justify-between gap-3">
      <div>
        <div className="text-xs font-black text-slate-500">{item.role}</div>
        <h3 className="text-lg font-black text-slate-900 mt-1">{item.name}</h3>
      </div>
      <StateBadge label={item.disabled ? 'Later' : 'Ready'} tone={item.disabled ? 'gray' : 'green'} />
    </div>
    <p className="text-sm text-slate-500 leading-5 mt-3 min-h-[42px]">{item.description}</p>
    <div className="flex gap-2 mt-4">
      {!item.disabled && item.url ? (
        <a
          href={item.url}
          target="_blank"
          rel="noreferrer"
          className="flex-1 text-center px-3 py-2 rounded-lg bg-slate-900 text-white text-sm font-black hover:bg-slate-700"
        >
          열기
        </a>
      ) : (
        <button disabled className="flex-1 px-3 py-2 rounded-lg bg-slate-100 text-slate-400 text-sm font-black">
          URL 필요
        </button>
      )}
      <button
        onClick={() => onCopy(item.command, `${item.name} 명령 복사됨`)}
        className="px-3 py-2 rounded-lg border border-slate-200 text-sm font-black text-slate-600 hover:bg-slate-50"
      >
        명령
      </button>
    </div>
  </div>
);

const ServiceSignal = ({ service, onCopy }) => (
  <div className="p-4">
    <div className="text-xs font-black text-slate-500">{service.owner}</div>
    <h3 className="text-lg font-black text-slate-900 mt-1">{service.name}</h3>
    <p className="text-sm text-slate-600 leading-5 mt-3">{service.metric}</p>
    <p className="text-xs text-red-600 font-bold mt-2">{service.risk}</p>
    <button
      onClick={() => onCopy(service.query, `${service.name} 쿼리 복사됨`)}
      className="mt-4 px-3 py-2 rounded-lg bg-slate-100 text-slate-700 text-xs font-black hover:bg-slate-200"
    >
      대표 쿼리 복사
    </button>
  </div>
);

const QueryCard = ({ type, item, onCopy }) => (
  <div className="bg-white rounded-xl border border-slate-200 p-5">
    <div className="flex items-start justify-between gap-3">
      <div>
        <div className="text-xs font-black text-slate-500">{type}</div>
        <h3 className="font-black text-slate-900 mt-1">{item.title}</h3>
      </div>
      <button
        onClick={() => onCopy(item.query, `${item.title} 복사됨`)}
        className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-black text-slate-600 hover:bg-slate-50"
      >
        복사
      </button>
    </div>
    {item.description && <p className="text-sm text-slate-500 leading-5 mt-2">{item.description}</p>}
    <pre className="mt-4 bg-slate-950 text-slate-100 rounded-lg p-3 text-xs leading-5 overflow-x-auto"><code>{item.query}</code></pre>
  </div>
);

const SectionHeader = ({ title, description }) => (
  <div>
    <h2 className="text-xl font-black text-slate-950">{title}</h2>
    <p className="text-sm text-slate-500 mt-1">{description}</p>
  </div>
);

const InfoBlock = ({ title, body }) => (
  <div className="bg-slate-50 border border-slate-200 rounded-lg p-4">
    <div className="font-black text-slate-900">{title}</div>
    <p className="text-sm text-slate-500 leading-5 mt-2">{body}</p>
  </div>
);

const CheckLine = ({ text }) => (
  <div className="flex items-start gap-2 text-sm text-slate-600">
    <span className="mt-1.5 h-2 w-2 rounded-full bg-slate-900 flex-shrink-0" />
    <span>{text}</span>
  </div>
);

const StateBadge = ({ label, tone = 'gray' }) => (
  <span className={`inline-flex items-center px-2.5 py-1 rounded-full border text-xs font-black ${toneStyle[tone] || toneStyle.gray}`}>
    {label}
  </span>
);

const EventStat = ({ label, value, tone = 'gray' }) => (
  <div className={`rounded-xl border p-4 ${toneStyle[tone] || toneStyle.gray}`}>
    <p className="text-sm font-bold opacity-75">{label}</p>
    <p className="text-3xl font-black mt-1">{value}</p>
  </div>
);

const FilterButton = ({ active, onClick, children }) => (
  <button
    onClick={onClick}
    className={`px-3 py-1.5 rounded-lg text-sm font-bold transition ${
      active ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
    }`}
  >
    {children}
  </button>
);

export default EventMonitor;
