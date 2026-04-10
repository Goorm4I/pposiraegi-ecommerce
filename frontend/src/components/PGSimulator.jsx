import React, { useState, useRef, useEffect } from 'react';

const MOCK_PG_URL = process.env.REACT_APP_MOCK_PG_URL || 'https://mock-pg-1046420547293.us-central1.run.app';

/* ─────────────────────────────────────────────────────────────────
   Mock PG 결제 시뮬레이터

   결제수단별 분기:
   - 뽀시페이: 잔액 확인 → 핀 입력 → 완료 (내부 지갑)
   - 카카오/토스: 앱 연결 시뮬레이션 → 자동 완료
   - 신용카드: 바로 Mock PG 서버 호출 → 완료/실패

   ⚠️ 학습/포트폴리오 목적의 시뮬레이션입니다.
   실제 결제가 진행되지 않습니다.
───────────────────────────────────────────────────────────────── */

const pgStyles = {
  bboshi: {
    name: '뽀시페이',
    color: 'from-orange-400 to-amber-500',
    bg: 'bg-orange-50',
    icon: <img src="/icon.png" alt="뽀시페이" className="w-12 h-12 rounded-full object-cover" />,
    brand: '뽀시페이',
  },
  kakao: {
    name: '카카오페이',
    color: 'from-yellow-300 to-yellow-400',
    bg: 'bg-yellow-50',
    icon: (
      <div className="w-12 h-12 rounded-full bg-[#FEE500] flex items-center justify-center">
        <svg viewBox="0 0 24 24" className="w-7 h-7" fill="#3A1D1D">
          <path d="M12 3C6.477 3 2 6.477 2 10.8c0 2.733 1.617 5.13 4.062 6.54L5.1 20.7a.3.3 0 0 0 .44.327l4.174-2.78A11.6 11.6 0 0 0 12 18.6c5.523 0 10-3.477 10-7.8S17.523 3 12 3z"/>
        </svg>
      </div>
    ),
    brand: '카카오페이',
  },
  toss: {
    name: '토스페이',
    color: 'from-blue-400 to-blue-600',
    bg: 'bg-blue-50',
    icon: (
      <div className="w-12 h-12 rounded-full bg-[#0064FF] flex items-center justify-center">
        <svg viewBox="0 0 24 24" className="w-7 h-7" fill="white">
          <path d="M12.5 4a1.5 1.5 0 0 0-1.5 1.5v5.586L8.207 8.293a1 1 0 0 0-1.414 1.414l4.5 4.5a1 1 0 0 0 1.414 0l4.5-4.5a1 1 0 0 0-1.414-1.414L13 10.586V5.5A1.5 1.5 0 0 0 12.5 4zM6 17a1 1 0 1 0 0 2h12a1 1 0 1 0 0-2H6z"/>
        </svg>
      </div>
    ),
    brand: '토스페이',
  },
  card: {
    name: '신용카드',
    color: 'from-gray-600 to-gray-800',
    bg: 'bg-gray-50',
    icon: (
      <div className="w-12 h-12 rounded-full bg-gradient-to-br from-gray-600 to-gray-800 flex items-center justify-center">
        <svg viewBox="0 0 24 24" className="w-7 h-7" fill="none">
          <rect x="2" y="5" width="20" height="14" rx="2" stroke="white" strokeWidth="1.5"/>
          <rect x="2" y="9" width="20" height="3" fill="white"/>
          <rect x="4" y="15" width="4" height="1.5" rx="0.75" fill="white"/>
        </svg>
      </div>
    ),
    brand: '신용카드',
  },
};

