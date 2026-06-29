# 登录注册联调测试指南

## 前置条件

### 1. 后端服务
确保后端服务已启动，并运行在 `http://localhost:8080`：
```bash
# 启动后端服务
cd D:\AIProjects\BookingApi
# 根据后端项目结构启动服务
```

### 2. 前端配置
确保 React Native 项目已正确安装依赖：
```bash
cd D:\AIProjects\BookingApi\booking-app
npm install
```

## 测试用例

### 测试1: 用户注册
**步骤：**
1. 启动React Native应用
2. 在登录页点击"立即注册"
3. 填写注册信息：
   - 用户名：`testuser001`
   - 密码：`123456`
   - 确认密码：`123456`
   - 手机号（可选）：`13800138000`
   - 邮箱（可选）：`test@example.com`
4. 勾选用户协议
5. 点击"注册"按钮

**预期结果：**
- 注册成功后显示loading
- 注册完成后自动登录
- 跳转到首页（Main Tab页面）
- Token已存储到本地

**验证方法：**
1. 查看控制台是否有 `Login Success` 日志
2. 使用 AsyncStorage/MMKV 查看 `auth_token` 是否有值
3. 按返回键不应回到登录页

### 测试2: 用户登录
**步骤：**
1. 退出应用（或清除Token）
2. 重新打开应用
3. 在登录页填写已注册账号信息
4. 点击"登录"按钮

**预期结果：**
- 登录成功后跳转到首页
- Token自动存储
- 用户信息正确显示

### 测试3: 自动登录
**步骤：**
1. 登录成功后退出应用（不清理数据）
2. 重新打开应用

**预期结果：**
- 显示启动页
- 自动验证Token
- Token有效则直接进入首页
- Token失效则显示登录页

### 测试4: Token过期处理
**步骤：**
1. 手动修改本地存储的Token为一个无效值
2. 调用需要认证的API（如获取用户信息）

**预期结果：**
- API返回401错误
- 清除本地Token
- 显示错误提示

## 常见问题排查

### 问题1: 请求发送失败
**检查项：**
- [ ] 后端服务是否已启动
- [ ] 后端端口是否为8080
- [ ] baseURL配置是否正确
- [ ] Android模拟器能否访问localhost（需使用`10.0.2.2`替代）

### 问题2: 响应解析错误
**检查项：**
- [ ] 后端返回格式是否为 `{code, message, data}`
- [ ] http拦截器是否正确提取data
- [ ] 查看控制台Network面板的实际响应

### 问题3: 页面跳转失败
**检查项：**
- [ ] 导航类型是否正确配置
- [ ] 是否正确使用 `getParent()` 获取父导航器
- [ ] TypeScript类型是否匹配

## 代码修改说明

### 1. http/index.ts - 响应处理
**修改前问题：**
```typescript
// 返回的是完整的 {code, message, data} 对象
return httpClient.get(url, config).then((res) => res.data);
```

**修改后：**
```typescript
// 正确提取 data 部分
return httpClient.get<BackendResponse<T>>(url, config).then((res) => res.data.data as T);
```

### 2. 登录/注册流程
**修改内容：**
- 登录成功后调用 `navigateToMain()` 跳转到首页
- 使用 `navigation.getParent()` 获取根导航器
- 使用 `CommonActions.reset()` 重置导航栈

### 3. 错误处理
**新增的错误码处理：**
- `err.needReLogin`: 401时的特殊处理
- `err.code === 1001`: 用户名已存在

## API接口对照表

| 接口 | 方法 | 路径 | 请求体 | 响应data |
|------|------|------|--------|----------|
| 注册 | POST | /api/user/register | `{username, password, phone?, email?}` | `userId` (number) |
| 登录 | POST | /api/user/login | `{username, password}` | `{token, userId, username, phone, email}` |
| 用户信息 | GET | /api/user/{userId} | - | `{userId, username, phone, email, ...}` |

## 移动端适配说明

### Android模拟器
如果使用Android模拟器，需要修改baseURL：
```typescript
// constants/index.ts
export const AppConfig = {
  api: {
    baseURL: __DEV__ ? 'http://10.0.2.2:8080/api' : 'http://localhost:8080/api',
    // ...
  },
};
```

### iOS模拟器
iOS模拟器可以使用localhost，无需修改。

### 真机调试
真机需要使用电脑的局域网IP地址：
```typescript
export const AppConfig = {
  api: {
    baseURL: 'http://192.168.x.x:8080/api', // 替换为实际IP
    // ...
  },
};
```

## 性能优化建议

1. **避免重复登录验证**：启动时只验证一次Token
2. **Token缓存**：已登录用户的Token在有效期内不重复获取
3. **Redux持久化**：auth状态已配置持久化，快速恢复登录状态
