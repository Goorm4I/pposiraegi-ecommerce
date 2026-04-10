// src/App.js

import React from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation } from 'react-router-dom';
import { getCurrentUser } from './api/auth';
import TimeDealList from './pages/TimeDealList';
import TimeDealDetail from './pages/TimeDealDetail';
import OrderResult from './pages/OrderResult';
import OrderCheckout from './pages/OrderCheckout';
import EventMonitor from './pages/EventMonitor';
import Login from './pages/Login';
import Register from './pages/Register';
import WishList from './pages/WishList';
import AddressManager from './pages/AddressManager';
import MyPage from './pages/MyPage';
import AdminPage from './pages/AdminPage';
import AdminRoute from './components/AdminRoute';
import PaymentCallback from './pages/PaymentCallback';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen">
        <Routes>
          <Route path="/" element={<TimeDealList />} />
          <Route path="/deal/:id" element={<TimeDealDetail />} />
          <Route path="/order/checkout/:id" element={<OrderCheckout />} />
          <Route path="/order/:id" element={<OrderResult />} />
          <Route path="/monitor" element={
            <AdminRoute>
              <EventMonitor />
            </AdminRoute>
          } />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/wishlist" element={<WishList />} />
          <Route path="/address" element={<AddressManager />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/payment/callback" element={<PaymentCallback />} />
          <Route path="/admin" element={
            <AdminRoute>
              <AdminPage />
            </AdminRoute>
          } />
        </Routes>
        <MonitorButton />
      </div>
    </BrowserRouter>
  );
}

const MonitorButton = () => {
  const location = useLocation();
  const user = getCurrentUser();
  const isAdmin = user?.email === 'admin@test.com';
  const hiddenPaths = ['/monitor', '/login', '/register', '/wishlist', '/address', '/mypage', '/admin'];
  const isDetailPage = location.pathname.startsWith('/deal/');
  if (!isAdmin || hiddenPaths.includes(location.pathname) || isDetailPage) return null;
  return (
    <Link
      to="/monitor"
      className="fixed bottom-6 right-6 bg-brand-800 text-white px-4 py-3 rounded-full shadow-lg hover:bg-brand-700 transition flex items-center gap-2 z-50"
    >
      <span>📊</span>
      <span className="hidden sm:inline text-sm font-medium">모니터링</span>
    </Link>
  );
};

export default App;