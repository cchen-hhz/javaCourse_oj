import axios from 'axios';
import { ElMessage } from 'element-plus';

const instance = axios.create({
  baseURL: import.meta.env.PROD ? 'http://localhost:8080/api' : '/api',
  timeout: 10000,
  withCredentials: true, // Important for session cookies
});

instance.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      
      // 判断是否是登录请求，如果是登录请求导致的 401，不进行跳转，交给组件处理
      const isLoginRequest = error.config?.url?.includes('/log/login');

      if (status === 401) {
        if (!isLoginRequest) {
          ElMessage.error('未登录或登录已过期，请重新登录');
          if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
            window.location.href = '/login';
          }
        }
      } else if (status === 500) {
        if (data && data.message) {
          ElMessage.error(`服务器错误: ${data.message}`);
        } else {
          ElMessage.error('服务器内部错误');
        }
      } else {
        ElMessage.error(data.message || '请求失败');
      }
    } else {
      ElMessage.error('网络错误，请检查您的网络连接');
    }
    return Promise.reject(error);
  }
);

export default instance;
