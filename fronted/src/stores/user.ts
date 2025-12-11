import { defineStore } from 'pinia';
import axios from '../utils/axios';

interface User {
  id: number;
  username: string;
  description: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null as User | null,
    isAuthenticated: false,
  }),
  actions: {
    async fetchUser() {
      if (!localStorage.getItem('isLoggedIn')) {
        return;
      }
      try {
        const response = await axios.get('/users/me');
        this.user = response.data;
        this.isAuthenticated = true;
      } catch (error) {
        this.user = null;
        this.isAuthenticated = false;
        localStorage.removeItem('isLoggedIn');
      }
    },
    async logout() {
        // Assuming backend handles logout via cookie clearing or similar mechanism if needed, 
        // but for JWT/Session based, we might just clear local state if no specific logout endpoint exists or just call it.
        // Based on provided API, there isn't a specific logout endpoint visible in the initial scan, 
        // but usually it's good to clear state.
        this.user = null;
        this.isAuthenticated = false;
        localStorage.removeItem('isLoggedIn');
        window.location.href = '/login';
    }
  },
});
