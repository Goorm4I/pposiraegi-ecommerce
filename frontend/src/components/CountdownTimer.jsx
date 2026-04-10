import React, { useEffect, useRef } from 'react';
import { useCountdown } from '../hooks/useCountdown';

const CountdownTimer = ({ targetTime, type = "end", onExpire }) => {
  const timeLeft = useCountdown(targetTime);
  const expiredCalled = useRef(false);
  // ✅ onExpire를 ref로 관리 → 매 렌더마다 새 함수가 와도 useEffect가 재실행되지 않음
  const onExpireRef = useRef(onExpire);
  useEffect(() => { onExpireRef.current = onExpire; }, [onExpire]);

  useEffect(() => {
    if (timeLeft.isExpired && !expiredCalled.current) {
      expiredCalled.current = true;
      onExpireRef.current?.();
    }
  }, [timeLeft.isExpired]); // ✅ onExpire 의존성 제거

  if (timeLeft.isExpired) {
    return (
      <span className="text-brand-400 text-sm">
        {type === "start" ? "시작됨" : "종료"}
      </span>
    );
  }

  const { hours, minutes, seconds, totalMinutes } = timeLeft;
  const isUrgent = totalMinutes <= 15;

  return (
    <div className="flex items-center gap-1 font-mono">
      <TimeBlock value={hours} urgent={isUrgent} />
      <span className={`font-bold ${isUrgent ? 'text-red-400' : 'text-brand-400'}`}>:</span>
      <TimeBlock value={minutes} urgent={isUrgent} />
      <span className={`font-bold ${isUrgent ? 'text-red-400' : 'text-brand-400'}`}>:</span>
      <TimeBlock value={seconds} urgent={isUrgent} />
    </div>
  );
};

const TimeBlock = ({ value, urgent }) => (
  <span className={`font-bold text-lg ${urgent ? 'text-red-500' : 'text-brand-800'}`}>
    {String(value).padStart(2, '0')}
  </span>
);

export default CountdownTimer;