const PGSimulator = ({ deal, paymentMethod, checkoutId, onComplete, onCancel }) => {
  const getInitialStep = () => {
    if (paymentMethod === 'bboshi') return 'auth'; // 주문서에서 이미 상품/금액/배송지 확인 완료
    if (paymentMethod === 'card') return 'processing';
    return 'appRedirect'; // kakao, toss
  };

  const [step, setStep] = useState(getInitialStep);
  const [progress, setProgress] = useState(0);
  const [pgResult, setPgResult] = useState(null);
  const intervalRef = useRef(null);

  const pg = pgStyles[paymentMethod];

  // 신용카드: 외부 GCP Mock PG 호출
  const callExternalPg = async () => {
    try {
      const res = await fetch(MOCK_PG_URL + '/pay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ orderId: String(checkoutId), amount: deal.discountPrice }),
      });
      const data = await res.json();
      setPgResult(data.response ?? data);
      if (data.code === 0 && data.response?.status === 'paid') {
        setStep('done');
        setTimeout(() => onComplete(data.response), 800);
      } else {
        setStep('pgFailed');
      }
    } catch (e) {
      setPgResult({ message: '결제 서버와 통신 중 오류가 발생했습니다.' });
      setStep('pgFailed');
    }
  };

  // 뽀시페이/카카오/토스: 로컬 mock imp_uid 생성 (외부 서버 호출 없음)
  const callMockPg = async () => {
    if (paymentMethod === 'card') {
      return callExternalPg();
    }
    await new Promise(resolve => setTimeout(resolve, 300));
    const mockResponse = {
      imp_uid: `mock_${Date.now()}`,
      status: 'paid',
      amount: deal.discountPrice,
      merchant_uid: String(checkoutId),
    };
    setPgResult(mockResponse);
    setStep('done');
    setTimeout(() => onComplete(mockResponse), 800);
  };

  // 카드: 마운트 즉시 진행 바 → 완료 후 Mock PG 호출
  // 카카오/토스: 2초 앱 연결 시뮬레이션 → Mock PG 호출
  useEffect(() => {
    if (paymentMethod === 'card') {
      startCardPayment();
    } else if (paymentMethod === 'kakao' || paymentMethod === 'toss') {
      const timer = setTimeout(() => callMockPg(), 2000);
      return () => clearTimeout(timer);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const startCardPayment = () => {
    let p = 0;
    intervalRef.current = setInterval(() => {
      p += 5;
      setProgress(Math.min(p, 100));
      if (p >= 100) {
        clearInterval(intervalRef.current);
        callMockPg();
      }
    }, 80);
  };

  // 뽀시페이 전용: 핀 입력 후 결제 처리
  const handleBboshiAuth = () => {
    setStep('processing');
    let p = 0;
    intervalRef.current = setInterval(() => {
      p += 5;
      setProgress(p);
      if (p >= 100) {
        clearInterval(intervalRef.current);
        callMockPg();
      }
    }, 100);
  };


  return (
    <div className="fixed inset-0 z-50 bg-gray-100 flex flex-col">
      {/* 헤더 */}
      <header className={`bg-gradient-to-r ${pg.color} text-white flex-shrink-0`}>
        <div className="px-4 py-3 flex items-center justify-between">
          <button
            onClick={onCancel}
            className="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/20"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          <div className="text-center">
            <p className="font-bold">{pg.brand}</p>
            <p className="text-xs opacity-80">{pg.name}</p>
          </div>
          <div className="w-10" />
        </div>
      </header>

      {/* Mock 안내 배너 */}
      <div className="bg-amber-100 border-b border-amber-200 px-4 py-2 flex-shrink-0">
        <p className="text-amber-800 text-xs text-center">
          🎓 <strong>포트폴리오 시연용 Mock 결제창</strong>입니다. 실제 결제가 진행되지 않습니다.
        </p>
      </div>

      <div className="flex-1 overflow-y-auto">
        <div className="p-4 max-w-md mx-auto pb-16">

          {/* ── 뽀시페이: 핀 입력 (주문서에서 이미 상품/금액/배송지 확인 완료) ── */}
          {step === 'auth' && (
            <div className="space-y-6 pt-8">
              <div className="text-center">
                <div className="text-6xl flex justify-center mb-4">{pg.icon}</div>
                <h2 className="text-xl font-bold text-gray-800 mb-2">결제 인증</h2>
                <p className="text-gray-500 text-sm">비밀번호를 입력해주세요</p>
              </div>
              <div className="flex justify-center gap-3">
                {[1, 2, 3, 4, 5, 6].map(i => (
                  <div key={i} className="w-4 h-4 rounded-full bg-gray-800" />
                ))}
              </div>
              <div className="grid grid-cols-3 gap-2 max-w-xs mx-auto">
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, '', 0, '←'].map((num, i) => (
                  <button
                    key={i}
                    onClick={num !== '' ? handleBboshiAuth : undefined}
                    className={`h-14 rounded-xl font-bold text-xl transition
                      ${num === '' ? '' : 'bg-white hover:bg-gray-50 active:bg-gray-100 shadow-sm'}`}
                  >
                    {num}
                  </button>
                ))}
              </div>
              <p className="text-center text-xs text-gray-400">(아무 버튼이나 누르면 결제가 진행됩니다)</p>
            </div>
          )}

          {/* ── 카카오/토스: 앱 연결 시뮬레이션 ── */}
          {step === 'appRedirect' && (
            <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
              <div className="mb-6">{pg.icon}</div>
              <h2 className="text-xl font-bold text-gray-800 mb-2">{pg.name} 연결 중</h2>
              <p className="text-gray-500 text-sm mb-8">앱으로 이동하여 결제를 진행합니다...</p>
              <div className="w-8 h-8 border-4 border-gray-200 border-t-gray-600 rounded-full animate-spin" />
              <p className="text-xs text-gray-400 mt-6">잠시 후 자동으로 처리됩니다</p>
            </div>
          )}

          {/* ── 처리 중 (뽀시페이/카드 공용) ── */}
          {step === 'processing' && (
            <div className="flex flex-col items-center justify-center min-h-[400px]">
              <div className="text-6xl flex justify-center mb-6">{pg.icon}</div>
              <h2 className="text-xl font-bold text-gray-800 mb-2">결제 처리 중</h2>
              <p className="text-gray-500 text-sm mb-6">잠시만 기다려주세요...</p>
              <div className="w-full max-w-xs">
                <div className="bg-gray-200 rounded-full h-2 overflow-hidden">
                  <div
                    className={`h-full bg-gradient-to-r ${pg.color} transition-all duration-100`}
                    style={{ width: `${progress}%` }}
                  />
                </div>
                <p className="text-center text-sm text-gray-400 mt-2">{progress}%</p>
              </div>
            </div>
          )}

          {/* ── 완료 ── */}
          {step === 'done' && (
            <div className="flex flex-col items-center justify-center min-h-[400px]">
              <div className={`w-24 h-24 rounded-full bg-gradient-to-r ${pg.color} flex items-center justify-center mb-6 shadow-lg`}>
                <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h2 className="text-xl font-bold text-gray-800 mb-2">결제 완료!</h2>
              <p className="text-gray-500 text-sm flex items-center gap-2">
                <span className="w-3 h-3 border-2 border-gray-300 border-t-gray-500 rounded-full animate-spin inline-block" />
                주문 확정 중...
              </p>
              <div className="mt-6 bg-gray-50 rounded-xl p-4 w-full max-w-xs space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">결제 금액</span>
                  <span className="font-bold text-gray-800">{deal.discountPrice.toLocaleString()}원</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">결제 수단</span>
                  <span className="text-gray-800">{pgResult?.card_name || pg.brand}</span>
                </div>
                {pgResult?.pg_provider && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">PG사</span>
                    <span className="text-gray-800 uppercase">{pgResult.pg_provider}</span>
                  </div>
                )}
                {pgResult?.imp_uid && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">승인번호</span>
                    <span className="text-gray-800 font-mono text-xs">{pgResult.imp_uid}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ── Mock PG 결제 실패 (카드 전용) ── */}
          {step === 'pgFailed' && (
            <div className="flex flex-col items-center justify-center min-h-[400px]">
              <div className="w-24 h-24 rounded-full bg-red-100 flex items-center justify-center mb-6">
                <svg className="w-12 h-12 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>
              <h2 className="text-xl font-bold text-gray-800 mb-2">결제 실패</h2>
              <p className="text-gray-500 text-sm mb-2">{pgResult?.message || '카드사 승인이 거절되었습니다.'}</p>
              {pgResult?.response?.fail_reason && (
                <p className="text-xs text-red-400 mb-6">{pgResult.response.fail_reason}</p>
              )}
              <button onClick={onCancel}
                className="px-6 py-3 bg-gray-800 text-white rounded-xl font-medium hover:bg-gray-700 transition">
                다시 시도하기
              </button>
            </div>
          )}

        </div>
      </div>

      {/* 하단 안내 */}
      <div className="flex-shrink-0 bg-white border-t px-4 py-3">
        <p className="text-xs text-gray-400 text-center">
          본 결제창은 포트폴리오 시연 목적으로 제작되었습니다.<br />
          실제 결제 시스템과 무관합니다.
        </p>
      </div>
    </div>
  );
};

export default PGSimulator;
