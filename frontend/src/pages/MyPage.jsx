import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { getCurrentUser, getAddress, saveAddress, fetchAddresses, logout } from '../api/auth';
import { getMyOrders } from '../api/order';

/* ── 결제 비밀번호는 localStorage에 유저별 저장 ── */
const getPayPw = (userId) => localStorage.getItem(`paypw_${userId}`) || null;
const savePayPw = (userId, pw) => localStorage.setItem(`paypw_${userId}`, pw);

/* ── 저장된 결제수단 ── */
const PAYMENT_METHODS = [
  { id: 'bboshi', name: '뽀시페이', desc: '잔액 500,000원', iconBg: 'bg-orange-100', iconText: 'text-orange-500' },
  { id: 'kakao',  name: '카카오페이', desc: '간편결제',       iconBg: 'bg-yellow-100', iconText: 'text-yellow-600' },
  { id: 'toss',   name: '토스페이',  desc: '간편결제',        iconBg: 'bg-blue-100',   iconText: 'text-blue-500' },
  { id: 'card',   name: '신용카드',  desc: '한도 충분',       iconBg: 'bg-gray-100',   iconText: 'text-gray-600' },
];

const MyPage = () => {
  const navigate = useNavigate();
  const user = getCurrentUser();

  useEffect(() => {
    if (!user) navigate('/login');
  }, [navigate, user]);

  const [activeSection, setActiveSection] = useState(null);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-brand-50">
      {/* 헤더 — 메인페이지 로고만 */}
      <header className="bg-[#faf6f0] border-b border-brand-100">
        <div className="container mx-auto px-4">
          <div className="flex items-center h-16">
            <Link to="/" className="flex items-center hover:opacity-75 transition">
              <img src="/icon1.png" alt="뽀시래기" className="h-10 object-contain" />
            </Link>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg space-y-4">

        {/* 프로필 카드 */}
        <div className="bg-white rounded-2xl p-5 shadow-sm flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-brand-100 flex items-center justify-center text-2xl flex-shrink-0">
            🐾
          </div>
          <div className="flex-1">
            <p className="font-bold text-brand-800 text-lg">{user.name}</p>
            <p className="text-sm text-brand-500">{user.id}</p>
          </div>
        </div>

        {/* 메뉴 리스트 */}
        <div className="bg-white rounded-2xl shadow-sm overflow-hidden divide-y divide-brand-100">

          {/* 1. 주소지 관리 */}
          <MenuItem
            icon="📦"
            label="배송지 관리"
            isOpen={activeSection === 'address'}
            onToggle={() => setActiveSection(s => s === 'address' ? null : 'address')}
          >
            <AddressSection user={user} />
          </MenuItem>

          {/* 2. 전화번호 관리 */}
          <MenuItem
            icon="📱"
            label="전화번호 관리"
            isOpen={activeSection === 'phone'}
            onToggle={() => setActiveSection(s => s === 'phone' ? null : 'phone')}
          >
            <PhoneSection user={user} />
          </MenuItem>

          {/* 3. 결제 수단 관리 */}
          <MenuItem
            icon="💳"
            label="결제 수단 관리"
            isOpen={activeSection === 'payment'}
            onToggle={() => setActiveSection(s => s === 'payment' ? null : 'payment')}
          >
            <PaymentSection user={user} />
          </MenuItem>

          {/* 4. 결제 비밀번호 */}
          <MenuItem
            icon="🔐"
            label="결제 비밀번호 설정"
            isOpen={activeSection === 'paypw'}
            onToggle={() => setActiveSection(s => s === 'paypw' ? null : 'paypw')}
          >
            <PayPasswordSection user={user} />
          </MenuItem>

          {/* 5. 주문처리현황 */}
          <MenuItem
            icon="📋"
            label="주문처리현황"
            isOpen={activeSection === 'orders'}
            onToggle={() => setActiveSection(s => s === 'orders' ? null : 'orders')}
          >
            <OrderHistorySection />
          </MenuItem>
        </div>

        {/* 로그아웃 */}
        <button
          onClick={handleLogout}
          className="w-full py-3.5 rounded-2xl border border-brand-200 text-brand-500 text-sm font-medium hover:bg-brand-50 transition"
        >
          로그아웃
        </button>
      </main>
    </div>
  );
};

/* ── 공통 메뉴 아이템 (아코디언) ── */
const MenuItem = ({ icon, label, isOpen, onToggle, children }) => (
  <div>
    <button
      onClick={onToggle}
      className="w-full flex items-center gap-3 px-5 py-4 hover:bg-brand-50 transition text-left"
    >
      <span className="text-xl">{icon}</span>
      <span className="flex-1 font-medium text-brand-800">{label}</span>
      <svg
        className={`w-4 h-4 text-brand-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`}
        fill="none" stroke="currentColor" viewBox="0 0 24 24"
      >
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
      </svg>
    </button>
    {isOpen && (
      <div className="px-5 pb-5 bg-brand-50/50 border-t border-brand-100">
        {children}
      </div>
    )}
  </div>
);

