import React from 'react';
import { Navigate } from 'react-router-dom';
import { getCurrentUser } from '../api/auth';

const ADMIN_EMAILS = (
  process.env.REACT_APP_ADMIN_EMAILS || 'admin@test.com'
).split(',').map(e => e.trim().toLowerCase());

const AdminRoute = ({ children }) => {
  const user = getCurrentUser();
  const isAdmin = user?.email && ADMIN_EMAILS.includes(user.email.toLowerCase());

  if (!isAdmin) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default AdminRoute;