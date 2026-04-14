// Kafka 이벤트 Mock 데이터

export const EVENT_TYPES = {
  ORDER_CREATED: { color: "blue", icon: "🔵", label: "주문 생성" },
  STOCK_RESERVED: { color: "green", icon: "🟢", label: "재고 확보" },
  PAYMENT_COMPLETED: { color: "purple", icon: "🟣", label: "결제 완료" },
  PAYMENT_FAILED: { color: "red", icon: "🔴", label: "결제 실패" },
  STOCK_ROLLBACK: { color: "orange", icon: "🟠", label: "재고 복구" },
  ORDER_COMPLETED: { color: "green", icon: "✅", label: "주문 완료" },
  ORDER_CANCELLED: { color: "red", icon: "❌", label: "주문 취소" }
};

// 성공 플로우 이벤트 생성
export const generateSuccessFlow = (orderId) => {
  const baseTime = Date.now();
  return [
    { eventType: "ORDER_CREATED", orderId, timestamp: baseTime, data: { userId: "user-123" } },
    { eventType: "STOCK_RESERVED", orderId, timestamp: baseTime + 100, data: { productId: "prod-1" } },
    { eventType: "PAYMENT_COMPLETED", orderId, timestamp: baseTime + 200, data: { amount: 25000 } },
    { eventType: "ORDER_COMPLETED", orderId, timestamp: baseTime + 300, data: {} }
  ];
};

// 실패 플로우 이벤트 생성 (Saga 보상)
export const generateFailureFlow = (orderId) => {
  const baseTime = Date.now();
  return [
    { eventType: "ORDER_CREATED", orderId, timestamp: baseTime, data: { userId: "user-456" } },
    { eventType: "STOCK_RESERVED", orderId, timestamp: baseTime + 100, data: { productId: "prod-1" } },
    { eventType: "PAYMENT_FAILED", orderId, timestamp: baseTime + 200, data: { reason: "잔액 부족" } },
    { eventType: "STOCK_ROLLBACK", orderId, timestamp: baseTime + 300, data: { restored: true } },
    { eventType: "ORDER_CANCELLED", orderId, timestamp: baseTime + 400, data: {} }
  ];
};

// 랜덤 이벤트 생성기 (모니터 페이지용)
let eventId = 0;
export const generateRandomEvent = () => {
  const orderId = `order-${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}`;
  const types = Object.keys(EVENT_TYPES);
  const randomType = types[Math.floor(Math.random() * types.length)];
  
  return {
    id: eventId++,
    eventType: randomType,
    orderId,
    timestamp: Date.now(),
    data: {}
  };
};

export default EVENT_TYPES;
