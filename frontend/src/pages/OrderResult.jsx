import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { getOrder } from '../api/order';

const OrderResult = () => {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  
  const [order, setOrder] = useState(location.state?.order || null);
  const [loading, setLoading] = useState(!location.state?.order);
  const paymentMethod = location.state?.paymentMethod;
  const pgResponse = location.state?.pgResponse;

  const fmt = (ms) => new Date(ms).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const baseRef = useRef(Date.now());
  const t1 = fmt(baseRef.current);
  const t2 = fmt(baseRef.current + 300);
  const t3 = fmt(baseRef.current + 800);
  const t4 = fmt(baseRef.current + 1200);

  const fetchOrder = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getOrder(id);
      setOrder(data);
    } catch (err) {
      console.error(err);
      setOrder(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (!order) {
      fetchOrder();
    }
  }, [id, order, fetchOrder]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-cream-200">
        <div className="w-12 h-12 border-4 border-primary-200 border-t-primary-500 rounded-full animate-spin" />
      </div>
    );
  }

  // 주문 정보를 불러올 수 없는 경우 → 홈으로 이동
  if (!order) {
    navigate('/', { replace: true });
    return null;
  }

  // 백엔드: PAID / 목업: PAID or COMPLETED 모두 허용
  const isSuccess = order?.status === 'PAID' || order?.status === 'COMPLETED' || !!order?.checkoutId;

  return (
    <div className="min-h-screen bg-cream-200">
      {/* 결과 헤더 */}
      <div className={`${isSuccess ? 'bg-green-500' : 'bg-red-500'} text-white`}>
        <div className="container mx-auto px-4 py-12 text-center">
          <div className="w-20 h-20 bg-white/20 rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="text-5xl">{isSuccess ? '✓' : '✗'}</span>
          </div>
          <h1 className="text-2xl font-bold mb-2">
            {isSuccess ? '주문이 완료되었습니다!' : '주문에 실패했습니다'}
          </h1>
          <p className="text-white/80">
            {isSuccess 
              ? '빠르게 배송 준비할게요 🚀' 
              : '다시 시도해 주세요'}
          </p>
        </div>
      </div>

      <main className="container mx-auto px-4 -mt-6">
        {/* 주문 정보 카드 */}
        <div className="bg-white rounded-2xl shadow-lg overflow-hidden">
          {/* 주문번호 */}
          <div className="p-5 border-b">
            <div className="flex justify-between items-center">
              <span className="text-gray-500">주문번호</span>
              <span className="font-mono font-semibold text-gray-800 text-xs break-all">
                {order.checkoutId ?? order.orderId ?? id}
              </span>
            </div>
          </div>

          {/* 상품 정보 */}
          <div className="p-5 border-b">
            <div className="flex gap-4">
              <div className="w-20 h-20 bg-gray-100 rounded-xl" />
              <div className="flex-1">
                <p className="font-medium text-gray-800 mb-1">{order.productName}</p>
                <p className="text-sm text-gray-500">수량: {order.quantity}개</p>
              </div>
            </div>
          </div>

          {/* 결제 정보 */}
          {isSuccess && (
            <div className="p-5 border-b space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">결제수단</span>
                <span className="text-gray-800">
                  {pgResponse?.card_name || (
                    <>
                      {paymentMethod === 'kakao' && '카카오페이'}
                      {paymentMethod === 'naver' && '네이버페이'}
                      {paymentMethod === 'toss' && '토스페이'}
                      {paymentMethod === 'card' && '신용/체크카드'}
                      {!paymentMethod && '간편결제'}
                    </>
                  )}
                </span>
              </div>
              {pgResponse?.pg_provider && (
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">PG사</span>
                  <span className="text-gray-800 uppercase">{pgResponse.pg_provider}</span>
                </div>
              )}
              {pgResponse?.imp_uid && (
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">승인번호</span>
                  <span className="text-gray-800 font-mono text-xs">{pgResponse.imp_uid}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-gray-500">결제금액</span>
                <span className="text-xl font-bold text-primary-500">
                  {order.totalPrice?.toLocaleString()}원
                </span>
              </div>
            </div>
          )}

          {/* 실패 사유 */}
          {!isSuccess && (
            <div className="p-5 border-b">
              <div className="bg-red-50 rounded-xl p-4">
                <p className="text-sm font-medium text-red-700 mb-1">실패 사유</p>
                <p className="text-red-600">{order?.failReason || '재고 소진'}</p>
              </div>
              
            </div>
          )}

          {/* 주문 처리 흐름 */}
          <div className="p-5">
            <p className="text-sm font-medium text-gray-700 mb-4">처리 내역</p>
            <div className="space-y-3">
              <ProcessStep
                icon="📝"
                label="주문 생성"
                time={t1}
                status="done"
              />
              <ProcessStep
                icon="📦"
                label="재고 확보"
                time={t2}
                status="done"
              />
              {isSuccess ? (
                <>
                  <ProcessStep
                    icon="💳"
                    label="결제 완료"
                    time={t3}
                    status="done"
                  />
                  <ProcessStep
                    icon="✅"
                    label="주문 완료"
                    time={t4}
                    status="done"
                    isLast
                  />
                </>
              ) : (
                <>
                  <ProcessStep
                    icon="❌"
                    label="결제 실패"
                    time={t3}
                    status="failed"
                    desc={order?.failReason}
                  />
                  <ProcessStep
                    icon="🚫"
                    label="주문 취소"
                    time={t4}
                    status="cancelled"
                    isLast
                  />
                </>
              )}
            </div>
          </div>
        </div>

        {/* 버튼 */}
        <div className="mt-6 space-y-3 pb-8">
          {isSuccess ? (
            <>
              <button
                onClick={() => navigate('/')}
                className="w-full py-4 bg-primary-500 text-white font-bold rounded-xl hover:bg-primary-600 transition"
              >
                딜 목록으로 돌아가기
              </button>
              <button
                onClick={() => navigate('/monitor')}
                className="w-full py-4 bg-white text-gray-700 font-medium rounded-xl border hover:bg-gray-50 transition"
              >
                주문 처리 현황 보기
              </button>
            </>
          ) : (
            <>
              <button 
                onClick={() => navigate(-1)}
                className="w-full py-4 bg-primary-500 text-white font-bold rounded-xl hover:bg-primary-600 transition"
              >
                다시 시도하기
              </button>
              <button 
                onClick={() => navigate('/')}
                className="w-full py-4 bg-white text-gray-700 font-medium rounded-xl border hover:bg-gray-50 transition"
              >
                목록으로 돌아가기
              </button>
            </>
          )}
        </div>
      </main>
    </div>
  );
};

/* ─────────────────────────────────────────────────────────────────
   처리 단계 컴포넌트
───────────────────────────────────────────────────────────────── */
const ProcessStep = ({ icon, label, time, status, desc, isLast }) => {
  const statusStyles = {
    done: 'bg-green-100 text-green-600 border-green-200',
    failed: 'bg-red-100 text-red-600 border-red-200',
    rollback: 'bg-orange-100 text-orange-600 border-orange-200',
    cancelled: 'bg-gray-100 text-gray-600 border-gray-200',
  };

  return (
    <div className="flex gap-3">
      {/* 타임라인 */}
      <div className="flex flex-col items-center">
        <div className={`w-10 h-10 rounded-full flex items-center justify-center border-2 ${statusStyles[status]}`}>
          {icon}
        </div>
        {!isLast && <div className="w-0.5 h-6 bg-gray-200 my-1" />}
      </div>
      
      {/* 내용 */}
      <div className="flex-1 pb-2">
        <div className="flex justify-between items-center">
          <span className="font-medium text-gray-800">{label}</span>
          <span className="text-xs text-gray-400">{time}</span>
        </div>
        {desc && <p className="text-sm text-gray-500 mt-1">{desc}</p>}
      </div>
    </div>
  );
};

export default OrderResult;
