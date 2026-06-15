/**
 * 用户API服务
 * 
 * 后端接口规范:
 * - POST /api/user/register: { username, password, phone, email? } => { code: 0, message: "success", data: userId }
 * - POST /api/user/login: { username, password } => { code: 0, message: "success", data: { token, userId, username, phone, email } }
 * - GET /api/user/{userId}: Headers: Authorization: Bearer {token} => { code: 0, message: "success", data: { userId, username, phone, email } }
 */
import {http} from '../http';
import type {
  LoginParams,
  LoginResponse,
  SendCodeParams,
  RegisterParams,
  ResetPasswordParams,
  UpdateUserParams,
  User,
} from '../../types';

// 后端登录响应类型（data部分）
interface LoginDataResponse {
  token: string;
  userId: number;
  username: string;
  phone?: string;
  email?: string;
}

// 后端用户信息响应类型
interface UserInfoResponse {
  userId: number;
  username: string;
  phone?: string;
  email?: string;
  nickname?: string;
}

// 用户资料完整响应类型 (Sprint 4 新增)
export interface UserProfileResponse {
  userId: number;
  username: string;
  phone?: string;
  email?: string;
  nickname?: string;
  avatar?: string;
  gender?: number; // 0-未知, 1-男, 2-女
  birthday?: string; // YYYY-MM-DD
  userType?: number; // 1-普通, 2-VIP, 3-企业
  userLevel?: number;
  memberLevel?: string; // 会员等级名称: "普通会员"、"黄金会员"等
  points?: number; // 积分
  totalOrderAmount?: number; // 累计消费金额
  totalOrders?: number; // 累计订单数
  createdAt?: string; // 注册时间
}

export interface UserIdentity {
  id: number;
  realName: string;
  idCardNo: string;
  maskedIdCardNo?: string;
  phone?: string;
  self?: boolean;
  defaultIdentity?: boolean;
  createdAt?: string;
}

export interface UserIdentityPayload {
  realName: string;
  idCardNo: string;
  phone?: string;
  self?: boolean;
  defaultIdentity?: boolean;
}

export const userApi = {
  /**
   * 用户名密码登录
   * @param params 登录参数 { username, password }
   * @returns 转换后的登录响应 { token, refreshToken, user }
   */
  login: async (params: LoginParams): Promise<LoginResponse> => {
    // 后端路径: /user/login (不含/api前缀，baseURL已包含)
    const response = await http.post<LoginDataResponse>('/user/login', params);
    
    // 转换后端响应为前端需要的格式
    const loginResponse: LoginResponse = {
      token: response.token,
      refreshToken: response.token, // 后端没有返回refreshToken，使用token代替
      user: {
        id: String(response.userId),
        username: response.username,
        phone: response.phone,
        email: response.email,
      },
    };
    return loginResponse;
  },

  /**
   * 用户注册
   * @param params 注册参数 { username, password, phone?, email?, nickname? }
   * @returns 注册成功后自动登录的响应
   */
  register: async (params: RegisterParams): Promise<LoginResponse> => {
    // 后端路径: /user/register
    await http.post<number>('/user/register', params);
    
    // 注册成功后自动调用登录获取token
    const loginResponse = await userApi.login({
      username: params.username,
      password: params.password,
    });
    
    return loginResponse;
  },

  /**
   * 获取用户信息
   * @param userId 用户ID
   * @returns 用户信息
   */
  getUserInfo: async (userId: number): Promise<User> => {
    // 后端路径: /user/{userId}
    const response = await http.get<UserInfoResponse>(`/user/${userId}`);
    
    const user: User = {
      id: String(response.userId),
      username: response.username,
      phone: response.phone,
      email: response.email,
      nickname: response.nickname,
    };
    return user;
  },

  /**
   * 根据用户名获取用户信息
   * @param username 用户名
   * @returns 用户信息
   */
  getUserByUsername: async (username: string): Promise<User> => {
    // 后端路径: /user/username/{username}
    const response = await http.get<UserInfoResponse>(`/user/username/${username}`);
    
    const user: User = {
      id: String(response.userId),
      username: response.username,
      phone: response.phone,
      email: response.email,
      nickname: response.nickname,
    };
    return user;
  },

  /**
   * 发送验证码（后端暂无此接口）
   * @param params { phone, type }
   */
  sendCode: (params: SendCodeParams) => {
    // 暂返回成功
    return Promise.resolve(0);
  },

  /**
   * 重置密码（后端暂无此接口）
   * @param params { phone, code, newPassword }
   */
  resetPassword: (params: ResetPasswordParams) => {
    // 暂返回成功
    return Promise.resolve(0);
  },

  /**
   * 更新用户信息（后端暂无此接口）
   * @param params 需要更新的用户信息
   */
  updateUserInfo: (params: UpdateUserParams) => {
    // 暂返回成功
    return Promise.resolve({} as User);
  },

  /**
   * 获取用户资料 (Sprint 4 新增)
   * GET /api/user/profile
   * @returns 用户完整资料
   */
  getUserProfile: async (): Promise<UserProfileResponse> => {
    const response = await http.get<UserProfileResponse>('/user/profile');
    return response;
  },

  /**
   * 更新用户资料 (Sprint 4 新增)
   * PUT /api/user/profile
   * @param params 需要更新的用户信息
   * @returns 更新后的用户资料
   */
  updateUserProfile: async (params: {
    nickname?: string;
    avatar?: string;
    email?: string;
    gender?: number;
    birthday?: string;
  }): Promise<UserProfileResponse> => {
    const response = await http.put<UserProfileResponse>('/user/profile', params);
    return response;
  },

  getIdentities: async (): Promise<UserIdentity[]> => {
    return http.get<UserIdentity[]>('/user/identities');
  },

  createIdentity: async (params: UserIdentityPayload): Promise<UserIdentity> => {
    return http.post<UserIdentity>('/user/identities', params);
  },

  updateIdentity: async (identityId: number, params: UserIdentityPayload): Promise<UserIdentity> => {
    return http.put<UserIdentity>(`/user/identities/${identityId}`, params);
  },

  deleteIdentity: async (identityId: number): Promise<void> => {
    return http.delete<void>(`/user/identities/${identityId}`);
  },

  /**
   * 退出登录（后端暂无此接口）
   */
  logout: () => {
    // 暂返回成功
    return Promise.resolve(0);
  },
};

export default userApi;
