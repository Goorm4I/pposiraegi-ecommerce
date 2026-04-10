import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser, getAddress, saveAddress, fetchAddresses } from '../api/auth';

const AddressManager = () => {
  const navigate = useNavigate();
  const user = getCurrentUser();

  const [form, setForm] = useState({ name: '', phone: '', zipcode: '', address: '', addressDetail: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [saved, setSaved] = useState(false);

  // 기존 배송지 불러오기 (localStorage 캐시 → 백엔드 갱신)
  useEffect(() => {
    if (!user) { navigate('/login'); return; }
    const cached = getAddress();
    if (cached) setForm(cached);
    // 백엔드에서 최신 배송지 가져와 캐시 갱신
    fetchAddresses().then(addr => { if (addr) setForm(addr); });

    // 카카오 주소 API 스크립트 동적 로드
    if (!document.getElementById('kakao-postcode-script')) {
      const script = document.createElement('script');
      script.id = 'kakao-postcode-script';
      script.src = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
      script.async = true;
      document.head.appendChild(script);
    }
  }, []);

  const handleAddressSearch = () => {
    new window.daum.Postcode({
      oncomplete: (data) => {
        const roadAddr = data.roadAddress || data.jibunAddress;
        setForm(prev => ({ ...prev, zipcode: data.zonecode, address: roadAddr, addressDetail: '' }));
        setErrors(prev => ({ ...prev, address: '' }));
        setTimeout(() => document.getElementById('addressDetail')?.focus(), 100);
      },
    }).open();
  };

  const validate = () => {
    const newErrors = {};
    if (!form.name) newErrors.name = '수령인 이름을 입력해주세요.';
    if (!form.phone) newErrors.phone = '연락처를 입력해주세요.';
    if (!form.zipcode || !form.address) newErrors.address = '주소 검색을 통해 주소를 입력해주세요.';
    if (!form.addressDetail) newErrors.addressDetail = '상세주소를 입력해주세요.';
    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const newErrors = validate();
    if (Object.keys(newErrors).length > 0) { setErrors(newErrors); return; }
    try {
      setLoading(true);
      await saveAddress(form);
      setSaved(true);
      setTimeout(() => navigate(-1), 1200);
    } finally {
      setLoading(false);
    }
  };

  const hasAddress = getAddress();

  return (
    <div className="min-h-screen bg-brand-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-brand-200 sticky top-0 z-40">
        <div className="container mx-auto px-4">
          <div className="flex items-center h-14">
            <button onClick={() => navigate(-1)} className="p-2 -ml-2 hover:bg-brand-100 rounded-xl transition">
              <svg className="w-6 h-6 text-brand-800" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <h1 className="ml-2 font-semibold text-brand-800">배송지 관리</h1>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        {/* 저장 완료 토스트 */}
        {saved && (
          <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-2xl flex items-center gap-3">
            <span className="text-green-500 text-xl">✓</span>
            <p className="text-green-700 font-medium text-sm">배송지가 저장되었습니다!</p>
          </div>
        )}

        {/* 현재 배송지 표시 */}
        {hasAddress && (
          <div className="bg-white rounded-2xl p-5 mb-4 shadow-sm">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-brand-500">📦</span>
              <p className="text-sm font-semibold text-brand-700">현재 등록된 배송지</p>
            </div>
            {hasAddress.name && (
              <p className="text-sm font-medium text-brand-800">{hasAddress.name} {hasAddress.phone && <span className="font-normal text-brand-500">| {hasAddress.phone}</span>}</p>
            )}
            <p className="text-sm text-brand-800 mt-0.5">
              ({hasAddress.zipcode}) {hasAddress.address}
            </p>
            <p className="text-sm text-brand-600 mt-0.5">{hasAddress.addressDetail}</p>
          </div>
        )}

        {/* 배송지 입력 폼 */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="text-base font-bold text-brand-800 mb-5">
            {hasAddress ? '배송지 변경' : '기본 배송지 등록'}
          </h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* 수령인 이름 */}
            <div>
              <label className="block text-sm font-medium text-brand-700 mb-1.5">수령인 이름</label>
              <input
                type="text"
                value={form.name}
                onChange={e => {
                  setForm(prev => ({ ...prev, name: e.target.value }));
                  setErrors(prev => ({ ...prev, name: '' }));
                }}
                placeholder="수령인 이름을 입력해주세요"
                className={`w-full px-4 py-3 rounded-2xl border focus:outline-none text-brand-800 placeholder-brand-300 transition
                  ${errors.name ? 'border-red-400' : 'border-brand-200 focus:border-brand-500'}`}
              />
              {errors.name && <p className="mt-1.5 text-xs text-red-500">{errors.name}</p>}
            </div>

            {/* 연락처 */}
            <div>
              <label className="block text-sm font-medium text-brand-700 mb-1.5">연락처</label>
              <input
                type="tel"
                value={form.phone}
                onChange={e => {
                  setForm(prev => ({ ...prev, phone: e.target.value }));
                  setErrors(prev => ({ ...prev, phone: '' }));
                }}
                placeholder="010-0000-0000"
                className={`w-full px-4 py-3 rounded-2xl border focus:outline-none text-brand-800 placeholder-brand-300 transition
                  ${errors.phone ? 'border-red-400' : 'border-brand-200 focus:border-brand-500'}`}
              />
              {errors.phone && <p className="mt-1.5 text-xs text-red-500">{errors.phone}</p>}
            </div>

            {/* 우편번호 + 검색 버튼 */}
            <div>
              <label className="block text-sm font-medium text-brand-700 mb-1.5">주소</label>
              <div className="flex gap-2 mb-2">
                <input
                  readOnly
                  value={form.zipcode}
                  placeholder="우편번호"
                  className={`w-28 px-4 py-3 rounded-2xl border bg-brand-50 text-brand-800 placeholder-brand-300 focus:outline-none cursor-default text-sm
                    ${errors.address ? 'border-red-400' : 'border-brand-200'}`}
                />
                <button
                  type="button"
                  onClick={handleAddressSearch}
                  className="flex-1 py-3 px-4 rounded-2xl border border-brand-300 text-brand-600 text-sm font-medium hover:bg-brand-50 active:scale-[0.98] transition"
                >
                  🔍 주소 검색
                </button>
              </div>
              {/* 도로명 주소 자동 입력 */}
              <input
                readOnly
                value={form.address}
                placeholder="도로명 주소 (주소 검색 후 자동 입력)"
                className="w-full px-4 py-3 rounded-2xl border border-brand-200 bg-brand-50 text-brand-800 placeholder-brand-300 focus:outline-none cursor-default text-sm"
              />
              {errors.address && <p className="mt-1.5 text-xs text-red-500">{errors.address}</p>}
            </div>

            {/* 상세주소 */}
            <div>
              <label className="block text-sm font-medium text-brand-700 mb-1.5">상세주소</label>
              <input
                id="addressDetail"
                type="text"
                value={form.addressDetail}
                onChange={e => {
                  setForm(prev => ({ ...prev, addressDetail: e.target.value }));
                  setErrors(prev => ({ ...prev, addressDetail: '' }));
                }}
                placeholder="동/호수 등 상세주소 입력"
                className={`w-full px-4 py-3 rounded-2xl border focus:outline-none text-brand-800 placeholder-brand-300 transition
                  ${errors.addressDetail ? 'border-red-400' : 'border-brand-200 focus:border-brand-500'}`}
              />
              {errors.addressDetail && <p className="mt-1.5 text-xs text-red-500">{errors.addressDetail}</p>}
            </div>

            <button
              type="submit"
              disabled={loading || saved}
              className="w-full py-3.5 bg-brand-500 text-white rounded-2xl font-bold text-base hover:bg-brand-400 active:scale-[0.98] transition disabled:opacity-60"
            >
              {loading
                ? <span className="flex items-center justify-center gap-2">
                    <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    저장 중...
                  </span>
                : saved ? '저장 완료 ✓' : '배송지 저장'}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
};

export default AddressManager;
