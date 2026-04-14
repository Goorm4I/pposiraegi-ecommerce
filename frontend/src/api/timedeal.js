import axios from 'axios';
import { USE_MOCK, API_BASE_URL } from './config';
import { mockTimeDeals } from '../mocks/timedeals';
import { getAuthHeader } from './auth';

// Mock 데이터를 런타임에 수정할 수 있도록 별도 배열로 관리
let mockData = [...mockTimeDeals];
let nextId = Math.max(...mockTimeDeals.map(d => d.id)) + 1;

// 백엔드 응답 → 프론트 형식 변환
const mapDeal = (deal) => {
  const hasSkus = (deal.product?.skus?.length ?? 0) > 0;
  const skuTotalStock = hasSkus
    ? deal.product.skus.reduce((sum, s) => sum + (s.stockQuantity ?? 0), 0)
    : null;
  const stock = skuTotalStock ?? deal.remainingQuantity ?? deal.totalQuantity ?? 0;
  const totalStock = deal.dealQuantity ?? deal.totalQuantity ?? 0;

  // 백엔드 상태 → 프론트 상태 정규화
  // PENDING → UPCOMING, EXPIRED → ENDED
  // ACTIVE + SKU 재고 0 → SOLDOUT (백엔드가 아직 상태 갱신 안 한 경우)
  const rawStatus = deal.status === 'PENDING' ? 'UPCOMING'
                  : deal.status === 'EXPIRED' ? 'ENDED'
                  : deal.status;
  const status = (rawStatus === 'ACTIVE' && hasSkus && skuTotalStock === 0)
    ? 'SOLDOUT'
    : rawStatus;

  return {
  id: deal.timeDealId ?? deal.id,  // 백엔드 필드명: timeDealId
  skuId: deal.product?.skus?.find(s => s.status === 'AVAILABLE')?.skuId
      ?? deal.product?.skus?.[0]?.skuId,
  productName: deal.product?.name ?? '상품명 없음',
  productImage: deal.product?.thumbnailUrl ?? `https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=800&h=800&fit=crop&q=80`,
  originalPrice: parseFloat(deal.product?.originPrice) || 0,
  discountPrice: parseFloat(deal.product?.salePrice ?? deal.product?.originPrice) || 0,
  discountRate: parseFloat(deal.product?.originPrice) > 0
    ? Math.round((1 - parseFloat(deal.product?.salePrice ?? deal.product?.originPrice) / parseFloat(deal.product?.originPrice)) * 100)
    : 0,
  stock,
  totalStock,
  status,
  startTime: deal.startTime,
  endTime: deal.endTime,
  brandName: deal.product?.brandName ?? '',
  description: deal.product?.description ?? '',
  };
};

// 타임딜 목록 조회
export const getTimeDeals = async () => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300));
    return [...mockData];
  }

  const response = await axios.get(`${API_BASE_URL}/api/v1/time-deals`);
  return (response.data.data ?? []).map(mapDeal);
};

// 타임딜 상세 조회 — 단건 API 사용 (SKU 정보 포함)
export const getTimeDeal = async (id) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 200));
    const deal = mockData.find(d => d.id === Number(id));
    if (!deal) throw new Error('타임딜을 찾을 수 없습니다.');
    return deal;
  }

  const response = await axios.get(`${API_BASE_URL}/api/v1/time-deals/${id}`);
  const deal = response.data.data;
  if (!deal) throw new Error('타임딜을 찾을 수 없습니다.');

  // detail 응답에는 remainingQuantity/dealQuantity 없음 → 목록에서 보완
  const listRes = await axios.get(`${API_BASE_URL}/api/v1/time-deals`);
  const listDeal = (listRes.data.data ?? []).find(d => String(d.timeDealId) === String(id));

  const merged = {
    ...deal,
    ...(listDeal ?? {}),
    product: {
      ...(listDeal?.product ?? {}),  // 가격 (originPrice, salePrice)
      ...(deal.product ?? {}),       // SKU, 이미지, 옵션 (우선)
    },
  };

  return mapDeal(merged);
};

// 카테고리 목록 조회 (어드민 폼에서 사용)
export const getCategories = async () => {
  if (USE_MOCK) return [{ id: 1, name: '반려견용품' }, { id: 2, name: '반려묘용품' }, { id: 3, name: '공용용품' }];
  const response = await axios.get(`${API_BASE_URL}/api/v1/categories`);
  return (response.data.data ?? []).map(c => ({ id: c.id, name: c.name }));
};

// 타임딜 등록
// 백엔드: POST /api/v1/admin/time-deals/with-product
export const createTimeDeal = async (payload) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300));
    const newDeal = { ...payload, id: nextId++ };
    mockData.push(newDeal);
    return newDeal;
  }

  // AdminPage 폼 → 백엔드 CreateRequestWithProduct 형식 변환
  const backendPayload = {
    product: {
      categoryId: payload.categoryId,
      name: payload.productName,
      description: payload.description || '',
      brandName: payload.brandName || '',
      originPrice: payload.originalPrice,
      salePrice: payload.discountPrice,
      status: 'FOR_SALE',
      images: (payload.images || []).filter(Boolean).map((url, idx) => ({
        imageUrl: url,
        imageType: idx === 0 ? 'THUMBNAIL' : 'DETAIL',
        displayOrder: idx + 1,
      })),
      optionGroups: [],
      skus: [{
        skuCode: `SKU-${Date.now()}`,
        status: 'AVAILABLE',
        additionalPrice: 0,
        stockQuantity: payload.totalStock,
        selectedOptionValues: [],
      }],
    },
    dealQuantity: payload.totalStock,
    startTime: payload.startTime,
    endTime: payload.endTime,
  };

  const response = await axios.post(
    `${API_BASE_URL}/api/v1/admin/time-deals/with-product`,
    backendPayload,
    { headers: getAuthHeader() }
  );
  return response.data.data;
};

// 타임딜 수정 (스케줄/재고 변경)
// 백엔드: PUT /api/v1/admin/time-deals/{id}
export const updateTimeDeal = async (id, payload) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300));
    const idx = mockData.findIndex(d => d.id === Number(id));
    if (idx === -1) throw new Error('타임딜을 찾을 수 없습니다.');
    mockData[idx] = { ...mockData[idx], ...payload };
    return mockData[idx];
  }

  const backendPayload = {
    startTime: payload.startTime,
    endTime: payload.endTime,
    dealQuantity: payload.totalStock ?? payload.stock,
  };

  const response = await axios.put(
    `${API_BASE_URL}/api/v1/admin/time-deals/${id}`,
    backendPayload,
    { headers: getAuthHeader() }
  );
  return response.data.data;
};

// 타임딜 삭제 (백엔드 삭제 엔드포인트 없음)
export const deleteTimeDeal = async (id) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 300));
    const idx = mockData.findIndex(d => d.id === Number(id));
    if (idx === -1) throw new Error('타임딜을 찾을 수 없습니다.');
    mockData.splice(idx, 1);
    return { success: true };
  }

  throw new Error('백엔드에서 타임딜 삭제 API를 지원하지 않습니다.');
};

export default { getTimeDeals, getTimeDeal, createTimeDeal, updateTimeDeal, deleteTimeDeal };