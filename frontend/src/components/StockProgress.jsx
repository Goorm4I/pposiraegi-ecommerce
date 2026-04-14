import React from 'react';

const StockProgress = ({ current, total, hideCount = false }) => {
  const sold = current;
  const remaining = total - current;
  const percentage = total > 0 ? (sold / total) * 100 : 0;

  return (
    <div className="w-full">
      <div className="flex justify-between items-center mb-2">
        {/* 1번: 오픈예정도 ##개 남음 원복 (hideCount 무관하게 항상 표시) */}
        <span className="text-sm text-brand-600">
          {remaining === 0 ? '품절' : `${remaining}개 남음`}
        </span>
        <span className="text-xs text-brand-400">
          {/* 오픈예정이면 total/total, 아니면 sold/total */}
          {hideCount ? `${total}/${total}` : `${sold}/${total}`}
        </span>
      </div>

      <div className="w-full bg-brand-100 rounded-full h-1.5 overflow-hidden">
        <div
          className="h-full rounded-full bg-gradient-to-r from-brand-400 to-brand-500 transition-all duration-500"
          style={{ width: hideCount ? '100%' : `${percentage}%` }}
        />
      </div>
    </div>
  );
};

export default StockProgress;
