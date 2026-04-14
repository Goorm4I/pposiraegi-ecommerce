import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../api/auth';

const Register = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: '', password: '', passwordConfirm: '', name: '', phone: '',
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    let { name, value } = e.target;
    if (name === 'phone') {
      value = value.replace(/[^0-9]/g, '');
      if (value.length <= 3) { /* 그대로 */ }
      else if (value.length <= 7) value = `${value.slice(0,3)}-${value.slice(3)}`;
      else value = `${value.slice(0,3)}-${value.slice(3,7)}-${value.slice(7,11)}`;
    }
    setForm(prev => ({ ...prev, [name]: value }));
    setErrors(prev => ({ ...prev, [name]: '' }));
  };

  const validate = () => {
    const newErrors = {};
    if (!form.email) newErrors.email = '이메일을 입력해주세요.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) newErrors.email = '올바른 이메일 형식으로 입력해주세요.';
    if (!form.password) newErrors.password = '비밀번호를 입력해주세요.';
    else if (form.password.length < 6) newErrors.password = '비밀번호는 6자 이상이어야 해요.';
    if (!form.passwordConfirm) newErrors.passwordConfirm = '비밀번호 확인을 입력해주세요.';
    else if (form.password !== form.passwordConfirm) newErrors.passwordConfirm = '비밀번호가 일치하지 않아요.';
    if (!form.name) newErrors.name = '이름을 입력해주세요.';
    else if (form.name.length < 2) newErrors.name = '이름은 2자 이상이어야 해요.';
    if (!form.phone) newErrors.phone = '전화번호를 입력해주세요.';
    else if (!/^01[0-9]-\d{3,4}-\d{4}$/.test(form.phone)) newErrors.phone = '올바른 전화번호 형식으로 입력해주세요. (예: 010-1234-5678)';
    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const newErrors = validate();
    if (Object.keys(newErrors).length > 0) { setErrors(newErrors); return; }
    try {
      setLoading(true);
      await register(form);
      navigate('/');
    } catch (err) {
      setErrors({ email: err.message });
    } finally {
      setLoading(false);
    }
  };

  const fields = [
    { name: 'email', label: '이메일', type: 'email', placeholder: 'example@email.com' },
    { name: 'password', label: '비밀번호', type: 'password', placeholder: '6자 이상 입력하세요' },
    { name: 'passwordConfirm', label: '비밀번호 확인', type: 'password', placeholder: '비밀번호를 다시 입력하세요' },
    { name: 'name', label: '이름', type: 'text', placeholder: '실명을 입력하세요' },
    { name: 'phone', label: '전화번호', type: 'tel', placeholder: '010-1234-5678' },
  ];

  return (
    <div className="min-h-screen bg-mesh flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2 mb-2">
            <span className="text-3xl">🐕</span>
            <span className="text-3xl">🐈</span>
          </div>
          <Link to="/" className="text-2xl font-bold text-brand-800 hover:text-brand-500 transition">뽀시래기 타임딜</Link>
          <p className="text-brand-500 text-sm mt-1">반려동물을 위한 특별한 가격</p>
        </div>
        <div className="bg-white rounded-3xl shadow-sm p-8">
          <h2 className="text-xl font-bold text-brand-800 mb-6">회원가입</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            {fields.map(({ name, label, type, placeholder }) => (
              <div key={name}>
                <label className="block text-sm font-medium text-brand-700 mb-1.5">{label}</label>
                <input
                  type={type} name={name} value={form[name]} onChange={handleChange}
                  placeholder={placeholder}
                  className={`w-full px-4 py-3 rounded-2xl border focus:outline-none text-brand-800 placeholder-brand-300 transition
                    ${errors[name] ? 'border-red-400' : 'border-brand-200 focus:border-brand-500'}`}
                />
                {errors[name] && <p className="mt-1.5 text-xs text-red-500">{errors[name]}</p>}
              </div>
            ))}
            <button type="submit" disabled={loading}
              className="w-full py-3.5 bg-brand-500 text-white rounded-2xl font-bold text-base hover:bg-brand-400 active:scale-[0.98] transition mt-2 disabled:opacity-60">
              {loading
                ? <span className="flex items-center justify-center gap-2">
                    <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    가입 중...
                  </span>
                : '가입하기'}
            </button>
          </form>
          <div className="mt-6 text-center text-sm text-brand-500">
            이미 회원이신가요?{' '}
            <Link to="/login" className="text-brand-500 font-bold hover:underline">로그인</Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
