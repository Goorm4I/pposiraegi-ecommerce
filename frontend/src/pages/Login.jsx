import React, { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { login } from '../api/auth';

const Login = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const isExpired = searchParams.get('expired') === '1';
  const [form, setForm] = useState({ email: '', password: '', remember: false });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password) {
      setError('이메일과 비밀번호를 입력해주세요.');
      return;
    }
    try {
      setLoading(true);
      setError('');
      await login(form);
      navigate('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-mesh flex items-center justify-center px-4">
      <div className="w-full max-w-sm">

        {/* 로고 */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2 mb-2">
            <span className="text-3xl">🐕</span>
            <span className="text-3xl">🐈</span>
          </div>
          <Link to="/" className="text-2xl font-bold text-brand-800 hover:text-brand-500 transition">뽀시래기 타임딜</Link>
          <p className="text-brand-500 text-sm mt-1">반려동물을 위한 특별한 가격</p>
        </div>

        {/* 카드 */}
        <div className="bg-white rounded-3xl shadow-sm p-8">
          <h2 className="text-xl font-bold text-brand-800 mb-6">로그인</h2>

          {isExpired && !error && (
            <div className="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-700 text-sm">
              세션이 만료되었습니다. 다시 로그인해주세요.
            </div>
          )}

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-brand-700 mb-1.5">이메일</label>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="이메일을 입력하세요"
                className="w-full px-4 py-3 rounded-2xl border border-brand-200 focus:outline-none focus:border-brand-500 text-brand-800 placeholder-brand-300 transition"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-brand-700 mb-1.5">비밀번호</label>
              <input
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="비밀번호를 입력하세요"
                className="w-full px-4 py-3 rounded-2xl border border-brand-200 focus:outline-none focus:border-brand-500 text-brand-800 placeholder-brand-300 transition"
              />
            </div>

            {/* 로그인 상태 유지 */}
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="remember"
                name="remember"
                checked={form.remember}
                onChange={handleChange}
                className="w-4 h-4 accent-brand-500 cursor-pointer"
              />
              <label htmlFor="remember" className="text-sm text-brand-600 cursor-pointer">
                로그인 상태 유지
              </label>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 bg-brand-500 text-white rounded-2xl font-bold text-base hover:bg-brand-400 active:scale-[0.98] transition mt-2 disabled:opacity-60"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  로그인 중...
                </span>
              ) : '로그인'}
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-brand-500">
            아직 회원이 아니신가요?{' '}
            <Link to="/register" className="text-brand-500 font-bold hover:underline">
              회원가입
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
