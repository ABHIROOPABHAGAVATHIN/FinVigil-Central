import axios from 'axios';

const ML_BASE_URL = import.meta.env.VITE_ML_API_BASE_URL || 'http://localhost:8000';

export const mlClient = axios.create({
  baseURL: ML_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});