/* ── 1. 배송지 관리 ── */
const AddressSection = ({ user }) => {
  const [form, setForm] = useState(getAddress() || { zipcode: '', address: '', addressDetail: '' });
  const [errors, setErrors] = useState({});
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    fetchAddresses().then(addr => { if (addr) setForm(addr); });
    if (!document.getElementById('kakao-postcode-script')) {
      const script = document.createElement('script');
      script.id = 'kakao-postcode-script';
      script.src = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
      script.async = true;
      document.head.appendChild(script);
    }
  }, []);

  const handleSearch = () => {
    new window.daum.Postcode({
      oncomplete: (data) => {
        const roadAddr = data.roadAddress || data.jibunAddress;
        setForm(prev => ({ ...prev, zipcode: data.zonecode, address: roadAddr, addressDetail: '' }));
        setErrors(prev => ({ ...prev, address: '' }));
        setTimeout(() => document.getElementById('mp-addressDetail')?.focus(), 100);
      },
    }).open();
  };

  const handleSave = () => {
    const newErrors = {};
    if (!form.zipcode || !form.address) newErrors.address = '주소 검색을 해주세요.';
    if (!form.addressDetail) newErrors.addressDetail = '상세주소를 입력해주세요.';
    if (Object.keys(newErrors).length) { setErrors(newErrors); return; }
    saveAddress(form); // async지만 UI는 즉시 반응
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  return (
    <div className="pt-4 space-y-3">
      {form.address && (
        <div className="text-sm text-brand-600 bg-white rounded-xl p-3 border border-brand-100">
          <p className="font-medium text-brand-800">현재 배송지</p>
          <p className="mt-1">({form.zipcode}) {form.address}</p>
          <p className="text-brand-500">{form.addressDetail}</p>
        </div>
      )}
      <div className="flex gap-2">
        <input readOnly value={form.zipcode} placeholder="우편번호"
          className={`w-24 px-3 py-2.5 rounded-xl border bg-white text-sm text-brand-800 placeholder-brand-300 cursor-default ${errors.address ? 'border-red-400' : 'border-brand-200'}`} />
        <button onClick={handleSearch}
          className="flex-1 py-2.5 px-3 rounded-xl border border-brand-300 text-brand-600 text-sm font-medium hover:bg-white transition">
          🔍 주소 검색
        </button>
      </div>
      <input readOnly value={form.address} placeholder="도로명 주소"
        className="w-full px-3 py-2.5 rounded-xl border border-brand-200 bg-white text-sm text-brand-800 placeholder-brand-300 cursor-default" />
      {errors.address && <p className="text-xs text-red-500">{errors.address}</p>}
      <input id="mp-addressDetail" type="text" value={form.addressDetail}
        onChange={e => { setForm(p => ({ ...p, addressDetail: e.target.value })); setErrors(p => ({ ...p, addressDetail: '' })); }}
        placeholder="상세주소 (동/호수 등)"
        className={`w-full px-3 py-2.5 rounded-xl border text-sm text-brand-800 placeholder-brand-300 focus:outline-none focus:border-brand-500 transition ${errors.addressDetail ? 'border-red-400' : 'border-brand-200'}`} />
      {errors.addressDetail && <p className="text-xs text-red-500">{errors.addressDetail}</p>}
      <SaveButton onClick={handleSave} saved={saved} />
    </div>
  );
};

/* ── 2. 전화번호 관리 ── */
const PhoneSection = ({ user }) => {
  const [phone, setPhone] = useState(user.phone || '');
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);

const handleChange = (e) => {
    let v = e.target.value.replace(/[^0-9]/g, '');
    if (v.length <= 7) 
      v = v.length <= 3 ? v : `${v.slice(0,3)}-${v.slice(3)}`;
    else 
      v = `${v.slice(0,3)}-${v.slice(3,7)}-${v.slice(7,11)}`;
    setPhone(v);
    setError('');
  };

  const handleSave = () => {
    if (!/^01[0-9]-\d{3,4}-\d{4}$/.test(phone)) {
      setError('올바른 전화번호 형식으로 입력해주세요. (예: 010-1234-5678)');
      return;
    }
    // 세션/로컬스토리지 업데이트
    const storage = localStorage.getItem('user') ? localStorage : sessionStorage;
    const updated = { ...JSON.parse(storage.getItem('user')), phone };
    storage.setItem('user', JSON.stringify(updated));
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  return (
    <div className="pt-4 space-y-3">
      <input type="tel" value={phone} onChange={handleChange} placeholder="010-1234-5678"
        className={`w-full px-3 py-2.5 rounded-xl border text-sm text-brand-800 placeholder-brand-300 focus:outline-none focus:border-brand-500 transition ${error ? 'border-red-400' : 'border-brand-200'}`} />
      {error && <p className="text-xs text-red-500">{error}</p>}
      <SaveButton onClick={handleSave} saved={saved} />
    </div>
  );
};

