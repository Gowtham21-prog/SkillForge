import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../services/courseService';

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const token = localStorage.getItem('token');
    if (storedUser && token) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const res = await authApi.login({ email, password });
    const { token, refreshToken, id, name, email: userEmail, role } = res.data;
    const userData = { id, name, email: userEmail, role };
    localStorage.setItem('token', token);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const register = async (name, email, password, role) => {
    const res = await authApi.register({ name, email, password, role });
    const { token, refreshToken, id, name: userName, email: userEmail, role: userRole } = res.data;
    const userData = { id, name: userName, email: userEmail, role: userRole };
    localStorage.setItem('token', token);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const logout = () => {
    const refreshToken = localStorage.getItem('refreshToken');
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
    if (refreshToken) {
      // Best-effort server-side revoke; ignore failures since we've already logged out locally
      authApi.logout({ refreshToken }).catch(() => {});
    }
  };

  return (
    <AuthContext.Provider value={{
      user, loading, login, register, logout,
      isInstructor: user?.role === 'INSTRUCTOR' || user?.role === 'ADMIN',
      isAdmin: user?.role === 'ADMIN',
    }}>
      {children}
    </AuthContext.Provider>
  );
};
