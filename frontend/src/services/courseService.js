import api from './api';

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  logout: (data) => api.post('/auth/logout', data),
  verifyEmail: (token) => api.post('/auth/verify-email', { token }),
  resendVerification: (email) => api.post('/auth/resend-verification', { email }),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword: (token, newPassword) => api.post('/auth/reset-password', { token, newPassword }),
};

export const courseApi = {
  getAll: (params = {}) => api.get('/courses', { params }),
  getById: (id) => api.get(`/courses/${id}`),
  getCategories: () => api.get('/courses/categories'),
  search: (keyword) => api.get('/courses/search', { params: { keyword } }),
  byCategory: (category) => api.get(`/courses/category/${category}`),
  myCourses: () => api.get('/courses/instructor/mine'),
  create: (data) => api.post('/courses', data),
  update: (id, data) => api.put(`/courses/${id}`, data),
  remove: (id) => api.delete(`/courses/${id}`),
};

export const instructorApi = {
  earnings: () => api.get('/instructor/earnings'),
};

export const adminApi = {
  stats: () => api.get('/admin/stats'),
  users: () => api.get('/admin/users'),
  updateUser: (userId, data) => api.patch(`/admin/users/${userId}`, data),
  deleteCourse: (courseId) => api.delete(`/admin/courses/${courseId}`),
  togglePublished: (courseId, published) => api.patch(`/admin/courses/${courseId}/publish`, { published }),
};

export const enrollmentApi = {
  purchase: (courseId) => api.post(`/enrollments/${courseId}/purchase`),
  myEnrollments: () => api.get('/enrollments/mine'),
  status: (courseId) => api.get(`/enrollments/${courseId}/status`),
  updateProgress: (courseId, progressPercent) =>
    api.patch(`/enrollments/${courseId}/progress`, { progressPercent }),
};

export const reviewApi = {
  getForCourse: (courseId) => api.get(`/courses/${courseId}/reviews`),
  add: (courseId, data) => api.post(`/courses/${courseId}/reviews`, data),
};

export const fileApi = {
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export const userApi = {
  me: () => api.get('/users/me'),
};
