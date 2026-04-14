import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getTimeDeals, createTimeDeal, updateTimeDeal, deleteTimeDeal, getCategories } from '../api/timedeal';
import { getCurrentUser, logout } from '../api/auth';
import { USE_MOCK } from '../api/config';

/* ── 상태 뱃지 색상 ── */
const STATUS_STYLE = {
  ACTIVE:   'bg-green-100 text-green-700',
  UPCOMING: 'bg-blue-100 text-blue-700',
  ENDED:    'bg-gray-100 text-gray-500',
  SOLDOUT:  'bg-red-100 text-red-600',
};
const STATUS_LABEL = {
  ACTIVE: '진행중', UPCOMING: '예정', ENDED: '종료', SOLDOUT: '품절',
};

/* ── 빈 폼 초기값 ── */
const EMPTY_FORM = {
  productName: '',
  brandName: '',
  categoryId: '',
  originalPrice: '',
  discountPrice: '',
  stock: '',
  totalStock: '',
  startTime: '',
  endTime: '',
  status: 'UPCOMING',
  images: [''],
  features: '',
  description: '',
  tags: '',
};

const AdminPage = () => {
  const navigate = useNavigate();
  const user = getCurrentUser();

  const [deals, setDeals] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState('list'); // 'list' | 'form'
  const [editTarget, setEditTarget] = useState(null); // null = 신규, deal = 수정
  const [form, setForm] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState(null); // 삭제 확인 대상 id
  const [previewDeal, setPreviewDeal] = useState(null); // 미리보기 대상
  const [toast, setToast] = useState(null); // { msg, type }
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  useEffect(() => {
    fetchDeals();
    getCategories().then(setCategories).catch(() => {});
  }, []);

  const fetchDeals = async () => {
    try {
      setLoading(true);
      const data = await getTimeDeals();
      setDeals(data);
    } catch (e) {
      showToast('딜 목록을 불러오지 못했어요.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const showToast = (msg, type = 'success') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2800);
  };

  /* ── 폼 열기 ── */
  const openNew = () => {
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setErrors({});
    setTab('form');
  };

  const openEdit = (deal) => {
    setEditTarget(deal);
    setForm({
      productName:   deal.productName || '',
      brandName:     deal.brandName || '',
      categoryId:    deal.categoryId || '',
      originalPrice: deal.originalPrice ?? '',
      discountPrice: deal.discountPrice ?? '',
      stock:         deal.stock ?? '',
      totalStock:    deal.totalStock ?? '',
      startTime:     deal.startTime ? deal.startTime.slice(0, 16) : '',
      endTime:       deal.endTime   ? deal.endTime.slice(0, 16)   : '',
      status:        deal.status || 'UPCOMING',
      images:        deal.images?.length ? deal.images : [''],
      features:      deal.features?.join('\n') || '',
      description:   deal.description || '',
      tags:          deal.tags?.join(', ') || '',
    });
    setErrors({});
    setTab('form');
  };

  /* ── 폼 핸들러 ── */
  const handleField = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    setErrors(prev => ({ ...prev, [name]: '' }));
  };

  const handleImage = (idx, value) => {
    const imgs = [...form.images];
    imgs[idx] = value;
    setForm(prev => ({ ...prev, images: imgs }));
  };

  const addImageField = () => {
    if (form.images.length >= 5) return;
    setForm(prev => ({ ...prev, images: [...prev.images, ''] }));
  };

  const removeImageField = (idx) => {
    const imgs = form.images.filter((_, i) => i !== idx);
    setForm(prev => ({ ...prev, images: imgs.length ? imgs : [''] }));
  };

  /* ── 유효성 검사 ── */
  const validate = () => {
    const e = {};
    if (!form.productName.trim())          e.productName   = '상품명을 입력해주세요.';
    if (!form.originalPrice || isNaN(form.originalPrice) || Number(form.originalPrice) <= 0)
                                           e.originalPrice = '정가를 올바르게 입력해주세요.';
    if (!form.discountPrice || isNaN(form.discountPrice) || Number(form.discountPrice) <= 0)
                                           e.discountPrice = '할인가를 올바르게 입력해주세요.';
    if (Number(form.discountPrice) >= Number(form.originalPrice))
                                           e.discountPrice = '할인가는 정가보다 낮아야 해요.';
    if (!form.stock || isNaN(form.stock) || Number(form.stock) < 0)
                                           e.stock         = '재고를 올바르게 입력해주세요.';
    if (!form.totalStock || isNaN(form.totalStock) || Number(form.totalStock) <= 0)
                                           e.totalStock    = '총 재고를 올바르게 입력해주세요.';
    if (!form.startTime)                   e.startTime     = '시작 시간을 입력해주세요.';
    if (!form.endTime)                     e.endTime       = '종료 시간을 입력해주세요.';
    if (form.startTime && form.endTime && form.startTime >= form.endTime)
                                           e.endTime       = '종료 시간은 시작 시간 이후여야 해요.';
    return e;
  };

  /* ── 저장 ── */
  const handleSave = async () => {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }

    const payload = {
      productName:   form.productName.trim(),
      brandName:     form.brandName?.trim() || '',
      categoryId:    form.categoryId || (categories[0]?.id ?? null),
      originalPrice: Number(form.originalPrice),
      discountPrice: Number(form.discountPrice),
      stock:         Number(form.stock),
      totalStock:    Number(form.totalStock),
      startTime:     new Date(form.startTime).toISOString(),
      endTime:       new Date(form.endTime).toISOString(),
      status:        form.status,
      images:        form.images.filter(Boolean),
      features:      form.features.split('\n').map(f => f.trim()).filter(Boolean),
      description:   form.description.trim(),
      tags:          form.tags.split(',').map(t => t.trim()).filter(Boolean),
    };

    try {
      setSaving(true);
      if (editTarget) {
        await updateTimeDeal(editTarget.id, payload);
        showToast('딜이 수정되었어요! ✏️');
      } else {
        await createTimeDeal(payload);
        showToast('새 딜이 등록되었어요! 🎉');
      }
      await fetchDeals();
      setTab('list');
    } catch (err) {
      showToast(err.message || '저장에 실패했어요.', 'error');
    } finally {
      setSaving(false);
    }
  };

  /* ── 삭제 ── */
  const handleDelete = async (id) => {
    try {
      await deleteTimeDeal(id);
      setDeals(prev => prev.filter(d => d.id !== id));
      showToast('딜이 삭제되었어요.', 'success');
    } catch (err) {
      showToast(err.message || '삭제에 실패했어요.', 'error');
    } finally {
      setDeleteConfirm(null);
    }
  };

  /* ── 필터링 ── */
  const filtered = deals.filter(d => {
    const matchStatus = statusFilter === 'ALL' || d.status === statusFilter;
    const matchSearch = !search || d.productName?.toLowerCase().includes(search.toLowerCase());
    return matchStatus && matchSearch;
  });

  const stats = {
    total:   deals.length,
    active:  deals.filter(d => d.status === 'ACTIVE').length,
    upcoming:deals.filter(d => d.status === 'UPCOMING').length,
    ended:   deals.filter(d => d.status === 'ENDED' || d.status === 'SOLDOUT').length,
  };

  const discountRate = (orig, disc) =>
    orig > 0 ? Math.round((1 - disc / orig) * 100) : 0;

  /* ══════════════════════════════════════════════════════
     렌더
  ══════════════════════════════════════════════════════ */
  return (
    <div className="min-h-screen bg-brand-50">

      {/* ── 헤더 ── */}
      <header className="bg-white border-b border-brand-200 sticky top-0 z-40">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between h-14">
            <div className="flex items-center gap-3">
              <Link to="/" className="flex items-center hover:opacity-75 transition">
                <img src="/icon1.png" alt="뽀시래기" className="h-8 object-contain" />
              </Link>
              <span className="text-brand-300">|</span>
              <span className="text-sm font-bold text-brand-700">관리자 콘솔</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-sm text-brand-500 hidden sm:block">👑 {user?.email}</span>
              <button
                onClick={() => { logout(); navigate('/'); }}
                className="text-xs text-brand-400 hover:text-brand-600 transition px-3 py-1.5 rounded-lg hover:bg-brand-100"
              >
                로그아웃
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* ── 토스트 ── */}
      {toast && (
        <div className={`fixed top-4 left-1/2 -translate-x-1/2 z-50 px-5 py-3 rounded-2xl shadow-lg text-sm font-medium transition
          ${toast.type === 'error' ? 'bg-red-500 text-white' : 'bg-brand-700 text-white'}`}>
          {toast.msg}
        </div>
      )}

      {/* ── 미리보기 모달 ── */}
      {previewDeal && <PreviewModal deal={previewDeal} onClose={() => setPreviewDeal(null)} />}

      {/* ── 삭제 확인 모달 ── */}
      {deleteConfirm && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center px-4">
          <div className="bg-white rounded-3xl p-6 max-w-sm w-full shadow-xl">
            <div className="text-center mb-4">
              <span className="text-4xl">🗑️</span>
            </div>
            <p className="text-center font-bold text-brand-800 mb-2">딜을 삭제할까요?</p>
            <p className="text-center text-sm text-brand-500 mb-6">
              삭제된 딜은 복구할 수 없어요.
            </p>
            <div className="flex gap-3">
              <button onClick={() => setDeleteConfirm(null)}
                className="flex-1 py-2.5 rounded-2xl border border-brand-200 text-brand-600 text-sm font-medium hover:bg-brand-50 transition">
                취소
              </button>
              <button onClick={() => handleDelete(deleteConfirm)}
                className="flex-1 py-2.5 rounded-2xl bg-red-500 text-white text-sm font-bold hover:bg-red-600 transition">
                삭제
              </button>
            </div>
          </div>
        </div>
      )}

      <main className="container mx-auto px-4 py-6 max-w-5xl">

        {/* ── 통계 카드 ── */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          {[
            { label: '전체 딜', value: stats.total, icon: '📋', color: 'bg-white' },
            { label: '진행중',  value: stats.active,   icon: '🔥', color: 'bg-green-50' },
            { label: '예정',    value: stats.upcoming, icon: '⏰', color: 'bg-blue-50' },
            { label: '종료',    value: stats.ended,    icon: '✅', color: 'bg-gray-50' },
          ].map(({ label, value, icon, color }) => (
            <div key={label} className={`${color} rounded-2xl p-4 shadow-sm`}>
              <div className="flex items-center gap-2 mb-1">
                <span className="text-lg">{icon}</span>
                <span className="text-xs text-brand-500 font-medium">{label}</span>
              </div>
              <p className="text-2xl font-bold text-brand-800">{value}</p>
            </div>
          ))}
        </div>

        {/* ── 탭 & 액션 ── */}
        <div className="flex items-center justify-between mb-4 gap-3 flex-wrap">
          <div className="flex gap-2">
            <button onClick={() => setTab('list')}
              className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${tab === 'list' ? 'bg-brand-500 text-white' : 'bg-white text-brand-600 border border-brand-200 hover:bg-brand-50'}`}>
              📋 딜 목록
            </button>
            <button onClick={openNew}
              className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${tab === 'form' && !editTarget ? 'bg-brand-500 text-white' : 'bg-white text-brand-600 border border-brand-200 hover:bg-brand-50'}`}>
              ➕ 새 딜 등록
            </button>
          </div>

          {/* 검색 + 필터 (목록 탭에서만) */}
          {tab === 'list' && (
            <div className="flex gap-2 flex-wrap">
              <input
                type="text"
                placeholder="상품명 검색"
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="px-3 py-2 rounded-xl border border-brand-200 text-sm text-brand-800 placeholder-brand-300 focus:outline-none focus:border-brand-500 transition w-40"
              />
              <select
                value={statusFilter}
                onChange={e => setStatusFilter(e.target.value)}
                className="px-3 py-2 rounded-xl border border-brand-200 text-sm text-brand-700 focus:outline-none focus:border-brand-500 transition bg-white"
              >
                <option value="ALL">전체 상태</option>
                <option value="ACTIVE">진행중</option>
                <option value="UPCOMING">예정</option>
                <option value="ENDED">종료</option>
                <option value="SOLDOUT">품절</option>
              </select>
            </div>
          )}
        </div>

        {/* ══════════════════════════════════════════════════════
            딜 목록 탭
        ══════════════════════════════════════════════════════ */}
        {tab === 'list' && (
          <>
            {loading ? (
              <div className="flex justify-center py-20">
                <div className="w-10 h-10 border-4 border-brand-200 border-t-brand-500 rounded-full animate-spin" />
              </div>
            ) : filtered.length === 0 ? (
              <div className="text-center py-20">
                <span className="text-4xl mb-3 block">🦴</span>
                <p className="text-brand-500">조건에 맞는 딜이 없어요</p>
              </div>
            ) : (
              <div className="space-y-3">
                {filtered.map(deal => (
                  <DealRow
                    key={deal.id}
                    deal={deal}
                    onEdit={() => openEdit(deal)}
                    onDelete={() => setDeleteConfirm(deal.id)}
                    onPreview={() => setPreviewDeal(deal)}
                    discountRate={discountRate(deal.originalPrice, deal.discountPrice)}
                  />
                ))}
              </div>
            )}
          </>
        )}

        {/* ══════════════════════════════════════════════════════
            등록 / 수정 폼 탭
        ══════════════════════════════════════════════════════ */}
        {tab === 'form' && (
          <div className="bg-white rounded-3xl shadow-sm p-6 space-y-6">
            <div className="flex items-center gap-2">
              <button onClick={() => setTab('list')}
                className="p-1.5 hover:bg-brand-100 rounded-xl transition">
                <svg className="w-5 h-5 text-brand-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
              </button>
              <h2 className="text-lg font-bold text-brand-800">
                {editTarget ? `딜 수정 — ${editTarget.productName?.split('|')[0].trim()}` : '새 딜 등록'}
              </h2>
            </div>

            {/* 상품명 */}
            <FormField label="상품명 *" error={errors.productName}>
              <input name="productName" value={form.productName} onChange={handleField}
                placeholder="ex) 오리지널 닭가슴살 | 강아지 간식 (파이프 분리 시 첫 줄이 타이틀로 사용)"
                className={inputCls(errors.productName)} />
            </FormField>

            {/* 브랜드명 + 카테고리 */}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="브랜드명">
                <input name="brandName" value={form.brandName} onChange={handleField}
                  placeholder="ex) 뽀시래기 프리미엄"
                  className={inputCls()} />
              </FormField>
              <FormField label="카테고리 *">
                <select name="categoryId" value={form.categoryId} onChange={handleField}
                  className="w-full px-4 py-3 rounded-2xl border border-brand-200 focus:outline-none focus:border-brand-500 text-brand-800 text-sm bg-white transition">
                  <option value="">카테고리 선택</option>
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </FormField>
            </div>

            {/* 가격 */}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="정가 (원) *" error={errors.originalPrice}>
                <input name="originalPrice" type="number" value={form.originalPrice} onChange={handleField}
                  placeholder="30000" className={inputCls(errors.originalPrice)} />
              </FormField>
              <FormField label="할인가 (원) *" error={errors.discountPrice}>
                <input name="discountPrice" type="number" value={form.discountPrice} onChange={handleField}
                  placeholder="19900" className={inputCls(errors.discountPrice)} />
                {form.originalPrice && form.discountPrice && Number(form.originalPrice) > 0 && (
                  <p className="text-xs text-brand-500 mt-1">
                    할인율: <span className="font-bold text-red-500">
                      {discountRate(Number(form.originalPrice), Number(form.discountPrice))}%
                    </span>
                  </p>
                )}
              </FormField>
            </div>

            {/* 재고
                [중요] 재고 수동 조정이 필요한 경우 RDS + Redis 둘 다 동시에 수정해야 함
                  RDS:   UPDATE products SET stock = N, "initialStock" = N WHERE id = ?;
                  Redis: redis-cli SET stock:{id} N
                  Redis만 수정 시 → DB 불일치 (Redis 재건 시 꼬임)
                  DB만 수정 시  → Redis 재고 그대로라 주문 안 들어옴
            */}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="현재 재고 *" error={errors.stock}>
                <input name="stock" type="number" value={form.stock} onChange={handleField}
                  placeholder="100" className={inputCls(errors.stock)} />
              </FormField>
              <FormField label="총 재고 (initialStock) *" error={errors.totalStock}>
                <input name="totalStock" type="number" value={form.totalStock} onChange={handleField}
                  placeholder="100" className={inputCls(errors.totalStock)} />
              </FormField>
            </div>
            {USE_MOCK && (
              <p className="text-xs text-blue-400 bg-blue-50 px-3 py-2 rounded-xl">
                [Mock Mode!] 등록 시 현재 재고 = 총 재고로 자동 설정됩니다. 운영 중 재고 수동 조정은 RDS + Redis 동시 수정 필요.
              </p>
            )}
            {Number(form.stock) > Number(form.totalStock) && form.stock && form.totalStock && (
              <p className="text-xs text-amber-500 bg-amber-50 px-3 py-2 rounded-xl">
                ⚠️ 현재 재고가 총 재고보다 많아요. 확인해주세요.
              </p>
            )}

            {/* 시간 */}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="시작 시간 *" error={errors.startTime}>
                <input name="startTime" type="datetime-local" value={form.startTime} onChange={handleField}
                  className={inputCls(errors.startTime)} />
              </FormField>
              <FormField label="종료 시간 *" error={errors.endTime}>
                <input name="endTime" type="datetime-local" value={form.endTime} onChange={handleField}
                  className={inputCls(errors.endTime)} />
              </FormField>
            </div>
            {form.startTime && form.endTime && form.startTime >= form.endTime && (
              <p className="text-xs text-red-500 bg-red-50 px-3 py-2 rounded-xl">
                ⚠️ 종료 시간이 시작 시간보다 빠르거나 같아요.
              </p>
            )}
            {form.startTime && form.endTime && form.startTime < form.endTime && new Date(form.startTime) < new Date() && (
              <p className="text-xs text-amber-500 bg-amber-50 px-3 py-2 rounded-xl">
                ⚠️ 시작 시간이 현재보다 과거예요. 의도한 설정인지 확인해주세요.
              </p>
            )}

            {/* 상태 */}
            <FormField label="딜 상태">
              <select name="status" value={form.status} onChange={handleField}
                className="w-full px-4 py-3 rounded-2xl border border-brand-200 focus:outline-none focus:border-brand-500 text-brand-800 text-sm bg-white transition">
                <option value="UPCOMING">예정 (UPCOMING)</option>
                <option value="ACTIVE">진행중 (ACTIVE)</option>
                <option value="ENDED">종료 (ENDED)</option>
                <option value="SOLDOUT">품절 (SOLDOUT)</option>
              </select>
            </FormField>

            {/* 이미지 URL */}
            <FormField label="상품 이미지 URL (최대 5개)">
              <div className="space-y-2">
                {form.images.map((img, idx) => (
                  <div key={idx} className="flex gap-2">
                    <input
                      value={img}
                      onChange={e => handleImage(idx, e.target.value)}
                      placeholder={`이미지 URL ${idx + 1}`}
                      className="flex-1 px-4 py-2.5 rounded-2xl border border-brand-200 focus:outline-none focus:border-brand-500 text-brand-800 text-sm transition"
                    />
                    {form.images.length > 1 && (
                      <button onClick={() => removeImageField(idx)}
                        className="px-3 py-2 rounded-xl bg-red-50 text-red-400 hover:bg-red-100 transition text-sm">
                        ✕
                      </button>
                    )}
                  </div>
                ))}
                {form.images.length < 5 && (
                  <button onClick={addImageField}
                    className="text-sm text-brand-400 hover:text-brand-600 transition flex items-center gap-1 mt-1">
                    <span>＋</span> 이미지 URL 추가
                  </button>
                )}
              </div>
            </FormField>

            {/* 특징 */}
            <FormField label="상품 특징 (줄바꿈으로 구분)">
              <textarea name="features" value={form.features} onChange={handleField}
                rows={4} placeholder={"ex)\n100% 유기농 인증 원료만 사용\n수의사 10인 공동 설계\n인공 방부제·착색료 無"}
                className="w-full px-4 py-3 rounded-2xl border border-brand-200 focus:outline-none focus:border-brand-500 text-brand-800 text-sm transition resize-none" />
            </FormField>

            {/* 태그 */}
            <FormField label="태그 (쉼표로 구분)">
              <input name="tags" value={form.tags} onChange={handleField}
                placeholder="ex) 강아지, 간식, 닭가슴살, 국내산"
                className={inputCls()} />
            </FormField>

            {/* 상품 설명 */}
            <FormField label="상품 설명">
              <textarea name="description" value={form.description} onChange={handleField}
                rows={4} placeholder="상품에 대한 자세한 설명을 입력하세요."
                className="w-full px-4 py-3 rounded-2xl border border-brand-200 focus:outline-none focus:border-brand-500 text-brand-800 text-sm transition resize-none" />
            </FormField>

            {/* 저장 버튼 */}
            <div className="flex gap-3 pt-2">
              <button onClick={() => setTab('list')}
                className="flex-1 py-3.5 rounded-2xl border border-brand-200 text-brand-600 text-sm font-medium hover:bg-brand-50 transition">
                취소
              </button>
              <button onClick={handleSave} disabled={saving}
                className="flex-1 py-3.5 rounded-2xl bg-brand-500 text-white text-sm font-bold hover:bg-brand-400 active:scale-[0.98] transition disabled:opacity-60">
                {saving
                  ? <span className="flex items-center justify-center gap-2">
                      <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      저장 중...
                    </span>
                  : editTarget ? '✏️ 수정 완료' : '🎉 딜 등록'}
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

/* ══════════════════════════════════════════════════════
   딜 목록 행 컴포넌트
══════════════════════════════════════════════════════ */
const DealRow = ({ deal, onEdit, onDelete, onPreview, discountRate }) => {
  const name = deal.productName?.split('|')[0]?.trim() || deal.productName;
  const stockPct = deal.totalStock > 0
    ? Math.round((deal.stock / deal.totalStock) * 100)
    : 0;

  return (
    <div className="bg-white rounded-2xl shadow-sm p-4 flex items-center gap-4 hover:shadow-md transition">
      {/* 이미지 썸네일 */}
      <div className="w-14 h-14 rounded-xl bg-brand-100 flex-shrink-0 overflow-hidden">
        {deal.images?.[0] ? (
          <img src={deal.images[0]} alt={name} className="w-full h-full object-cover" />
        ) : (
          <span className="w-full h-full flex items-center justify-center text-2xl">🐾</span>
        )}
      </div>

      {/* 정보 */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap mb-1">
          <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${STATUS_STYLE[deal.status] || 'bg-gray-100 text-gray-500'}`}>
            {STATUS_LABEL[deal.status] || deal.status}
          </span>
          <span className="text-xs text-red-500 font-bold">{discountRate}% 할인</span>
        </div>
        <p className="text-sm font-semibold text-brand-800 truncate">{name}</p>
        <div className="flex items-center gap-3 mt-1 text-xs text-brand-400 flex-wrap">
          <span>
            <span className="line-through">{deal.originalPrice?.toLocaleString()}원</span>
            {' → '}
            <span className="text-brand-700 font-bold">{deal.discountPrice?.toLocaleString()}원</span>
          </span>
          <span>재고 {deal.stock}/{deal.totalStock} ({stockPct}%)</span>
        </div>
        <div className="flex items-center gap-1 mt-1 text-xs text-brand-300">
          <span>🕐 {deal.startTime ? new Date(deal.startTime).toLocaleString('ko-KR', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : '-'}</span>
          <span>~</span>
          <span>{deal.endTime ? new Date(deal.endTime).toLocaleString('ko-KR', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : '-'}</span>
        </div>
        {/* 재고 바 */}
        <div className="mt-1.5 h-1 bg-brand-100 rounded-full overflow-hidden w-full max-w-[200px]">
          <div
            className={`h-full rounded-full transition-all ${stockPct > 30 ? 'bg-brand-400' : 'bg-red-400'}`}
            style={{ width: `${100 - stockPct}%` }}
          />
        </div>
      </div>

      {/* 버튼 */}
      <div className="flex gap-2 flex-shrink-0">
        <button onClick={onPreview}
          className="px-3 py-2 rounded-xl text-xs font-semibold bg-gray-50 text-gray-500 hover:bg-gray-100 transition">
          미리보기
        </button>
        <button onClick={onEdit}
          className="px-3 py-2 rounded-xl text-xs font-semibold bg-brand-50 text-brand-600 hover:bg-brand-100 transition">
          수정
        </button>
        <button onClick={onDelete}
          className="px-3 py-2 rounded-xl text-xs font-semibold bg-red-50 text-red-500 hover:bg-red-100 transition">
          삭제
        </button>
      </div>
    </div>
  );
};

/* ══════════════════════════════════════════════════════
   미리보기 모달
══════════════════════════════════════════════════════ */
const PreviewModal = ({ deal, onClose }) => {
  const [selectedImage, setSelectedImage] = useState(0);
  const images = deal.images?.filter(Boolean).length ? deal.images.filter(Boolean) : [deal.productImage].filter(Boolean);
  const discountRate = deal.originalPrice > 0 ? Math.round((1 - deal.discountPrice / deal.originalPrice) * 100) : 0;
  const stockPct = deal.totalStock > 0 ? Math.round((deal.stock / deal.totalStock) * 100) : 0;

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center px-4">
      <div className="bg-[#faf6f0] rounded-3xl w-full max-w-sm max-h-[90vh] overflow-y-auto shadow-2xl">
        {/* 모달 헤더 */}
        <div className="sticky top-0 bg-white/90 backdrop-blur-sm flex items-center justify-between px-4 py-3 rounded-t-3xl border-b border-brand-100 z-10">
          <span className="text-xs font-bold text-brand-500 bg-brand-50 px-2 py-1 rounded-lg">👁 고객 화면 미리보기</span>
          <button onClick={onClose} className="p-1.5 hover:bg-brand-100 rounded-xl transition">
            <svg className="w-4 h-4 text-brand-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* 이미지 갤러리 */}
        <div className="px-4 pt-4">
          <div className="relative bg-brand-100 rounded-2xl overflow-hidden mb-2">
            <div className="flex transition-transform duration-300 ease-in-out"
              style={{ transform: `translateX(-${selectedImage * 100}%)` }}>
              {images.map((img, idx) => (
                <img key={idx} src={img} alt={`${deal.productName} ${idx + 1}`}
                  className="w-full aspect-square object-cover flex-shrink-0" />
              ))}
            </div>
            <div className="absolute top-3 left-3">
              <span className="bg-brand-800 text-white px-3 py-1.5 rounded-xl font-bold text-sm shadow-lg">
                {discountRate}% SALE
              </span>
            </div>
          </div>
          {images.length > 1 && (
            <div className="flex gap-2 overflow-x-auto pb-1">
              {images.map((img, idx) => (
                <button key={idx} onClick={() => setSelectedImage(idx)}
                  className={`flex-shrink-0 w-12 h-12 rounded-xl overflow-hidden border-2 transition-all ${
                    selectedImage === idx ? 'border-brand-500' : 'border-transparent opacity-60'}`}>
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 상품 정보 */}
        <div className="bg-white rounded-2xl mx-4 mt-3 p-4 shadow-sm">
          <div className="flex items-center gap-2 mb-2">
            <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${STATUS_STYLE[deal.status] || 'bg-gray-100 text-gray-500'}`}>
              {STATUS_LABEL[deal.status] || deal.status}
            </span>
          </div>
          <h2 className="text-base font-bold text-brand-800 mb-3 leading-snug">{deal.productName}</h2>
          <div className="flex items-baseline gap-2 mb-3">
            <span className="text-2xl font-bold text-brand-800">{deal.discountPrice?.toLocaleString()}<span className="text-sm font-medium text-brand-600">원</span></span>
            <span className="text-sm text-brand-400 line-through">{deal.originalPrice?.toLocaleString()}원</span>
          </div>
          {/* 재고 바 */}
          <div className="mb-1">
            <div className="flex justify-between text-xs text-brand-500 mb-1">
              <span>{deal.totalStock - deal.stock}명 구매</span>
              <span>{stockPct}% 판매됨</span>
            </div>
            <div className="h-2 bg-brand-100 rounded-full overflow-hidden">
              <div className="h-full bg-brand-400 rounded-full transition-all" style={{ width: `${stockPct}%` }} />
            </div>
          </div>
          <div className="text-xs text-brand-400 mt-2">
            🕐 {deal.startTime ? new Date(deal.startTime).toLocaleString('ko-KR', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : '-'}
            {' ~ '}
            {deal.endTime ? new Date(deal.endTime).toLocaleString('ko-KR', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : '-'}
          </div>
        </div>

        {/* 상품 특징 */}
        {deal.features?.length > 0 && (
          <div className="bg-white rounded-2xl mx-4 mt-3 p-4 shadow-sm">
            <h3 className="font-bold text-brand-800 mb-3 text-sm">상품 정보</h3>
            <div className="text-sm text-brand-600 space-y-1.5">
              {deal.features.map((f, i) => <p key={i}>{f}</p>)}
            </div>
          </div>
        )}

        {/* 하단 여백 */}
        <div className="h-4" />
      </div>
    </div>
  );
};

/* ── 헬퍼 컴포넌트 ── */
const FormField = ({ label, error, children }) => (
  <div>
    <label className="block text-sm font-medium text-brand-700 mb-1.5">{label}</label>
    {children}
    {error && <p className="mt-1.5 text-xs text-red-500">{error}</p>}
  </div>
);

const inputCls = (error) =>
  `w-full px-4 py-3 rounded-2xl border focus:outline-none text-brand-800 placeholder-brand-300 text-sm transition
   ${error ? 'border-red-400' : 'border-brand-200 focus:border-brand-500'}`;

export default AdminPage;
