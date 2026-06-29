/**
 * 用户类型定义
 */
export interface User {
  id: string;
  username: string;
  phone?: string;
  email?: string;
  nickname?: string;
  avatar?: string;
  gender?: 'male' | 'female' | 'unknown';
  birthday?: string;
  memberLevel?: string;
  points?: number;
  createTime?: string;
}

export interface LoginParams {
  username: string;
  password: string;
}

export interface LoginResponse {
  user: User;
  token: string;
  refreshToken: string;
}

export interface SendCodeParams {
  phone: string;
  type: 'login' | 'register' | 'resetPassword';
}

export interface RegisterParams {
  username: string;
  password: string;
  phone?: string;
  email?: string;
  nickname?: string;
}

export interface ResetPasswordParams {
  phone: string;
  code: string;
  newPassword: string;
}

export interface UpdateUserParams {
  nickname?: string;
  avatar?: string;
  email?: string;
  gender?: 'male' | 'female' | 'unknown';
  birthday?: string;
}
