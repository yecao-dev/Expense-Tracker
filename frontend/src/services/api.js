import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' }
});

// Automatically attach JWT token to every request
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── AUTH ENDPOINTS ──

export const register = async (name, email, password) => {
  const response = await api.post('/auth/register', { name, email, password });
  localStorage.setItem('token', response.data.token);
  localStorage.setItem('userName', response.data.name);
  return response.data;
};

export const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  localStorage.setItem('token', response.data.token);
  localStorage.setItem('userName', response.data.name);
  return response.data;
};

export const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('userName');
};

export const isLoggedIn = () => !!localStorage.getItem('token');
export const getUserName = () => localStorage.getItem('userName');

// ── PLAID ENDPOINTS ──

export const createLinkToken = async () => {
  const response = await api.post('/plaid/create-link-token');
  return response.data.link_token;
};

export const exchangePublicToken = async (publicToken) => {
  const response = await api.post('/plaid/exchange-token', {
    public_token: publicToken
  });
  return response.data;
};

export const syncTransactions = async () => {
  const response = await api.post('/plaid/sync-transactions');
  return response.data;
};

// ── TRANSACTION ENDPOINTS ──

export const getTransactions = async (startDate, endDate) => {
  const response = await api.get('/transactions', {
    params: { start: startDate, end: endDate }
  });
  return response.data;
};

export const getSpendingByCategory = async (startDate, endDate) => {
  const response = await api.get('/transactions/by-category', {
    params: { start: startDate, end: endDate }
  });
  return response.data;
};

export const getMonthlyTrends = async (startDate, endDate) => {
  const response = await api.get('/transactions/monthly-trends', {
    params: { start: startDate, end: endDate }
  });
  return response.data;
};

export const getBudgets = async () => {
  const response = await api.get('/budgets');
  return response.data;
};

export const createBudget = async (category, monthlyLimit) => {
  const response = await api.post('/budgets', { category, monthlyLimit });
  return response.data;
};

export const deleteBudget = async (id) => {
  const response = await api.delete(`/budgets/${id}`);
  return response.data;
};

export const exportCsv = async (startDate, endDate) => {
  const token = localStorage.getItem('token');
  const baseUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
  const response = await fetch(
    `${baseUrl}/transactions/export?start=${startDate}&end=${endDate}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'transactions.csv';
  a.click();
  window.URL.revokeObjectURL(url);
};