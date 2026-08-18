import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, instructorOnly = false, adminOnly = false }) {
  const { user, loading, isInstructor, isAdmin } = useAuth();

  if (loading) return null;

  if (!user) return <Navigate to="/login" replace />;
  if (adminOnly && !isAdmin) return <Navigate to="/" replace />;
  if (instructorOnly && !isInstructor) return <Navigate to="/dashboard" replace />;

  return children;
}
