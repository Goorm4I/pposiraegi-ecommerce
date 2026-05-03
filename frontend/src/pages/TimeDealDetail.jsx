import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getTimeDeal, getTimeDeals } from '../api/timedeal';
import { getCurrentUser, getAddress } from '../api/auth';
import CountdownTimer from '../components/CountdownTimer';
import StockProgress from '../components/StockProgress';
import TimeDealCard from '../components/TimeDealCard';

const TimeDealDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [deal, setDeal] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedImage, setSelectedImage] = useState(0);
  const [relatedDeals, setRelatedDeals] = useState([]);

  const [checkingStock, setCheckingStock] = useState(false);
  const [stockError, setStockError] = useState(null);
  const [alreadyPurchased, setAlreadyPurchased] = useState(false);

  const user = getCurrentUser();
  const [showAddressBanner, setShowAddressBanner] = useState(
    () => !!user && !getAddress()
  );

  const fetchDeal = useCallback(async () => {
    try {
      setLoading(prev => prev);
      const data = await getTimeDeal(id);
      setDeal(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [id]);

  const fetchRelatedDeals = useCallback(async () => {
    try {
      const all = await getTimeDeals();
      const related = all
        .filter(d => d.id !== Number(id) && (d.status === 'ACTIVE' || d.status === 'UPCOMING'))
        .slice(0, 3);
      setRelatedDeals(related);
    } catch {}
  }, [id]);

  useEffect(() => {
    window.scrollTo(0, 0);
    fetchDeal();
    fetchRelatedDeals();
    setSelectedImage(0);
    if (user) {
      const key = `paid_deal_${user.id}_${id}`;
      setAlreadyPurchased(!!sessionStorage.getItem(key));
    } else {
      setAlreadyPurchased(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => {
    if (!deal) return;
    const interval = setInterval(fetchDeal, 5000);
    return () => clearInterval(interval);
  }, [deal, fetchDeal]);

  const handleDealExpire = useCallback(() => {
    setDeal(prev => {
      if (!prev || prev.status !== 'ACTIVE') return prev;
      return { ...prev, status: 'ENDED', stock: 0 };
    });
  }, []);

  const handleDealStart = useCallback(() => {
    setDeal(prev => {
      if (!prev || prev.status !== 'UPCOMING') return prev;
      return { ...prev, status: 'ACTIVE' };
    });
  }, []);

  const handlePurchaseClick = async () => {
    if (!getCurrentUser()) { navigate('/login'); return; }
    if (alreadyPurchased) return;
    if (deal.status === 'ACTIVE' && deal.stock > 0) {
      navigate(`/order/checkout/${id}`);
      return;
    }
    setCheckingStock(true);
    setStockError(null);
    try {
      const latestDeal = await getTimeDeal(id);
      setDeal(latestDeal);
      if (latestDeal.stock <= 0) { setStockError('앗! 방금 품절되었어요 😢'); return; }
      if (latestDeal.status !== 'ACTIVE') { setStockError('타임딜이 종료되었어요'); return; }
      navigate(`/order/checkout/${id}`);
    } catch {
      setStockError('재고 확인 중 오류가 발생했어요');
    } finally {
      setCheckingStock(false);
    }
  };

  if (loading) return <LoadingScreen />;
  if (error) return <ErrorScreen error={error} onRetry={() => navigate('/')} />;

  const images = deal.images || [deal.productImage];
  const canPurchase = deal.status === 'ACTIVE' && deal.stock > 0 && !alreadyPurchased;
  const isUpcoming = deal.status === 'UPCOMING';

  return (
    <div className="min-h-screen bg-[#faf6f0] flex flex-col">
      <header className="bg-[#faf6f0] border-b border-brand-100 relative z-10 isolate">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between h-16">
            <Link to="/" className="flex items-center hover:opacity-75 transition">
              <img src="/icon1.png" alt="뽀시래기" className="h-10 object-contain" />
            </Link>
            <div className="flex items-center gap-1">
              {user ? (
                <>
                  <Link to="/wishlist"
                    className="w-10 h-10 flex items-center justify-center rounded-full hover:bg-brand-100 transition"
                    title="찜목록">
                    <svg className="w-6 h-6 text-[#7c4a1e]" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                    </svg>
                  </Link>
                  <Link to="/mypage"
                    className="w-10 h-10 flex items-center justify-center rounded-full hover:bg-brand-100 transition"
                    title="마이페이지">
                    <svg className="w-6 h-6 text-[#7c4a1e]" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                      <circle cx="12" cy="7" r="4" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </Link>
                </>
              ) : (
                <>
                  <Link to="/login"
                    className="px-4 py-2 text-sm font-semibold text-brand-700 hover:text-brand-500 transition rounded-full hover:bg-brand-50">
                    로그인
                  </Link>
                  <Link to="/register"
                    className="px-4 py-2 text-sm font-semibold bg-brand-500 text-white rounded-full hover:bg-brand-400 active:scale-95 transition">
                    회원가입
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      <main className="flex-1 pb-4">
        <div className="bg-transparent">
          <div className="container mx-auto px-4 py-4 max-w-3xl">
            <div className="relative bg-brand-100 rounded-2xl overflow-hidden mb-3">
              <div
                className="flex transition-transform duration-300 ease-in-out"
                style={{ transform: `translateX(-${selectedImage * 100}%)` }}
              >
                {images.map((img, idx) => (
                  <img
                    key={idx}
                    src={img}
                    alt={`${deal.productName} ${idx + 1}`}
                    className="w-full aspect-square object-cover max-h-96 flex-shrink-0"
                  />
                ))}
              </div>
              <div className="absolute top-4 left-4">
                <span className="bg-brand-800 text-white px-4 py-2 rounded-xl font-bold text-lg shadow-lg">
                  {deal.discountRate}% SALE
                </span>
              </div>
              {deal.status === 'ACTIVE' && (
                <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent p-4">
                  <div className="flex items-center justify-center gap-2 text-white">
                    <span className="text-sm opacity-80">남은 시간</span>
                    <CountdownTimer targetTime={deal.endTime} type="end" onExpire={handleDealExpire} />
                  </div>
                </div>
              )}
              {isUpcoming && (
                <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent p-4">
                  <div className="flex items-center justify-center gap-2 text-white">
                    <span className="text-sm opacity-80">오픈까지</span>
                    <CountdownTimer targetTime={deal.startTime} type="start" onExpire={handleDealStart} />
                  </div>
                </div>
              )}
            </div>

            {images.length > 1 && (
              <div className="flex gap-2 overflow-x-auto pb-1">
                {images.map((img, idx) => (
                  <button
                    key={idx}
                    onMouseEnter={() => setSelectedImage(idx)}
                    onClick={() => setSelectedImage(idx)}
                    className={`flex-shrink-0 w-16 h-16 rounded-xl overflow-hidden border-2 transition-all ${
                      selectedImage === idx ? 'border-brand-500 opacity-100' : 'border-transparent opacity-60 hover:opacity-90'
                    }`}
                  >
                    <img src={img} alt={`상품 이미지 ${idx + 1}`} className="w-full h-full object-cover" />
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="bg-white rounded-2xl mt-4 p-6 max-w-3xl mx-auto shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              {deal.status === 'ACTIVE' ? (
                <span className="badge bg-brand-500 text-white">
                  <span className="inline-block w-1.5 h-1.5 bg-white rounded-full mr-1.5 animate-pulse"></span>
                  LIVE
                </span>
              ) : isUpcoming ? (
                <span className="badge bg-brand-300 text-brand-800">오픈예정</span>
              ) : (
                <span className="badge bg-brand-200 text-brand-600">종료</span>
              )}
              {!isUpcoming && deal.stock <= 10 && deal.stock > 0 && (
                <span className="badge bg-brand-500/10 text-brand-500">{deal.stock}개 남음</span>
              )}
            </div>
          </div>

          <h2 className="text-xl font-bold text-brand-800 mb-4 leading-snug">{deal.productName}</h2>

          <div className="flex items-baseline gap-3 mb-4">
            <span className="text-3xl font-bold text-brand-800">
              {deal.discountPrice.toLocaleString()}
              <span className="text-base font-medium text-brand-600">원</span>
            </span>
            <span className="text-lg text-brand-400 line-through">{deal.originalPrice.toLocaleString()}원</span>
          </div>

          <div className="bg-brand-50 rounded-2xl p-4 mb-4">
            <div className="flex items-center gap-4 text-sm">
              <div className="flex items-center gap-1.5 text-brand-600"><span className="text-brand-500">✓</span>무료배송</div>
              <div className="flex items-center gap-1.5 text-brand-600"><span className="text-brand-500">✓</span>당일발송</div>
            </div>
          </div>

          <StockProgress
            current={isUpcoming ? 0 : deal.totalStock - deal.stock}
            total={deal.totalStock}
            hideCount={isUpcoming}
          />
        </div>

        <div className="bg-white rounded-2xl mt-4 p-6 max-w-3xl mx-auto shadow-sm">
          <h3 className="font-bold text-brand-800 mb-4">상품 정보</h3>
          <div className="text-sm text-brand-600 space-y-2">
            {(deal.features || [
              '• 프리미엄 원료 사용',
              '• 수의사 추천 제품',
              '• 안전성 검증 완료',
              '• 국내 생산, 엄격한 품질 관리',
            ]).map((f, i) => <p key={i}>{f}</p>)}
          </div>
        </div>

        <div className="bg-white rounded-2xl mt-4 p-6 max-w-3xl mx-auto shadow-sm">
          <h3 className="font-bold text-brand-800 mb-4">구매 안내</h3>
          <div className="text-xs text-brand-500 space-y-1.5">
            <p>• 타임딜 상품은 한정 수량으로 조기 품절될 수 있습니다.</p>
            <p>• 결제 완료 후 재고 소진 시 자동 환불 처리됩니다.</p>
            <p>• 타임딜 상품은 쿠폰 적용이 불가합니다.</p>
          </div>
        </div>

        {relatedDeals.length > 0 && (
          <div className="bg-white rounded-2xl mt-4 p-6 max-w-3xl mx-auto shadow-sm">
            <h3 className="font-bold text-brand-800 mb-4">🔥 지금 인기있는 다른 딜</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {relatedDeals.map(d => (
                <TimeDealCard key={d.id} deal={d} onDealExpire={handleDealExpire} />
              ))}
            </div>
          </div>
        )}
      </main>

      <div className="sticky bottom-0 bg-white border-t border-brand-200 z-50">
        <div className="container mx-auto px-4 py-4 max-w-3xl">
          {showAddressBanner && (
            <div className="mb-3 p-3 bg-brand-50 border border-brand-200 rounded-2xl flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-brand-100 flex items-center justify-center flex-shrink-0">
                <svg className="w-4 h-4 text-brand-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-brand-800">배송지가 등록되지 않았습니다</p>
                <p className="text-xs text-brand-500">주문 전에 배송지를 먼저 등록해 주세요.</p>
              </div>
              <div className="flex items-center gap-2 flex-shrink-0">
                <button
                  onClick={() => navigate('/address')}
                  className="px-3 py-1.5 text-xs font-semibold border border-brand-400 text-brand-600 rounded-xl hover:bg-brand-100 transition"
                >
                  배송지 관리
                </button>
                <button
                  onClick={() => setShowAddressBanner(false)}
                  className="p-1 text-brand-400 hover:text-brand-600 transition"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          )}
          {alreadyPurchased && (
            <div className="mb-3 p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3">
              <span className="text-amber-500 text-base flex-shrink-0 mt-0.5">⚠️</span>
              <div>
                <p className="text-amber-800 text-sm font-semibold mb-0.5">구매 완료된 상품입니다</p>
                <p className="text-amber-700 text-xs">금일 구매 가능한 수량이 모두 소진되었습니다 (1인 1구매)</p>
              </div>
            </div>
          )}
          {stockError && (
            <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-xl text-center">
              <p className="text-red-600 text-sm font-medium">{stockError}</p>
            </div>
          )}
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <p className="text-xs text-brand-500">타임딜가</p>
              <p className="text-2xl font-bold text-brand-800">{deal.discountPrice.toLocaleString()}원</p>
            </div>
            <button
              onClick={handlePurchaseClick}
              disabled={isUpcoming || deal.stock === 0 || checkingStock || alreadyPurchased}
              className={`px-8 py-4 rounded-2xl font-bold text-lg transition ${
                canPurchase && !checkingStock
                  ? 'bg-brand-500 text-white hover:bg-brand-400 active:scale-[0.98]'
                  : 'bg-brand-200 text-brand-400 cursor-not-allowed'
              }`}
            >
              {checkingStock ? (
                <span className="flex items-center gap-2">
                  <span className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  확인중
                </span>
              ) : alreadyPurchased ? '이미 구매한 상품입니다'
                : deal.stock === 0 ? '품절'
                : isUpcoming ? (
                  <span className="flex flex-col items-center leading-tight">
                    <span className="text-xs font-normal opacity-70">구매까지</span>
                    <CountdownTimer targetTime={deal.startTime} type="start" onExpire={handleDealStart} />
                  </span>
                )
                : '구매하기'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

const LoadingScreen = () => (
  <div className="min-h-screen flex items-center justify-center bg-brand-50">
    <div className="text-center">
      <div className="w-16 h-16 border-4 border-brand-200 border-t-brand-500 rounded-full animate-spin mx-auto mb-4" />
      <p className="text-brand-600">로딩 중...</p>
    </div>
  </div>
);

const ErrorScreen = ({ error, onRetry }) => (
  <div className="min-h-screen flex items-center justify-center bg-brand-50">
    <div className="text-center px-8">
      <span className="text-5xl mb-4 block">🐾</span>
      <p className="text-brand-800 font-medium mb-2">문제가 발생했어요</p>
      <p className="text-brand-600 text-sm mb-6">{error}</p>
      <button onClick={onRetry} className="btn-primary">돌아가기</button>
    </div>
  </div>
);

export default TimeDealDetail;