import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import TimeDealCard from '../components/TimeDealCard';
import { getCurrentUser } from '../api/auth';

const WishList = () => {
  const navigate = useNavigate();
  const [user] = useState(getCurrentUser());
  const [wishList, setWishList] = useState([]);
  const [authKey, setAuthKey] = useState(0);

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    loadWishList();
  }, [user]);

  const loadWishList = () => {
    const currentUser = getCurrentUser();
    if (!currentUser) return;
    try {
      const data = JSON.parse(localStorage.getItem(`wishlist_${currentUser.id}`) || '[]');
      setWishList(data);
    } catch {
      setWishList([]);
    }
  };

  return (
    <div className="min-h-screen bg-mesh">
      {/* 헤더 — 메인페이지 로고만 */}
      <header className="bg-[#faf6f0] border-b border-brand-100">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between h-16">
            <a href="/" className="flex items-center hover:opacity-75 transition">
              <img src="/icon1.png" alt="뽀시래기" className="h-10 object-contain" />
            </a>
            <a href="/mypage" className="w-10 h-10 flex items-center justify-center rounded-full hover:bg-brand-100 transition" title="마이페이지">
              <svg className="w-6 h-6 text-[#7c4a1e]" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </a>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 pb-12 pt-8">
        {/* 타이틀 */}
        <div className="mb-8">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-3xl">❤️</span>
            <h1 className="text-3xl font-bold text-brand-800">찜 목록</h1>
          </div>
          <p className="text-brand-600">{user?.name}님이 찜한 상품이에요</p>
        </div>

        {wishList.length > 0 ? (
          <section>
            <div className="flex items-center gap-2 mb-6">
              <span className="text-brand-500 font-bold text-lg">{wishList.length}개</span>
              <span className="text-brand-600">의 상품을 찜했어요</span>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {wishList.map(deal => (
                <TimeDealCard
                  key={`${authKey}-${deal.id}`}
                  deal={deal}
                  onWishChange={() => { setAuthKey(k => k + 1); loadWishList(); }}
                />
              ))}
            </div>
          </section>
        ) : (
          <div className="text-center py-20">
            <span className="text-5xl mb-4 block">🤍</span>
            <p className="text-brand-600 mb-6">아직 찜한 상품이 없어요</p>
            <button
              onClick={() => navigate('/')}
              className="px-6 py-3 bg-brand-500 text-white rounded-full font-semibold hover:bg-brand-400 transition"
            >
              타임딜 보러가기
            </button>
          </div>
        )}
      </main>
    </div>
  );
};

export default WishList;
