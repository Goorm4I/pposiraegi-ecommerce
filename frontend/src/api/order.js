import axios from 'axios';
import { USE_MOCK, API_BASE_URL } from './config';
import { getAuthHeader } from './auth';
import { createMockOrder, payMockOrder } from '../mocks/orders';
import { mockTimeDeals } from '../mocks/timedeals';

// 주문 Mock 저장소
const mockOrderStore = {};

// ── 주문서(CheckoutSession) 생성 ─────────────────────────────────
// POST /api/v1/orders → checkoutId 발급 (멱등성 키)
export const createOrderSheet = async (skuId, quantity = 1) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 500));
    const deal = mockTimeDeals.find(d => d.id === Number(skuId));
    if (!deal) throw new Error('타임딜을 찾을 수 없습니다.');
    const checkoutId = `checkout-${skuId}-${Date.now()}`;
    return { checkoutId, totalAmount: deal.discountPrice };
  }

  // Snowflake ID는 JS Number 최대 정밀도(2^53)를 초과하므로 raw JSON string으로 전송
  const res = await axios.post(
    `${API_BASE_URL}/api/v1/orders`,
    `{"orderItems":[{"skuId":${skuId},"quantity":${quantity}}]}`,
    { headers: { ...getAuthHeader(), 'Content-Type': 'application/json' } }
  );
  const data = res.data?.data;
  if (!data?.checkoutId) {
    throw new Error('주문서 생성에 실패했습니다. 다시 시도해주세요.');
  }
  return data; // { checkoutId, products, totalAmount, shippingAddress }
};

// ── 주문 확정 ─────────────────────────────────────────────────────
// POST /api/v1/orders/submit
// 멱등성: checkoutId가 DB UNIQUE 제약으로 중복 차단됨
export const submitOrder = async (checkoutId, shippingAddressId, paymentMethod, pgImpUid) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 800));
    return { orderId: checkoutId };
  }

  // Snowflake ID 정밀도 손실 방지: raw JSON string으로 전송
  const addressPart = shippingAddressId != null ? shippingAddressId : 'null';
  const res = await axios.post(
    `${API_BASE_URL}/api/v1/orders/submit`,
    `{"checkoutId":${checkoutId},"shippingAddressId":${addressPart},"paymentMethod":"${paymentMethod}","pgImpUid":"${pgImpUid}"}`,
    { headers: { ...getAuthHeader(), 'Content-Type': 'application/json', 'Idempotency-Key': String(checkoutId) } }
  );
  return res.data.data; // { orderNumber, orderName, amount, pgConfig }
};

// ── 레거시 (mock 전용) ────────────────────────────────────────────
export const createOrder = async (timedealId, quantity = 1) => {
  await new Promise(resolve => setTimeout(resolve, 500));
  const deal = mockTimeDeals.find(d => d.id === Number(timedealId));
  if (!deal) throw new Error('타임딜을 찾을 수 없습니다.');
  const order = createMockOrder(timedealId, deal.productName, deal.discountPrice);
  mockOrderStore[order.orderId] = order;
  return { orderId: order.orderId, status: order.status };
};

export const payOrder = async (orderId, paymentMethod, pgResponse) => {
  await new Promise(resolve => setTimeout(resolve, 800));
  const order = mockOrderStore[orderId];
  if (!order) throw new Error('주문을 찾을 수 없습니다.');
  const paid = payMockOrder(order);
  mockOrderStore[orderId] = paid;
  return paid;
};

export const getOrder = async (checkoutId) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 200));
    const order = mockOrderStore[checkoutId];
    if (!order) throw new Error('주문을 찾을 수 없습니다.');
    return order;
  }

  const res = await axios.get(
    `${API_BASE_URL}/api/v1/orders/${checkoutId}`,
    { headers: getAuthHeader() }
  );
  return res.data?.data ?? null;
};

// ── 내 주문 내역 조회 ─────────────────────────────────────────────
// GET /api/v1/orders/my
export const getMyOrders = async () => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300));
    return [];
  }

  const res = await axios.get(
    `${API_BASE_URL}/api/v1/orders/my`,
    { headers: getAuthHeader() }
  );
  return res.data?.data ?? [];
};

export default { createOrderSheet, submitOrder, createOrder, payOrder, getOrder, getMyOrders };
