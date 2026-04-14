// src/api/auth.js

import axios from 'axios';
import { USE_MOCK, API_BASE_URL } from './config';

// 목업 유저 데이터
const MOCK_USERS = [
  { id: 'admin', email: 'admin@test.com', password: '123456', name: '관리자' },
  { id: 'test', email: 'test@test.com', password: '1234', name: '테스트유저' },
];

// 로그인
export const login = async ({ email, password, remember }) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 500));
    const user = MOCK_USERS.find(u => u.email === email && u.password === password);
    if (!user) throw new Error('이메일 또는 비밀번호가 올바르지 않아요.');
    const userData = { id: user.id, name: user.name, email: user.email };
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem('user', JSON.stringify(userData));
    return userData;
  }

  const response = await axios.post(`${API_BASE_URL}/api/v1/auth/login`, { email, password });
  const { accessToken, refreshToken } = response.data.data;
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
  // JWT subject에서 userId 추출 (백엔드가 sub: userId로 발급)
  const payload = JSON.parse(atob(accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
  const userData = { id: payload.sub, email };
  const storage = remember ? localStorage : sessionStorage;
  storage.setItem('user', JSON.stringify(userData));
  return userData;
};

// 회원가입
export const register = async ({ email, password, name, phone }) => {
  if (USE_MOCK) {
    await new Promise(resolve => setTimeout(resolve, 500));
    const exists = MOCK_USERS.find(u => u.email === email);
    if (exists) throw new Error('이미 사용 중인 이메일이에요.');
    MOCK_USERS.push({ id: email, email, password, name, phone });
    const userData = { id: email, email, name };
    sessionStorage.setItem('user', JSON.stringify(userData));
    return userData;
  }

  await axios.post(`${API_BASE_URL}/api/v1/users`, { email, password, name, phoneNumber: phone });
  // 가입 후 자동 로그인: JWT 토큰 발급 + user 저장
  const loginRes = await axios.post(`${API_BASE_URL}/api/v1/auth/login`, { email, password });
  const { accessToken, refreshToken } = loginRes.data.data;
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
  const payload = JSON.parse(atob(accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
  const userData = { id: payload.sub, email, name };
  sessionStorage.setItem('user', JSON.stringify(userData));
  return userData;
};

// 백엔드 배송지 필드 → 프론트 형식 변환
const mapAddress = (addr) => ({
  id: addr.id,
  name: addr.recipientName,
  phone: addr.phoneNumber,
  zipcode: addr.zipCode,
  address: addr.baseAddress,
  addressDetail: addr.detailAddress,
  isDefault: addr.isDefault,
});

// 배송지 저장 — 백엔드 API + localStorage 캐시
export const saveAddress = async ({ name, phone, zipcode, address, addressDetail }) => {
  const user = getCurrentUser();
  if (!user) return;
  const addressData = { name, phone, zipcode, address, addressDetail };

  if (!USE_MOCK) {
    try {
      await axios.post(`${API_BASE_URL}/api/v1/users/me/addresses`, {
        recipientName: name,
        phoneNumber: phone,
        zipCode: zipcode,
        baseAddress: address,
        detailAddress: addressDetail,
        isDefault: true,
      }, { headers: getAuthHeader() });
    } catch (_) {
      // 백엔드 실패 시 localStorage fallback
    }
  }

  localStorage.setItem(`address_${user.id}`, JSON.stringify(addressData));
  return addressData;
};

// 배송지 조회 — localStorage 캐시 (동기, 즉시 반환)
export const getAddress = () => {
  const user = getCurrentUser();
  if (!user) return null;
  const data = localStorage.getItem(`address_${user.id}`);
  return data ? JSON.parse(data) : null;
};

// 배송지 목록 백엔드에서 조회 → localStorage 캐시 갱신 (비동기)
export const fetchAddresses = async () => {
  const user = getCurrentUser();
  if (!user || USE_MOCK) return null;
  try {
    const res = await axios.get(`${API_BASE_URL}/api/v1/users/me/addresses`, { headers: getAuthHeader() });
    const list = res.data.data ?? [];
    if (list.length > 0) {
      const addr = list.find(a => a.isDefault) ?? list[0];
      const mapped = mapAddress(addr);
      localStorage.setItem(`address_${user.id}`, JSON.stringify(mapped));
      return mapped;
    }
  } catch (_) {}
  return null;
};

// 로그아웃
export const logout = async () => {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!USE_MOCK && refreshToken) {
    try {
      await axios.post(`${API_BASE_URL}/api/v1/auth/logout`, { refreshToken },
        { headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` } }
      );
    } catch (_) {}
  }
  localStorage.removeItem('user');
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  sessionStorage.removeItem('user');
};

// API 요청용 Authorization 헤더
export const getAuthHeader = () => {
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

// 현재 로그인된 유저 가져오기
export const getCurrentUser = () => {
  const data = localStorage.getItem('user') || sessionStorage.getItem('user');
  return data ? JSON.parse(data) : null;
};

// 401 인터셉터: 토큰 만료 시 자동 로그아웃 + 로그인 페이지 이동
axios.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      sessionStorage.removeItem('user');
      // 현재 페이지가 /login이 아닐 때만 리다이렉트
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login?expired=1';
      }
    }
    return Promise.reject(err);
  }
);

export default { login, register, logout, getCurrentUser, saveAddress, getAddress };
