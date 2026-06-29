import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '@/types';

interface AuthState {
  isAuthenticated: boolean;
  token: string | null;
  user: User | null;
  expiresAt: number | null;
}

interface AuthActions {
  setCredentials: (token: string, user: User, expiresIn?: number) => void;
  updateUser: (user: Partial<User>) => void;
  logout: () => void;
  checkAuth: () => boolean;
}

export const useAuthStore = create<AuthState & AuthActions>()(
  persist(
    (set, get) => ({
      isAuthenticated: false,
      token: null,
      user: null,
      expiresAt: null,
      
      setCredentials: (token, user, expiresIn = 86400) => {
        set({
          isAuthenticated: true,
          token,
          user,
          expiresAt: Date.now() + expiresIn * 1000,
        });
      },
      
      updateUser: (userData) => {
        const currentUser = get().user;
        if (currentUser) {
          set({
            user: { ...currentUser, ...userData },
          });
        }
      },
      
      logout: () => {
        set({
          isAuthenticated: false,
          token: null,
          user: null,
          expiresAt: null,
        });
      },
      
      checkAuth: () => {
        const { isAuthenticated, expiresAt } = get();
        if (!isAuthenticated || !expiresAt) {
          return false;
        }
        // 检查是否过期
        if (Date.now() > expiresAt) {
          get().logout();
          return false;
        }
        return true;
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        isAuthenticated: state.isAuthenticated,
        token: state.token,
        user: state.user,
        expiresAt: state.expiresAt,
      }),
    }
  )
);