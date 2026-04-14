import { useState, useEffect } from 'react';

export const useCountdown = (targetTime) => {
  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft(targetTime));

  useEffect(() => {
    // 이미 만료된 경우 인터벌 시작하지 않음
    if (calculateTimeLeft(targetTime).isExpired) {
      setTimeLeft({ hours: 0, minutes: 0, seconds: 0, totalMinutes: 0, isExpired: true });
      return;
    }

    const timer = setInterval(() => {
      const next = calculateTimeLeft(targetTime);
      setTimeLeft(next);

      // ✅ 만료되면 인터벌 즉시 중지 → 이후 상태 업데이트 없음
      if (next.isExpired) {
        clearInterval(timer);
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [targetTime]);

  return timeLeft;
};

function calculateTimeLeft(targetTime) {
  const difference = new Date(targetTime) - new Date();

  if (difference <= 0) {
    return { hours: 0, minutes: 0, seconds: 0, totalMinutes: 0, isExpired: true };
  }

  return {
    hours: Math.floor((difference / (1000 * 60 * 60)) % 24),
    minutes: Math.floor((difference / (1000 * 60)) % 60),
    seconds: Math.floor((difference / 1000) % 60),
    totalMinutes: difference / (1000 * 60),
    isExpired: false,
  };
}

export const formatTime = (timeLeft) => {
  const { hours, minutes, seconds } = timeLeft;
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
};

export default useCountdown;
