import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import CountdownTimer from './CountdownTimer';
import StockProgress from './StockProgress';
import { getCurrentUser } from '../api/auth';

const getWishKey = () => {
  const user = getCurrentUser();
  return user ? `wishlist_${user.id}` : null;
};
const getWishList = () => {
  const key = getWishKey();
  if (!key) return [];
  try { return JSON.parse(localStorage.getItem(key) || '[]'); } catch { return []; }
};
const saveWishList = (list) => {
  const key = getWishKey();
  if (!key) return;
  localStorage.setItem(key, JSON.stringify(list));
};

const TimeDealCard = ({ deal, onWishChange, onDealExpire }) => {
  const navigate = useNavigate();
  const isLoggedIn = !!getCurrentUser();
  const [liked, setLiked] = useState(() => isLoggedIn && getWishList().some(item => item.id === deal.id));

  useEffect(() => {
    if (!isLoggedIn) setLiked(false);
  }, [isLoggedIn]);

  const {
    id, productName, productImage, originalPrice, discountPrice,
    discountRate, stock, totalStock, startTime, endTime, status
  } = deal;

  const getStatusConfig = () => {
    switch (status) {
      case 'UPCOMING': return { badge: 'bg-brand-300 text-brand-800', text: '오픈예정', cardStyle: 'opacity-90', clickable: true };
      case 'ACTIVE':   return { badge: 'bg-brand-500 text-white',     text: 'LIVE',    cardStyle: '',             clickable: true };
      case 'SOLDOUT':  return { badge: 'bg-brand-200 text-brand-600', text: '품절',    cardStyle: 'opacity-50 grayscale-[20%]', clickable: false };
      default:         return { badge: 'bg-brand-200 text-brand-600', text: '종료',    cardStyle: 'opacity-50',   clickable: false };
    }
  };

  const config = getStatusConfig();
  const isHot = status === 'ACTIVE' && (totalStock - stock) / totalStock >= 0.5;
  const isEnded = status === 'SOLDOUT' || status === 'ENDED';

  const handleClick = () => {
    if (config.clickable) navigate(`/deal/${id}`);
  };

  const handleLike = (e) => {
    e.stopPropagation();
    if (isEnded) return;
    if (!getCurrentUser()) { navigate('/login'); return; }
    const wishList = getWishList();
    const newList = liked ? wishList.filter(item => item.id !== id) : [...wishList, deal];
    saveWishList(newList);
    setLiked(!liked);
    if (onWishChange) onWishChange();
  };

  return (
    <div
      onClick={handleClick}
      className={`card overflow-hidden ${config.cardStyle} ${config.clickable ? 'cursor-pointer' : 'cursor-not-allowed'}`}
    >
      <div className="relative overflow-hidden bg-brand-100">
        <img src={productImage} alt={productName} className="w-full h-56 object-cover transition-transform duration-500 hover:scale-105" />

        {/* 좌측 상단: LIVE/오픈예정 뱃지 */}
        <div className="absolute top-4 left-4">
          <span className={`badge ${config.badge}`}>
            {status === 'ACTIVE' && <span className="inline-block w-1.5 h-1.5 bg-white rounded-full mr-1.5 animate-pulse"></span>}
            {config.text}
          </span>
        </div>

        {/* 우측 상단: HOT 뱃지 */}
        {isHot && (
          <div className="absolute top-4 right-4">
            <span className="badge bg-red-500 text-white">
              <span style={{marginRight:"3px"}}>🔥</span>HOT
            </span>
          </div>
        )}

        {/* 찜 버튼 */}
        <button
          onClick={handleLike}
          className={`absolute bottom-4 right-4 w-9 h-9 bg-white rounded-full shadow-md flex items-center justify-center transition-transform ${
            isEnded ? 'opacity-30 cursor-not-allowed' : 'active:scale-90 hover:scale-110'
          }`}
        >
          {liked ? (
            <svg className="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
          ) : (
            <svg className="w-5 h-5 text-brand-400" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
          )}
        </button>
      </div>

      <div className="p-5">
        <h3 className="font-semibold text-brand-800 mb-3 line-clamp-2 leading-snug">{productName}</h3>

        {/* ✅ 가격 영역: 줄바꿈 방지 — 가격+원가 한 줄, SALE% 별도 줄 */}
        <div className="mb-4">
          <div className="flex items-baseline gap-2 mb-1">
            <span className="text-2xl font-bold text-brand-800 whitespace-nowrap">
              {discountPrice.toLocaleString()}<span className="text-sm font-medium text-brand-600">원</span>
            </span>
            <span className="text-sm text-brand-400 line-through whitespace-nowrap">
              {originalPrice.toLocaleString()}원
            </span>
          </div>
          <span className="text-sm font-bold text-brand-500">{discountRate}% SALE</span>
        </div>

        <div className="mb-4 p-3 bg-brand-50 rounded-2xl">
          {status === 'UPCOMING' ? (
            <div className="flex items-center justify-between">
              <span className="text-sm text-brand-600">시작까지</span>
              <CountdownTimer targetTime={startTime} type="start" onExpire={onDealExpire} />
            </div>
          ) : status === 'ACTIVE' ? (
            <div className="flex items-center justify-between">
              <span className="text-sm text-brand-600">남은시간</span>
              <CountdownTimer targetTime={endTime} type="end" onExpire={onDealExpire} />
            </div>
          ) : (
            <span className="text-brand-400 text-sm">종료된 딜</span>
          )}
        </div>

        <StockProgress
          current={status === 'UPCOMING' ? 0 : totalStock - stock}
          total={totalStock}
          hideCount={status === 'UPCOMING'}
        />
      </div>
    </div>
  );
};

export default TimeDealCard;
