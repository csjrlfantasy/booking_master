# 票多星TicketMore - Sprint 1 Code Review检查清单

## 文档信息

| 项目名称 | 票多星TicketMore |
|---------|-----------------|
| Sprint | Sprint 1 |
| 审查阶段 | 开发完成Review |
| 技术栈 | 后端: Spring Boot 3.2.x + Java 17<br/>前端: React Native + TypeScript |

---

## 目录

- [一、后端代码规范检查](#一后端代码规范检查)
- [二、后端安全检查](#二后端安全检查)
- [三、后端性能检查](#三后端性能检查)
- [四、后端最佳实践检查](#四后端最佳实践检查)
- [五、后端专项检查清单](#五后端专项检查清单)
- [六、前端代码规范检查](#六前端代码规范检查)
- [七、前端安全检查](#七前端安全检查)
- [八、前端性能检查](#八前端性能检查)
- [九、前端最佳实践检查](#九前端最佳实践检查)
- [十、检查结果评分](#十检查结果评分)

## 一、后端代码规范检查

### 1.1 命名规范

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 类名 | 使用PascalCase，如 `UserService`, `TicketController` | 必检 |
| 方法名 | 使用camelCase，如 `getUserById()`, `createOrder()` | 必检 |
| 变量名 | 使用camelCase，禁用单字符命名(循环变量除外) | 必检 |
| 常量名 | 使用UPPER_SNAKE_CASE，如 `MAX_RETRY_COUNT` | 必检 |
| 包名 | 全部小写，层级分明，如 `com.ticket.app.controller.user` | 必检 |
| 接口名 | 使用I前缀或Service后缀，如 `IUserService` 或 `UserService` | 建议 |

**命名示例：**
```java
// ✅ 正确
public class UserServiceImpl implements UserService {
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private UserRepository userRepository;
    
    public User getUserById(Long userId) { ... }
}

// ❌ 错误
public class userservice {  // 类名
    private int a;  // 无意义变量
    private userRepository b;  // 变量名无意义
}
```

### 1.2 代码格式

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 缩进 | 使用4空格缩进，禁止Tab缩进 | 必检 |
| 行长度 | 单行不超过120字符 | 建议 |
| 空行 | 同一类成员之间使用空行分隔 | 必检 |
| 大括号 | 新行开始大括号，闭合大括号与对应开始对齐 | 必检 |
| 导包 | 按顺序：java, javax, spring, 其他，按字母排序 | 建议 |
| 不需要的导包 | 禁止存在未使用的import语句 | 必检 |

**格式化配置检查：**
```xml
<!-- pom.xml中建议配置 -->
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <java.version>17</java.version>
    <!-- Google Java Style Guide -->
    <google-java-format.version>1.17.0</google-java-format.version>
</properties>
```

### 1.3 注释规范

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 类注释 | 类必须包含Javadoc注释，说明职责 | 必检 |
| 公共方法注释 | 公开API必须包含Javadoc，说明参数和返回值 | 必检 |
| 重要逻辑注释 | 复杂业务逻辑必须添加行内注释 | 必检 |
| TODO注释 | 未完成代码使用TODO标记，标注负责人 | 建议 |
| 禁用注释代码 | 禁止保留注释掉的代码块 | 必检 |

**注释示例：**
```java
/**
 * 用户服务实现类
 * 负责用户注册、登录、信息管理等业务逻辑
 *
 * @author developer
 * @since 2026-04-09
 */
@Service
public class UserServiceImpl implements UserService {
    
    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，不存在返回null
     */
    public User getUserById(Long userId) { ... }
}
```

---

## 二、后端安全检查

### 2.1 密码安全

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 密码存储 | 必须使用BCrypt或更强算法加密，禁止明文存储 | 必检 |
| 密码传输 | 密码不允许在URL参数中传输，必须使用POST Body | 必检 |
| 密码校验 | 密码强度校验(长度、复杂度) | 必检 |
| 忘记密码 | 必须使用验证码方式，禁止直接返回明文密码 | 必检 |

**检查代码示例：**
```java
// ✅ 正确
@PostMapping("/register")
public Result<?> register(@RequestBody @Valid RegisterDTO dto) {
    // BCrypt加密
    String encryptedPassword = passwordEncoder.encode(dto.getPassword());
    user.setPassword(encryptedPassword);
    userRepository.save(user);
}

// ❌ 错误
@PostMapping("/register")
public Result<?> register(String username, String password) {
    user.setPassword(password);  // 明文存储
}
```

### 2.2 SQL注入防护

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 参数化查询 | 必须使用PreparedStatement或MyBatis #{}语法 | 必检 |
| 禁止字符串拼接 | 禁止使用字符串拼接SQL语句 | 必检 |
| _LIMIT参数 | 分页参数必须转为Integer类型并校验范围 | 必检 |
| 敏感字符过滤 | 对LIKE语句的特殊字符进行转义 | 建议 |

**检查代码示例：**
```java
// ✅ 正确 - MyBatis
@Select("SELECT * FROM user WHERE username = #{username}")
User findByUsername(@Param("username") String username);

// ❌ 错误 - 字符串拼接
@Select("SELECT * FROM user WHERE username = '" + username + "'")
```

### 2.3 XSS防护

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 输入校验 | 对用户输入进行长度和格式校验 | 必检 |
| 输出转义 | 响应HTML内容必须进行转义 | 必检 |
| 富文本 | 富文本内容使用白名单过滤 | 必检 |
| 文件名 | 上传文件名必须过滤特殊字符 | 必检 |

### 2.4 JWT Token安全

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 签名算法 | 使用HS256或更强的非对称算法 | 必检 |
| 密钥管理 | 密钥不能硬编码，必须从配置中心获取 | 必检 |
| 过期时间 | Token过期时间不超过24小时 | 必检 |
| 敏感信息 | Token中不能存储敏感信息 | 必检 |
| 刷新机制 | 实现Token刷新机制 | 必检 |

**检查配置：**
```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:}  # 从环境变量获取
  expiration: 86400000   # 24小时
  algorithm: HS256
```

---

## 三、后端性能检查

### 3.1 数据库索引

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 主键索引 | 每张表必须有主键 | 必检 |
| 联合索引 | WHERE条件中的字段必须有对应索引 | 必检 |
| 索引顺序 | 复合索引字段顺序与查询条件匹配 | 必检 |
| 避免全表扫描 | 检查是否有不必要的全表扫描 | 必检 |
| 覆盖索引 | 考虑使用覆盖索引减少回表 | 建议 |

**检查示例：**
```sql
-- 用户表索引检查
CREATE TABLE `user` (
    ...
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),           -- 状态查询
    KEY `idx_created_at` (`created_at`),  -- 时间排序
    KEY `idx_inviter_id` (`inviter_id`)    -- 推荐关系
);

-- 订单表联合索引
KEY `idx_user_status` (`user_id`, `order_status`),  -- 用户+状态查询
KEY `idx_expire_time` (`expire_time`, `order_status`)  -- 超时订单查询
```

### 3.2 N+1查询问题

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 列表查询 | 使用JOIN或批量查询避免N+1 | 必检 |
| 延迟加载 | 谨慎使用，ONE侧不建议使用 | 建议 |
| 批量操作 | 循环中的单条查询改为批量查询 | 必检 |

**检查代码示例：**
```java
// ❌ 错误 - N+1查询
@Query("SELECT t FROM Ticket t")
List<Ticket> findAll();
for (Ticket t : tickets) {
    Venue venue = venueRepository.findById(t.getVenueId());  // N次查询
}

// ✅ 正确 - JOIN FETCH
@Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.venue WHERE t.status = :status")
List<Ticket> findAllWithVenue(@Param("status") Integer status);

// ✅ 正确 - 批量查询
List<Long> venueIds = tickets.stream().map(Ticket::getVenueId).toList();
Map<Long, Venue> venueMap = venueRepository.findAllById(venueIds).stream()
    .collect(Collectors.toMap(Venue::getId, v -> v));
```

### 3.3 异常处理

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 异常捕获 | 业务异常使用自定义异常，系统异常统一处理 | 必检 |
| 日志记录 | 异常必须记录日志，包含堆栈信息 | 必检 |
| 异常传播 | 底层异常应包装后向上传递 | 必检 |
| 性能影响 | 避免在循环中捕获异常 | 建议 |

---

## 四、后端最佳实践检查

### 4.1 Controller层职责

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 参数校验 | 必须使用@Valid进行参数校验 | 必检 |
| 单一职责 | Controller只负责接收请求和响应，不处理业务逻辑 | 必检 |
| 返回类型统一 | 使用统一的Result包装响应 | 必检 |
| 日志记录 | 入口处记录请求日志 | 必检 |
| 禁止业务逻辑 | 不应在Controller中直接操作数据库 | 必检 |

**检查代码示例：**
```java
// ✅ 正确
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Valid RegisterDTO dto) {
        log.info("用户注册请求: {}", dto.getPhone());
        UserVO userVO = userService.register(dto);
        return Result.success(userVO);
    }
}

// ❌ 错误 - 业务逻辑放在Controller
@PostMapping("/register")
public Result<?> register(@RequestBody RegisterDTO dto) {
    // 直接在Controller处理业务
    User user = new User();
    user.setUsername(dto.getUsername());
    passwordEncoder.encode(dto.getPassword()); // 业务逻辑
    userRepository.save(user);  // 数据操作
}
```

### 4.2 Service层业务逻辑

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 事务管理 | 业务方法必须添加@Transactional注解 | 必检 |
| 事务传播 | 正确设置事务传播属性 | 必检 |
| 异常处理 | 业务异常抛出BusinessException | 必检 |
| 接口设计 | 接口职责单一，不包含无关方法 | 建议 |
| 代码复用 | 公共逻辑抽取到Service基类或工具类 | 建议 |

### 4.3 Repository层查询优化

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| SQL优化 | 避免SELECT *，只查询必要字段 | 建议 |
| 分页查询 | 大数据量必须分页，限制单次查询数量 | 必检 |
| 排序优化 | 有排序的查询必须有对应索引 | 必检 |
| 读写分离 | 读操作使用只读连接 | 建议 |

### 4.4 异常处理统一

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 全局异常处理 | 使用@RestControllerAdvice统一处理 | 必检 |
| 错误码统一 | 使用ResultCode枚举定义错误码 | 必检 |
| 异常信息 | 异常信息不能泄露敏感信息 | 必检 |
| 异步异常 | 异步任务的异常需要特殊处理 | 建议 |

---

## 五、专项检查清单

### 5.1 订单模块检查

| 检查项 | 检查标准 |
|--------|---------|
| 座位锁定 | 必须使用分布式锁 |
| 超时释放 | 订单超时自动释放座位 |
| 并发控制 | 避免超卖问题 |
| 幂等性 | 接口需要支持幂等 |

### 5.2 支付模块检查

| 检查项 | 检查标准 |
|--------|---------|
| 回调验签 | 支付回调必须验证签名 |
| 幂等处理 | 防止回调重复处理 |
| 状态同步 | 支付状态与订单状态同步 |

### 5.3 用户模块检查

| 检查项 | 检查标准 |
|--------|---------|
| 登录限制 | 登录失败次数限制 |
| 密码强度 | 密码复杂度要求 |
| 短信验证码 | 验证码有效期和限流 |

---

## 六、前端代码规范检查

### 6.1 React Native最佳实践

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 组件命名 | 使用PascalCase，如 `UserProfile`, `TicketCard` | 必检 |
| 组件文件 | 使用kebab-case，如 `user-profile.tsx`, `ticket-card.tsx` | 必检 |
| Props类型 | 使用TypeScript接口定义Props类型 | 必检 |
| State管理 | 使用Redux Toolkit进行全局状态管理 | 必检 |
| 自定义Hook | 复杂逻辑抽取为自定义Hook | 建议 |
| 组件拆分 | 单个组件不超过300行 | 建议 |

**命名示例：**
```typescript
// ✅ 正确
interface UserCardProps {
  user: User;
  onPress?: () => void;
}

export const UserCard: React.FC<UserCardProps> = ({ user, onPress }) => {
  return <Pressable onPress={onPress}>{user.name}</Pressable>;
};

// ❌ 错误
const UserCard = (props) => {
  return <Text>{props.user.name}</Text>;
};
```

### 6.2 代码格式

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 缩进 | 使用2空格缩进 | 必检 |
| 行长度 | 单行不超过100字符 | 建议 |
| 引号 | 使用单引号 | 建议 |
| 分号 | 语句末尾使用分号 | 建议 |
| 导包排序 | 按顺序：React, 内置, 外部组件, 项目组件 | 建议 |

**ESLint配置检查：**
```javascript
// .eslintrc.js
module.exports = {
  extends: ['@react-native', 'plugin:@typescript-eslint/recommended'],
  rules: {
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    'react-hooks/exhaustive-deps': 'warn',
  },
};
```

### 6.3 注释规范

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 组件注释 | 复杂组件必须包含注释说明功能 | 必检 |
| 函数注释 | 公共函数必须包含JSDoc注释 | 建议 |
| 常量注释 | 配置常量必须包含注释说明用途 | 建议 |
| TODO注释 | 未完成代码使用TODO标记 | 建议 |

---

## 七、前端安全检查

### 7.1 敏感信息处理

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| API密钥 | 禁止硬编码，必须从环境变量获取 | 必检 |
| Token存储 | 使用安全存储，不使用LocalStorage | 必检 |
| 敏感数据 | 敏感数据不在日志中输出 | 必检 |
| 调试模式 | 生产环境必须关闭调试模式 | 必检 |

**检查代码示例：**
```typescript
// ✅ 正确
import { SecureStorage } from './services/storage';

const token = await SecureStorage.get('auth_token');

// ❌ 错误
const token = localStorage.getItem('auth_token');
```

### 7.2 请求安全

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| HTTPS | 所有API请求必须使用HTTPS | 必检 |
| 请求头 | 正确设置Content-Type | 必检 |
| 身份验证 | 敏感接口必须携带Token | 必检 |
| 输入校验 | 用户输入必须校验 | 必检 |

---

## 八、前端性能检查

### 8.1 列表渲染性能

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| FlatList | 长列表必须使用FlatList | 必检 |
| keyExtractor | 必须提供唯一key | 必检 |
| 虚拟化 | 启用虚拟化优化 | 必检 |
| 分页加载 | 大数据量必须分页加载 | 必检 |
| 图片优化 | 使用缓存和适当尺寸 | 建议 |

**检查代码示例：**
```typescript
// ✅ 正确
<FlatList
  data={tickets}
  keyExtractor={(item) => item.id}
  renderItem={({ item }) => <TicketCard ticket={item} />}
  onEndReached={loadMore}
  onEndReachedThreshold={0.5}
/>

// ❌ 错误 - 使用Map渲染大量数据
{tickets.map((ticket) => (
  <TicketCard key={ticket.id} ticket={ticket} />
))}
```

### 8.2 内存泄漏防护

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| useEffect清理 | 异步操作必须在cleanup中清理 | 必检 |
| 定时器清理 | useEffect中的定时器必须清理 | 必检 |
| 订阅取消 | 订阅必须在cleanup中取消 | 必检 |
| 图片释放 | 离开页面必须释放图片资源 | 建议 |

**检查代码示例：**
```typescript
// ✅ 正确
useEffect(() => {
  const subscription = api.subscribe(data => {
    setData(data);
  });
  return () => subscription.unsubscribe();
}, []);

// ❌ 错误 - 没有清理
useEffect(() => {
  const subscription = api.subscribe(data => {
    setData(data);
  });
}, []);
```

### 8.3 渲染优化

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| React.memo | 纯组件使用memo包装 | 建议 |
| useCallback | 事件处理函数使用useCallback | 建议 |
| useMemo | 复杂计算使用useMemo | 建议 |
| 避免内联函数 | 避免在render中定义内联函数 | 建议 |

---

## 九、前端最佳实践检查

### 9.1 状态管理规范

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| Redux Toolkit | 使用RTK管理全局状态 | 必检 |
| Slice划分 | 按功能模块划分Slice | 必检 |
| 异步请求 | 使用createAsyncThunk | 必检 |
| 选择器 | 使用createSelector优化选择器 | 建议 |
| 状态规范化 | 范式化存储实体数据 | 建议 |

**检查代码示例：**
```typescript
// ✅ 正确 - RTK
const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setUser: (state, action: PayloadAction<User>) => {
      state.currentUser = action.payload;
    },
  },
});

// ❌ 错误 - 直接修改state
const updateUser = (user) => {
  state.currentUser = user;
};
```

### 9.2 API层设计

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| API封装 | 统一封装API请求方法 | 必检 |
| 错误处理 | 统一处理API错误 | 必检 |
| 请求拦截 | 添加请求拦截器 | 必检 |
| 响应拦截 | 添加响应拦截器 | 必检 |
| 类型定义 | 请求/响应必须定义类型 | 必检 |

### 9.3 组件设计原则

| 检查项 | 检查标准 | 优先级 |
|--------|---------|--------|
| 单一职责 | 组件只负责一个功能 | 必检 |
| Props传递 | 避免Props层级过深 | 必检 |
| 组件复用 | 公共组件抽取到components目录 | 必检 |
| 样式分离 | 样式与逻辑分离 | 建议 |

---

## 十、检查结果评分

| 评分等级 | 说明 |
|---------|------|
| 优秀 (90-100) | 符合所有检查标准，可直接合并 |
| 良好 (75-89) | 有少量小问题，建议修复后合并 |
| 合格 (60-74) | 存在中等问题，需要修改后合并 |
| 不合格 (<60) | 存在严重问题，必须重构 |

---

## 附录：Review记录模板

详见: `CodeReview问题记录模板.md`