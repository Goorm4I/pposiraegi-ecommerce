/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // 뉴트럴 팔레트
        brand: {
          900: '#1A1A1A',  // 거의 블랙
          800: '#2C2C2C',  // 차콜 (메인 텍스트)
          700: '#4A4A4A',  // 다크그레이
          600: '#6B6B6B',  // 미디엄그레이
          500: '#8B7355',  // 웜브라운 (포인트)
          400: '#A08B6E',  // 라이트브라운
          300: '#D4A574',  // 베이지 (액센트)
          200: '#E8E4DF',  // 라이트그레이
          100: '#F5F3F0',  // 오프화이트
          50:  '#FAF8F5',  // 웜화이트 (배경)
        },
        warm: {
          brown: '#8B7355',
          beige: '#D4A574',
          cream: '#FAF8F5',
          sand: '#E8DFD4',
        }
      },
      fontFamily: {
        sans: ['Pretendard', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
      },
      boxShadow: {
        'soft': '0 2px 20px rgba(0, 0, 0, 0.04)',
        'medium': '0 4px 30px rgba(0, 0, 0, 0.06)',
        'lift': '0 12px 40px rgba(0, 0, 0, 0.08)',
      },
      backgroundImage: {
        'mesh-gradient': 'radial-gradient(at 40% 20%, rgba(212, 165, 116, 0.1) 0px, transparent 50%), radial-gradient(at 80% 0%, rgba(139, 115, 85, 0.08) 0px, transparent 50%), radial-gradient(at 0% 50%, rgba(232, 228, 223, 0.5) 0px, transparent 50%)',
      }
    },
  },
  plugins: [],
}
