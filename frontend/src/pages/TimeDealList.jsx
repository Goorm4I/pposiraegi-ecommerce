import React, { useState, useEffect, useCallback } from 'react';
import { getTimeDeals } from '../api/timedeal';
import TimeDealCard from '../components/TimeDealCard';
import CountdownTimer from '../components/CountdownTimer';
import { Link, useNavigate } from 'react-router-dom';
import { getCurrentUser, logout } from '../api/auth';

const TimeDealList = () => {
  const [deals, setDeals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [user, setUser] = useState(getCurrentUser());
  const [authKey, setAuthKey] = useState(0);
  const [activeFilter, setActiveFilter] = useState('popular');
  const [filterOpen, setFilterOpen] = useState(false);
  const [bannerIndex, setBannerIndex] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    fetchDeals();
  }, []);

  const fetchDeals = async () => {
    try {
      setLoading(true);
      const data = await getTimeDeals();
      setDeals(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDealEnd = useCallback((dealId) => {
    setDeals(prev => prev.map(d =>
      d.id === dealId ? { ...d, status: 'ENDED', stock: 0 } : d
    ));
  }, []);

  const handleDealStart = useCallback((dealId) => {
    setDeals(prev => prev.map(d =>
      d.id === dealId ? { ...d, status: 'ACTIVE' } : d
    ));
  }, []);

  // ✅ hooks를 early return 위에 선언
  const resolvedBannerDeals = React.useMemo(() => {
    const active = deals.filter(d => d.status === 'ACTIVE');
    if (active.length === 0) return [];
    const shuffled = [...active].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, 3);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deals.length]);

  // 홍보 슬라이드 + 딜 슬라이드 합치기 (홍보가 항상 첫 번째)
  const PROMO_SLIDE = { id: 'promo', isPromo: true };
  const allBannerSlides = resolvedBannerDeals.length > 0
    ? [PROMO_SLIDE, ...resolvedBannerDeals]
    : [];

  useEffect(() => {
    if (allBannerSlides.length < 2) return;
    const timer = setInterval(() => {
      setBannerIndex(i => (i + 1) % allBannerSlides.length);
    }, 4000);
    return () => clearInterval(timer);
  }, [allBannerSlides.length]);

  if (loading) {
    return (
      <div className="min-h-screen bg-mesh flex items-center justify-center">
        <div className="text-center">
          <div className="relative w-16 h-16 mx-auto mb-4">
            <div className="absolute inset-0 border-4 border-brand-200 rounded-full"></div>
            <div className="absolute inset-0 border-4 border-transparent border-t-brand-500 rounded-full animate-spin"></div>
          </div>
          <p className="text-brand-600">로딩 중...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-mesh flex items-center justify-center">
        <div className="text-center">
          <span className="text-5xl mb-4 block">🐾</span>
          <p className="text-brand-800 font-medium mb-4">{error}</p>
          <button onClick={fetchDeals} className="btn-primary">다시 시도</button>
        </div>
      </div>
    );
  }

  const activeDeals = deals.filter(d => d.status === 'ACTIVE');

  const FILTERS = [
    { key: 'popular',  label: '인기순' },
    { key: 'deadline', label: '마감임박순' },
    { key: 'latest',   label: '최신순' },
    { key: 'priceAsc', label: '가격 낮은순' },
    { key: 'priceDesc', label: '가격 높은순' },
  ];

  const sortedActiveDeals = [...activeDeals].sort((a, b) => {
    if (activeFilter === 'popular') {
      const rateA = (a.totalStock - a.stock) / a.totalStock;
      const rateB = (b.totalStock - b.stock) / b.totalStock;
      return rateB - rateA;
    } else if (activeFilter === 'deadline') {
      return new Date(a.endTime) - new Date(b.endTime);
    } else if (activeFilter === 'latest') {
      return new Date(b.startTime) - new Date(a.startTime);
    } else if (activeFilter === 'priceAsc') {
      return a.discountPrice - b.discountPrice;
    } else {
      return b.discountPrice - a.discountPrice;
    }
  });

  const upcomingDeals = deals.filter(d => d.status === 'UPCOMING');
  const endedDeals = deals.filter(d => d.status === 'SOLDOUT' || d.status === 'ENDED');

  return (
    <div className="min-h-screen bg-mesh">
      {/* 상단 네비게이션 바 */}
      <header className="bg-[#faf6f0] border-b border-brand-100">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between h-16">

            {/* 로고 */}
            <Link to="/" className="flex items-center hover:opacity-75 transition">
              <img src="/icon1.png" alt="뽀시래기" className="h-10 object-contain" />
            </Link>

            {/* 우측 버튼 */}
            <div className="flex items-center gap-1">
              {user ? (
                <>
                  {/* 관리자 버튼 */}
                  {user.email === 'admin@test.com' && (
                    <Link to="/admin"
                      className="px-3 py-1.5 text-xs font-bold bg-brand-800 text-white rounded-full hover:bg-brand-700 transition mr-1"
                      title="관리자 콘솔">
                      👑 관리자
                    </Link>
                  )}
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

      <main className="container mx-auto px-4 pb-12 pt-8">

        {/* 홍보 배너 슬라이더 + 양옆 콘텐츠 */}
        {allBannerSlides.length > 0 && (
          <section className="mb-10">
            <div className="flex gap-4" style={{ minHeight: '20rem' }}>

              {/* 좌측: 딜 현황 요약 */}
              <div className="hidden lg:flex flex-col gap-3 w-52 flex-shrink-0">
                <div className="bg-white rounded-2xl p-5 shadow-sm flex-1 flex flex-col justify-center items-center text-center">
                  <div className="w-12 h-12 rounded-full bg-brand-100 flex items-center justify-center mb-3 text-2xl">
                    🐾
                  </div>
                  <p className="text-4xl font-bold text-brand-800">{activeDeals.length}</p>
                  <p className="text-sm text-brand-500 mt-1.5 font-medium">지금 진행중인 딜</p>
                </div>
                <div className="bg-white rounded-2xl p-5 shadow-sm flex-1 flex flex-col justify-center items-center text-center">
                  <div className="w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center mb-3 text-2xl">
                    ⏰
                  </div>
                  <p className="text-4xl font-bold text-brand-800">{upcomingDeals.length}</p>
                  <p className="text-sm text-brand-500 mt-1.5 font-medium">오픈 예정 딜</p>
                </div>
              </div>

              {/* 중앙: 배너 슬라이더 */}
              <div className="relative rounded-2xl overflow-hidden shadow-sm flex-1 self-stretch min-h-72"
                onClick={() => {
                  const slide = allBannerSlides[bannerIndex];
                  if (!slide.isPromo) navigate(`/deal/${slide.id}`);
                }}
                style={{ cursor: allBannerSlides[bannerIndex]?.isPromo ? 'default' : 'pointer' }}
              >
                <div className="absolute inset-0 overflow-hidden rounded-2xl">
                  <div
                    className="flex h-full transition-transform duration-500 ease-in-out"
                    style={{ transform: `translateX(-${bannerIndex * 100}%)` }}
                  >
                    {allBannerSlides.map((slide, idx) => (
                      slide.isPromo ? (
                        /* ── 홍보 슬라이드 ── */
                        <div key="promo" className="relative w-full h-full flex-shrink-0 flex items-center px-10"
                          style={{ background: 'linear-gradient(135deg, #7c4a1e 0%, #c47a3a 40%, #e8a85a 70%, #d4813a 100%)' }}>
                          {/* 배경 장식 */}
                          <div className="absolute inset-0 overflow-hidden pointer-events-none">
                            <div className="absolute -top-10 -right-10 w-64 h-64 rounded-full bg-white/5" />
                            <div className="absolute -bottom-16 -left-8 w-56 h-56 rounded-full bg-white/5" />
                          </div>

                          {/* 우측 이모티콘 — 산발 배치 */}
                          <div className="absolute right-0 top-0 bottom-0 w-96 hidden sm:block pointer-events-none select-none">
                            <span className="absolute text-7xl" style={{ top: '12%', right: '30%' }}>🐶</span>
                            <span className="absolute text-6xl" style={{ bottom: '15%', right: '10%' }}>🐱</span>
                            <span className="absolute text-4xl" style={{ top: '8%', right: '8%' }}>✨</span>
                            <span className="absolute text-5xl" style={{ top: '48%', right: '52%' }}>🐾</span>
                            <span className="absolute text-3xl" style={{ bottom: '8%', right: '48%' }}>⭐</span>
                            <span className="absolute text-3xl" style={{ top: '30%', right: '5%' }}>🦴</span>
                          </div>

                          {/* 텍스트 */}
                          <div className="relative z-10" style={{ maxWidth: '55%' }}>
                            <p className="text-white/80 text-base font-medium mb-3">
                              프리미엄 반려용품을 특별한 가격으로
                            </p>
                            <p className="text-white font-bold text-4xl leading-snug whitespace-nowrap">
                              오늘 우리 애 간식값,<br />
                              <span className="text-yellow-200">뽀시래기 타임딜</span>로 '뽀시'기 성공!
                            </p>
                          </div>
                        </div>
                      ) : (
                        /* ── 딜 슬라이드 ── */
                        <div key={slide.id} className="relative w-full h-full flex-shrink-0">
                          <img src={slide.productImage} alt={slide.productName}
                            className="w-full h-full object-cover object-center" />
                          <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/30 to-transparent" />
                          <div className="absolute bottom-16 left-0 right-0 px-6">
                            <p className="text-white font-bold text-3xl leading-tight truncate mb-2">
                              {slide.productName.split('|')[0].trim()}
                            </p>
                            {slide.productName.split('|')[1] && (
                              <p className="text-white/80 text-base font-medium truncate">
                                {slide.productName.split('|')[1].trim()}
                              </p>
                            )}
                          </div>
                        </div>
                      )
                    ))}
                  </div>
                </div>

                {/* 하단 컨트롤 바 */}
                <div className="absolute bottom-0 left-0 right-0 px-5 py-3 flex items-center gap-3">
                  <div className="flex gap-1.5 flex-1">
                    {allBannerSlides.map((_, idx) => (
                      <button key={idx}
                        onClick={e => { e.stopPropagation(); setBannerIndex(idx); }}
                        className={`h-0.5 rounded-full transition-all duration-300 ${idx === bannerIndex ? 'bg-white flex-[2]' : 'bg-white/40 flex-1'}`}
                      />
                    ))}
                  </div>
                  <span className="text-white/70 text-xs tabular-nums">{bannerIndex + 1} / {allBannerSlides.length}</span>
                  <button onClick={e => { e.stopPropagation(); setBannerIndex(i => (i - 1 + allBannerSlides.length) % allBannerSlides.length); }}
                    className="w-7 h-7 bg-white/20 hover:bg-white/40 backdrop-blur-sm rounded-full flex items-center justify-center transition">
                    <svg className="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                    </svg>
                  </button>
                  <button onClick={e => { e.stopPropagation(); setBannerIndex(i => (i + 1) % allBannerSlides.length); }}
                    className="w-7 h-7 bg-white/20 hover:bg-white/40 backdrop-blur-sm rounded-full flex items-center justify-center transition">
                    <svg className="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </button>
                </div>
              </div>

              {/* 우측: 마감 임박 딜 */}
              {(() => {
                const urgentDeal = [...activeDeals].sort((a, b) => new Date(a.endTime) - new Date(b.endTime))[0];
                if (!urgentDeal) return null;
                return (
                  <div className="hidden lg:flex flex-col w-52 flex-shrink-0 bg-white rounded-2xl shadow-sm overflow-hidden">
                    {/* 상단: 이미지 — object-top으로 잘림 방지 */}
                    <div className="relative cursor-pointer" onClick={() => navigate(`/deal/${urgentDeal.id}`)}>
                      <img src={urgentDeal.productImage} alt={urgentDeal.productName}
                        className="w-full h-36 object-cover object-top" />
                      <div className="absolute top-2 left-2">
                        <span className="bg-red-500 text-white text-xs font-bold px-2 py-1 rounded-full">🔥 마감 임박</span>
                      </div>
                    </div>
                    {/* 하단: 텍스트 + 타이머 + 버튼 */}
                    <div className="flex flex-col flex-1 p-4 justify-between">
                      <p className="text-sm text-brand-800 font-semibold leading-snug line-clamp-3 mb-3 cursor-pointer"
                        onClick={() => navigate(`/deal/${urgentDeal.id}`)}>
                        {urgentDeal.productName.split('|')[0].trim()}
                      </p>
                      <div>
                        <div className="bg-red-50 rounded-xl px-3 py-2.5 mb-3 text-center flex flex-col items-center">
                          <p className="text-xs text-red-400 mb-1 font-medium">남은 시간</p>
                          <CountdownTimer targetTime={urgentDeal.endTime} type="end" />
                        </div>
                        <button
                          onClick={() => navigate(`/order/checkout/${urgentDeal.id}`)}
                          className="w-full py-2.5 bg-brand-500 text-white text-sm font-bold rounded-xl hover:bg-brand-400 active:scale-[0.98] transition"
                        >
                          바로 구매
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })()}

            </div>
          </section>
        )}

        {/* 지금 진행중인 딜 */}
        {activeDeals.length > 0 && (
          <section className="mb-12">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2 bg-brand-500 text-white px-4 py-2 rounded-full">
                  <span className="w-2 h-2 bg-white rounded-full animate-pulse"></span>
                  <span className="font-semibold text-sm">LIVE</span>
                </div>
                <h2 className="text-xl font-bold text-brand-800">지금 진행중인 딜</h2>
              </div>
              {/* 필터 드롭다운 */}
              <div className="relative">
                <button
                  onClick={() => setFilterOpen(o => !o)}
                  className="flex items-center gap-2 px-4 py-2 bg-white border border-brand-200 rounded-full text-sm font-medium text-brand-700 hover:border-brand-400 transition shadow-sm"
                >
                  {FILTERS.find(f => f.key === activeFilter)?.label}
                  <svg className={`w-4 h-4 transition-transform ${filterOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
                {filterOpen && (
                  <>
                    <div className="fixed inset-0 z-10" onClick={() => setFilterOpen(false)} />
                    <div className="absolute right-0 mt-2 w-40 bg-white border border-brand-200 rounded-2xl shadow-lg z-20 overflow-hidden">
                      {FILTERS.map(f => (
                        <button
                          key={f.key}
                          onClick={() => { setActiveFilter(f.key); setFilterOpen(false); }}
                          className={`w-full text-left px-4 py-2.5 text-sm transition ${
                            activeFilter === f.key
                              ? 'bg-brand-50 text-brand-500 font-semibold'
                              : 'text-brand-700 hover:bg-brand-50'
                          }`}
                        >
                          {activeFilter === f.key && <span className="mr-1">✓</span>}
                          {f.label}
                        </button>
                      ))}
                    </div>
                  </>
                )}
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {sortedActiveDeals.map(deal => (
                <TimeDealCard
                  key={`${authKey}-${deal.id}`}
                  deal={deal}
                  onWishChange={() => setAuthKey(k => k + 1)}
                  // ✅ 만료 시 fetchDeals() 대신 로컬 상태만 업데이트
                  onDealExpire={() => handleDealEnd(deal.id)}
                />
              ))}
            </div>
          </section>
        )}

        {/* 오픈 예정 딜 */}
        {upcomingDeals.length > 0 && (
          <section className="mb-12">
            <div className="flex items-center gap-3 mb-6">
              <h2 className="text-xl font-bold text-brand-800">오픈 예정 딜</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {upcomingDeals.map(deal => (
                <TimeDealCard
                  key={`${authKey}-${deal.id}`}
                  deal={deal}
                  onWishChange={() => setAuthKey(k => k + 1)}
                  // ✅ 시작 시간 만료 시 UPCOMING → ACTIVE로 이동
                  onDealExpire={() => handleDealStart(deal.id)}
                />
              ))}
            </div>
          </section>
        )}

        {/* 종료된 딜 */}
        {endedDeals.length > 0 && (
          <section className="mb-12">
            <div className="flex items-center gap-3 mb-6">
              <h2 className="text-xl font-bold text-brand-400">종료된 딜</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {endedDeals.map(deal => (
                <TimeDealCard
                  key={`${authKey}-${deal.id}`}
                  deal={deal}
                />
              ))}
            </div>
          </section>
        )}

        {deals.length === 0 && (
          <div className="text-center py-20">
            <span className="text-5xl mb-4 block">🦴</span>
            <p className="text-brand-600">아직 등록된 타임딜이 없어요</p>
          </div>
        )}
      </main>

      {/* 푸터 */}
      <footer className="border-t border-brand-200 bg-white mt-8">
        <div className="container mx-auto px-4 py-8 max-w-4xl">
          <div className="text-xs text-brand-400 leading-relaxed space-y-1 text-center">
            <p>
              <span className="font-medium text-brand-500">회사명</span> (주) 뽀시래기&nbsp;&nbsp;
              <span className="font-medium text-brand-500">주소</span> 경기도 성남시 분당구 판교로 242&nbsp;&nbsp;
              <span className="font-medium text-brand-500">대표</span> 포아이&nbsp;&nbsp;
              <span className="font-medium text-brand-500">사업자등록번호</span> 107-87-83297
            </p>
            <p>
              <span className="font-medium text-brand-500">통신판매업 신고번호</span> 2026-구름-0000호&nbsp;&nbsp;
              <span className="font-medium text-brand-500">대표번호</span> 02-0000-0000&nbsp;&nbsp;
              <span className="font-medium text-brand-500">메일주소</span> support@pposiraegi.kr
            </p>
            <p className="pt-1 text-brand-300">© 2026 pposiraegi Inc.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default TimeDealList;