/* ── 3. 결제 수단 관리 ── */
const PaymentSection = ({ user }) => {
  const storageKey = `payment_methods_${user.id}`;
  const [enabled, setEnabled] = useState(() => {
    const saved = localStorage.getItem(storageKey);
    return saved ? JSON.parse(saved) : ['bboshi', 'kakao', 'toss', 'card'];
  });
  const [saved, setSaved] = useState(false);

  const toggle = (id) => {
    setEnabled(prev =>
      prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id]
    );
  };

  const handleSave = () => {
    localStorage.setItem(storageKey, JSON.stringify(enabled));
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  return (
    <div className="pt-4 space-y-3">
      <p className="text-xs text-brand-500">결제 시 사용할 수단을 선택하세요.</p>
      {PAYMENT_METHODS.map(pm => (
        <button key={pm.id} onClick={() => toggle(pm.id)}
          className={`w-full flex items-center gap-3 p-3 rounded-xl border-2 transition ${enabled.includes(pm.id) ? 'border-brand-500 bg-brand-50' : 'border-brand-100 bg-white opacity-50'}`}>
          <div className={`w-9 h-9 rounded-xl ${pm.iconBg} flex items-center justify-center text-lg flex-shrink-0`}>
            {pm.id === 'bboshi' ? <img src="/icon.png" alt="뽀시페이" className="w-7 h-7 rounded-full object-cover" /> :
             pm.id === 'kakao'  ? <span className={`font-bold text-xs ${pm.iconText}`}>K</span> :
             pm.id === 'toss'   ? <span className={`font-bold text-xs ${pm.iconText}`}>T</span> :
             <span className={`text-sm ${pm.iconText}`}>💳</span>}
          </div>
          <div className="flex-1 text-left">
            <p className="font-semibold text-brand-800 text-sm">{pm.name}</p>
            <p className="text-xs text-brand-500">{pm.desc}</p>
          </div>
          <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition ${enabled.includes(pm.id) ? 'bg-brand-500 border-brand-500' : 'border-brand-300'}`}>
            {enabled.includes(pm.id) && (
              <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
              </svg>
            )}
          </div>
        </button>
      ))}
      <SaveButton onClick={handleSave} saved={saved} />
    </div>
  );
};

/* ── 4. 결제 비밀번호 ── */
const PayPasswordSection = ({ user }) => {
  const hasExisting = !!getPayPw(user.id);
  const [step, setStep] = useState(hasExisting ? 'verify' : 'new'); // verify | new | confirm
  const [input, setInput] = useState('');
  const [newPw, setNewPw] = useState('');
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);

  const handleVerify = () => {
    if (input !== getPayPw(user.id)) { setError('비밀번호가 일치하지 않아요.'); return; }
    setError(''); setInput(''); setStep('new');
  };

  const handleNew = () => {
    if (input.length < 4) { setError('4자리 이상 입력해주세요.'); return; }
    setNewPw(input); setError(''); setInput(''); setStep('confirm');
  };

  const handleConfirm = () => {
    if (input !== newPw) { setError('비밀번호가 일치하지 않아요.'); setInput(''); return; }
    savePayPw(user.id, newPw);
    setSaved(true);
    setTimeout(() => { setSaved(false); setStep('verify'); setInput(''); }, 2000);
  };

  const labels = {
    verify:  { title: '현재 비밀번호 확인', btn: '확인', action: handleVerify },
    new:     { title: hasExisting ? '새 결제 비밀번호 입력' : '결제 비밀번호 설정', btn: '다음', action: handleNew },
    confirm: { title: '비밀번호 한번 더 입력', btn: '저장', action: handleConfirm },
  };
  const current = labels[step];

  return (
    <div className="pt-4 space-y-3">
      {saved ? (
        <div className="p-3 bg-green-50 border border-green-200 rounded-xl text-center text-sm text-green-700 font-medium">
          ✓ 결제 비밀번호가 저장되었어요!
        </div>
      ) : (
        <>
          <p className="text-sm font-medium text-brand-700">{current.title}</p>
          <input
            type="password"
            value={input}
            onChange={e => { setInput(e.target.value); setError(''); }}
            placeholder="비밀번호 입력"
            className={`w-full px-3 py-2.5 rounded-xl border text-sm tracking-widest focus:outline-none focus:border-brand-500 transition ${error ? 'border-red-400' : 'border-brand-200'}`}
          />
          {error && <p className="text-xs text-red-500">{error}</p>}
          <button onClick={current.action}
            className="w-full py-2.5 bg-brand-500 text-white rounded-xl text-sm font-semibold hover:bg-brand-400 active:scale-[0.98] transition">
            {current.btn}
          </button>
        </>
      )}
    </div>
  );
};

/* ── 5. 주문처리현황 ── */
const ORDER_STATUS_LABEL = {
  PENDING_PAYMENT: { text: '결제 대기중', color: 'text-yellow-600 bg-yellow-50 border-yellow-200' },
  PAID:            { text: '결제 완료',   color: 'text-blue-600 bg-blue-50 border-blue-200' },
  CANCELED:        { text: '취소됨',      color: 'text-gray-500 bg-gray-50 border-gray-200' },
  PARTIAL_REFUNDED:{ text: '부분 환불',   color: 'text-orange-600 bg-orange-50 border-orange-200' },
  REFUNDED:        { text: '환불 완료',   color: 'text-orange-600 bg-orange-50 border-orange-200' },
  PAYMENT_FAILED:  { text: '결제 실패',   color: 'text-red-600 bg-red-50 border-red-200' },
};

const ITEM_STATUS_LABEL = {
  PROCESSING: '처리중',
  SHIPPED:    '배송중',
  DELIVERED:  '배송완료',
  CANCELED:   '취소됨',
};

const OrderHistorySection = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedOrderId, setExpandedOrderId] = useState(null);

  useEffect(() => {
    getMyOrders()
      .then(data => setOrders(data))
      .catch(() => setError('주문 내역을 불러오는데 실패했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="pt-4 text-center text-sm text-brand-500 py-6">불러오는 중...</div>
    );
  }

  if (error) {
    return (
      <div className="pt-4 text-center text-sm text-red-500 py-4">{error}</div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="pt-4 text-center text-sm text-brand-400 py-6">
        아직 주문 내역이 없어요.
      </div>
    );
  }

  return (
    <div className="pt-4 space-y-3">
      {orders.map(order => {
        const statusInfo = ORDER_STATUS_LABEL[order.status] || { text: order.status, color: 'text-brand-500 bg-brand-50 border-brand-200' };
        const isExpanded = expandedOrderId === order.orderId;
        const date = order.createdAt ? new Date(order.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '';

        return (
          <div key={order.orderId} className="bg-white rounded-xl border border-brand-100 overflow-hidden">
            <button
              onClick={() => setExpandedOrderId(id => id === order.orderId ? null : order.orderId)}
              className="w-full px-4 py-3 flex items-center gap-2 text-left hover:bg-brand-50 transition"
            >
              <div className="flex-1 min-w-0">
                <p className="text-xs text-brand-400">{date} · {order.orderNumber}</p>
                <p className="text-sm font-semibold text-brand-800 mt-0.5">
                  {order.items?.[0]?.productName ?? '주문 상품'}
                  {order.items?.length > 1 && ` 외 ${order.items.length - 1}건`}
                </p>
                <p className="text-sm text-brand-600 mt-0.5">
                  {Number(order.totalAmount).toLocaleString('ko-KR')}원
                </p>
              </div>
              <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full border ${statusInfo.color}`}>
                  {statusInfo.text}
                </span>
                <svg
                  className={`w-4 h-4 text-brand-400 transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}
                  fill="none" stroke="currentColor" viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </div>
            </button>

            {isExpanded && (
              <div className="border-t border-brand-100 px-4 py-3 space-y-2 bg-brand-50/40">
                {order.items?.map((item, idx) => (
                  <div key={idx} className="flex items-start justify-between gap-2 text-sm">
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-brand-800 truncate">{item.productName}</p>
                      {item.skuName && <p className="text-xs text-brand-500">{item.skuName}</p>}
                    </div>
                    <div className="text-right flex-shrink-0">
                      <p className="text-brand-700">{item.quantity}개 · {Number(item.unitPrice).toLocaleString('ko-KR')}원</p>
                      {item.status && (
                        <p className="text-xs text-brand-400 mt-0.5">
                          {ITEM_STATUS_LABEL[item.status] ?? item.status}
                        </p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

/* ── 공통 저장 버튼 ── */
const SaveButton = ({ onClick, saved }) => (
  <button onClick={onClick} disabled={saved}
    className={`w-full py-2.5 rounded-xl text-sm font-semibold transition active:scale-[0.98] ${saved ? 'bg-green-500 text-white' : 'bg-brand-500 text-white hover:bg-brand-400'}`}>
    {saved ? '✓ 저장 완료' : '저장'}
  </button>
);

export default MyPage;
