import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 60000,
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.data?.message) {
      return Promise.reject(new Error(error.response.data.message));
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('Request timed out. The server may be busy.'));
    }
    return Promise.reject(new Error(error.message || 'An unexpected error occurred'));
  }
);

export default apiClient;
