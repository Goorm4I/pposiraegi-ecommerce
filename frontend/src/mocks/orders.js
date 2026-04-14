// 주문 Mock 데이터

export const mockOrders = {};

let orderCounter = 1;

export const createMockOrder = (timedealId, productName, price) => {
  const orderId = `order-${String(orderCounter++).padStart(3, '0')}`;

  return {
    orderId,
    timedealId,
    productName,
    quantity: 1,
    totalPrice: price,
    status: 'PENDING',
    createdAt: new Date().toISOString(),
  };
};

// 결제 처리 (POST /api/orders/{orderId}/pay 대응)
// 백엔드는 PENDING → PAYING → PAID/FAILED 흐름
export const payMockOrder = (order) => {
  return { ...order, status: 'PAID', failReason: null };
};

export default mockOrders;
