import axios from 'axios';
import type { ApiResponse, AuthSession, ShortenUrlRequest, UrlResponse } from '../types';

let csrfHeader = 'X-CSRF-TOKEN';
let csrfToken: string | null = null;

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const method = config.method?.toUpperCase();
  if (csrfToken && method && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    config.headers.set(csrfHeader, csrfToken);
  }
  return config;
});

// Response interceptor for logging
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export const urlApi = {
  shorten: async (request: ShortenUrlRequest): Promise<ApiResponse<UrlResponse>> => {
    const response = await api.post<ApiResponse<UrlResponse>>('/urls/shorten', request);
    return response.data;
  },

  getAll: async (): Promise<ApiResponse<UrlResponse[]>> => {
    const response = await api.get<ApiResponse<UrlResponse[]>>('/urls');
    return response.data;
  },

  getById: async (id: number): Promise<ApiResponse<UrlResponse>> => {
    const response = await api.get<ApiResponse<UrlResponse>>(`/urls/${id}`);
    return response.data;
  },

  getStats: async (id: number): Promise<ApiResponse<UrlResponse>> => {
    const response = await api.get<ApiResponse<UrlResponse>>(`/urls/${id}/stats`);
    return response.data;
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    const response = await api.delete<ApiResponse<void>>(`/urls/${id}`);
    return response.data;
  },
};

export const authApi = {
  session: async (): Promise<AuthSession> => {
    const response = await api.get<AuthSession>('/auth/session');
    csrfHeader = response.data.csrfHeader;
    csrfToken = response.data.csrfToken;
    return response.data;
  },

  logout: async (): Promise<void> => {
    await api.post('/logout', undefined, { baseURL: '' });
    csrfToken = null;
  },
};

export default api;
