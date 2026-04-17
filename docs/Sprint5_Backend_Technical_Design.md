# Sprint 5 后端技术设计方案

## 项目概述
本文档描述Sprint 5后端开发任务的技术设计方案，包括用户管理API、订单管理API、数据统计API和系统配置API。

**Sprint 5任务清单**:
- B001: 用户管理API - 列表查询/禁用/角色管理
- B002: 订单管理API - 列表/详情/状态修改
- B003: 数据统计API - 销售额/订单量/用户统计
- B004: 系统配置API - 基础配置/通知配置

**技术栈**:
- 后端框架: Spring Boot 2.7.x
- ORM框架: MyBatis-Plus 3.5.x
- 数据库: MySQL 8.0
- 接口文档: Swagger 3.0 (SpringDoc OpenAPI)

---

## 一、数据库设计

### 1.1 新增表结构

#### 1.1.1 系统配置表 (system_config)

```sql
CREATE TABLE `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `config_type` VARCHAR(50) DEFAULT 'string' COMMENT '配置类型:string,number,boolean,json',
  `config_name` VARCHAR(200) COMMENT '配置名称',
  `config_desc` VARCHAR(500) COMMENT '配置描述',
  `config_group` VARCHAR(50) DEFAULT 'general' COMMENT '配置分组:general,notification,payment',
  `is_system` TINYINT DEFAULT 0 COMMENT '是否系统配置(0-否,1-是)',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_group` (`config_group`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';
```

#### 1.1.2 用户角色表 (user_role)

```sql
CREATE TABLE `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_desc` VARCHAR(200) COMMENT '角色描述',
  `role_type` TINYINT DEFAULT 1 COMMENT '角色类型:1-系统角色,2-自定义角色',
  `permissions` TEXT COMMENT '权限列表(JSON数组)',
  `status` TINYINT DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色表';
```

#### 1.1.3 用户角色关联表 (user_role_relation)

```sql
CREATE TABLE `user_role_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

#### 1.1.4 操作日志表 (operation_log)

```sql
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `operator_id` BIGINT COMMENT '操作人ID',
  `operator_name` VARCHAR(50) COMMENT '操作人用户名',
  `operation_type` VARCHAR(50) COMMENT '操作类型',
  `operation_module` VARCHAR(50) COMMENT '操作模块',
  `operation_desc` VARCHAR(500) COMMENT '操作描述',
  `request_method` VARCHAR(10) COMMENT '请求方法',
  `request_url` VARCHAR(500) COMMENT '请求URL',
  `request_params` TEXT COMMENT '请求参数',
  `request_ip` VARCHAR(50) COMMENT '请求IP',
  `response_result` TEXT COMMENT '响应结果',
  `status` TINYINT DEFAULT 1 COMMENT '状态:0-失败,1-成功',
  `error_msg` TEXT COMMENT '错误信息',
  `cost_time` BIGINT COMMENT '耗时(毫秒)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

### 1.2 实体类定义

#### SystemConfig.java
```java
@Data
@TableName("system_config")
public class SystemConfig implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String configKey;
    private String configValue;
    private String configType;
    private String configName;
    private String configDesc;
    private String configGroup;
    private Integer isSystem;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
```

#### UserRole.java
```java
@Data
@TableName("user_role")
public class UserRole implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String roleName;
    private String roleCode;
    private String roleDesc;
    private Integer roleType;
    private String permissions;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 二、API接口设计

### 2.1 B001: 用户管理API

#### 2.1.1 管理员获取用户列表
- **接口路径**: `GET /api/admin/users`
- **是否需要认证**: 是 (管理员Token)
- **请求参数**:
  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | page | int | 否 | 页码，默认1 |
  | pageSize | int | 否 | 每页条数，默认10 |
  | keyword | string | 否 | 搜索关键词(用户名/手机号/昵称) |
  | status | int | 否 | 用户状态(0-禁用,1-正常,2-待激活) |
  | userType | int | 否 | 用户类型(1-普通,2-VIP,3-企业) |
  | startDate | string | 否 | 注册开始日期(yyyy-MM-dd) |
  | endDate | string | 否 | 注册结束日期(yyyy-MM-dd) |

- **响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 123456,
        "username": "test_user",
        "nickname": "测试用户",
        "phone": "13800138000",
        "email": "test@example.com",
        "userType": 1,
        "userTypeText": "普通用户",
        "status": 1,
        "statusText": "正常",
        "totalOrderAmount": 1000.00,
        "orderCount": 5,
        "createdAt": "2026-01-01 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 2.1.2 管理员获取用户详情
- **接口路径**: `GET /api/admin/users/{id}`
- **是否需要认证**: 是 (管理员Token)

#### 2.1.3 启用/禁用用户
- **接口路径**: `PUT /api/admin/users/{id}/status`
- **请求参数**:
```json
{
  "status": 0
}
```

#### 2.1.4 分配用户角色
- **接口路径**: `PUT /api/admin/users/{id}/role`
- **请求参数**:
```json
{
  "roleIds": [1, 2]
}
```

#### 2.1.5 获取用户角色列表
- **接口路径**: `GET /api/admin/roles`
- **是否需要认证**: 是 (管理员Token)

### 2.2 B002: 订单管理API

#### 2.2.1 管理员获取订单列表
- **接口路径**: `GET /api/admin/orders`
- **是否需要认证**: 是 (管理员Token)
- **请求参数**:
  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | page | int | 否 | 页码，默认1 |
  | pageSize | int | 否 | 每页条数，默认10 |
  | orderNo | string | 否 | 订单号 |
  | userId | long | 否 | 用户ID |
  | status | int | 否 | 订单状态(1-待支付,2-已支付,3-已完成,4-已取消,5-已退款,6-退款中) |
  | payStatus | int | 否 | 支付状态(0-未支付,1-已支付,2-已退款) |
  | startDate | string | 否 | 订单开始日期 |
  | endDate | string | 否 | 订单结束日期 |

- **响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 123456,
        "orderNo": "T202604161234567890",
        "userId": 123456,
        "username": "test_user",
        "ticketName": "演唱会A",
        "venueName": "北京体育馆",
        "showDate": "2026-05-01",
        "showTime": "19:30:00",
        "totalQuantity": 2,
        "totalAmount": 598.00,
        "payAmount": 598.00,
        "orderStatus": 2,
        "orderStatusText": "已支付",
        "payStatus": 1,
        "payChannel": "alipay",
        "createdAt": "2026-04-16 14:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 2.2.2 管理员获取订单详情
- **接口路径**: `GET /api/admin/orders/{id}`
- **是否需要认证**: 是 (管理员Token)

#### 2.2.3 管理员取消订单
- **接口路径**: `PUT /api/admin/orders/{id}/cancel`
- **请求参数**:
```json
{
  "cancelReason": "票务已售罄"
}
```

#### 2.2.4 管理员强制退款
- **接口路径**: `PUT /api/admin/orders/{id}/refund`
- **请求参数**:
```json
{
  "refundAmount": 598.00,
  "reason": "客户申请退款",
  "proofImages": ["http://example.com/img1.jpg"]
}
```

### 2.3 B003: 数据统计API

#### 2.3.1 获取销售统计
- **接口路径**: `GET /api/admin/stats/sales`
- **是否需要认证**: 是 (管理员Token)
- **请求参数**:
  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | type | string | 否 | 统计类型(day-日报,week-周报,month-月报)，默认day |
  | startDate | string | 否 | 开始日期(yyyy-MM-dd) |
  | endDate | string | 否 | 结束日期(yyyy-MM-dd) |

- **响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalSales": 100000.00,
    "totalOrders": 500,
    "averageOrderAmount": 200.00,
    "trendList": [
      {
        "date": "2026-04-16",
        "sales": 10000.00,
        "orderCount": 50,
        "refundCount": 2,
        "refundAmount": 400.00
      }
    ]
  }
}
```

#### 2.3.2 获取订单统计
- **接口路径**: `GET /api/admin/stats/orders`
- **是否需要认证**: 是 (管理员Token)
- **响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalOrders": 1000,
    "pendingPayOrders": 50,
    "paidOrders": 900,
    "completedOrders": 800,
    "cancelledOrders": 100,
    "refundingOrders": 10,
    "orderStatusDistribution": [
      {"status": 1, "count": 50},
      {"status": 2, "count": 900}
    ]
  }
}
```

#### 2.3.3 获取用户统计
- **接口路径**: `GET /api/admin/stats/users`
- **是否需要认证**: 是 (管理员Token)
- **响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalUsers": 5000,
    "activeUsers": 3000,
    "newUsersToday": 50,
    "newUsersThisWeek": 300,
    "newUsersThisMonth": 1000,
    "vipUsers": 500,
    "userGrowthTrend": [
      {"date": "2026-04-16", "count": 50}
    ]
  }
}
```

#### 2.3.4 获取数据面板概览
- **接口路径**: `GET /api/admin/stats/overview`
- **是否需要认证**: 是 (管理员Token)
- **响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "todaySales": 50000.00,
    "todayOrders": 250,
    "todayUsers": 100,
    "todayRefunds": 5,
    "yesterdaySales": 45000.00,
    "salesGrowth": 11.11,
    "pendingRefundCount": 10,
    "pendingOrderCount": 20
  }
}
```

### 2.4 B004: 系统配置API

#### 2.4.1 获取系统配置列表
- **接口路径**: `GET /api/admin/config`
- **是否需要认证**: 是 (管理员Token)
- **请求参数**:
  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | configGroup | string | 否 | 配置分组 |
  | keyword | string | 否 | 搜索关键词 |

#### 2.4.2 获取单个配置
- **接口路径**: `GET /api/admin/config/{id}`
- **是否需要认证**: 是 (管理员Token)

#### 2.4.3 更新系统配置
- **接口路径**: `PUT /api/admin/config/{id}`
- **请求参数**:
```json
{
  "configValue": "new_value",
  "configDesc": "新的描述"
}
```

#### 2.4.4 批量更新系统配置
- **接口路径**: `PUT /api/admin/config/batch`
- **请求参数**:
```json
{
  "configs": [
    {"id": 1, "configValue": "value1"},
    {"id": 2, "configValue": "value2"}
  ]
}
```

#### 2.4.5 获取通知配置
- **接口路径**: `GET /api/admin/config/notification`
- **是否需要认证**: 是 (管理员Token)

#### 2.4.6 更新通知配置
- **接口路径**: `PUT /api/admin/config/notification`
- **请求参数**:
```json
{
  "emailEnabled": true,
  "emailHost": "smtp.example.com",
  "emailPort": 465,
  "emailUsername": "noreply@example.com",
  "smsEnabled": true,
  "smsProvider": "aliyun"
}
```

---

## 三、DTO设计

### 3.1 用户管理相关DTO

#### AdminUserListRequest.java
```java
@Data
public class AdminUserListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Integer status;
    private Integer userType;
    private LocalDate startDate;
    private LocalDate endDate;
}
```

#### AdminUserListResponse.java
```java
@Data
public class AdminUserListResponse {
    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private Integer userType;
    private String userTypeText;
    private Integer status;
    private String statusText;
    private BigDecimal totalOrderAmount;
    private Integer orderCount;
    private LocalDateTime createdAt;
}
```

#### UpdateUserStatusRequest.java
```java
@Data
public class UpdateUserStatusRequest {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
```

#### AssignUserRoleRequest.java
```java
@Data
public class AssignUserRoleRequest {
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
```

### 3.2 订单管理相关DTO

#### AdminOrderListRequest.java
```java
@Data
public class AdminOrderListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String orderNo;
    private Long userId;
    private Integer status;
    private Integer payStatus;
    private LocalDate startDate;
    private LocalDate endDate;
}
```

#### AdminOrderListResponse.java
```java
@Data
public class AdminOrderListResponse {
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private String phone;
    private String ticketName;
    private String venueName;
    private LocalDate showDate;
    private LocalTime showTime;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer orderStatus;
    private String orderStatusText;
    private Integer payStatus;
    private String payChannel;
    private LocalDateTime createdAt;
}
```

#### AdminRefundRequest.java
```java
@Data
public class AdminRefundRequest {
    private BigDecimal refundAmount;
    private String reason;
    private String proofImages;
}
```

### 3.3 统计相关DTO

#### SalesStatsResponse.java
```java
@Data
public class SalesStatsResponse {
    private BigDecimal totalSales;
    private Integer totalOrders;
    private BigDecimal averageOrderAmount;
    private List<TrendItem> trendList;
    
    @Data
    public static class TrendItem {
        private String date;
        private BigDecimal sales;
        private Integer orderCount;
        private Integer refundCount;
        private BigDecimal refundAmount;
    }
}
```

#### OverviewStatsResponse.java
```java
@Data
public class OverviewStatsResponse {
    private BigDecimal todaySales;
    private Integer todayOrders;
    private Integer todayUsers;
    private Integer todayRefunds;
    private BigDecimal yesterdaySales;
    private BigDecimal salesGrowth;
    private Integer pendingRefundCount;
    private Integer pendingOrderCount;
}
```

### 3.4 系统配置相关DTO

#### SystemConfigRequest.java
```java
@Data
public class SystemConfigRequest {
    private Long id;
    private String configKey;
    private String configValue;
    private String configDesc;
}
```

#### BatchUpdateConfigRequest.java
```java
@Data
public class BatchUpdateConfigRequest {
    @NotEmpty(message = "配置列表不能为空")
    private List<SystemConfigRequest> configs;
}
```

#### NotificationConfigRequest.java
```java
@Data
public class NotificationConfigRequest {
    private Boolean emailEnabled;
    private String emailHost;
    private Integer emailPort;
    private String emailUsername;
    private String emailPassword;
    private Boolean smsEnabled;
    private String smsProvider;
}
```

---

## 四、目录结构

```
booking-api/src/main/java/com/ticket/app/
├── controller/
│   └── admin/
│       ├── AdminUserController.java      # 用户管理API
│       ├── AdminOrderController.java      # 订单管理API
│       ├── AdminStatsController.java      # 数据统计API
│       ├── AdminConfigController.java     # 系统配置API
│       └── dto/
│           ├── request/                   # 请求DTO
│           └── response/                  # 响应DTO
├── service/
│   └── admin/
│       ├── AdminUserService.java
│       ├── AdminUserServiceImpl.java
│       ├── AdminOrderService.java
│       ├── AdminOrderServiceImpl.java
│       ├── AdminStatsService.java
│       ├── AdminStatsServiceImpl.java
│       ├── AdminConfigService.java
│       └── AdminConfigServiceImpl.java
├── repository/
│   ├── user/
│   │   ├── UserRepository.java
│   │   └── UserRoleRepository.java
│   ├── order/
│   │   └── OrderRepository.java           # 扩展统计查询
│   └── config/
│       └── SystemConfigRepository.java
└── entity/
    ├── SystemConfig.java
    ├── UserRole.java
    └── OperationLog.java
```

---

## 五、权限设计

### 5.1 管理员角色
- **超级管理员**: 拥有所有权限
- **运营管理员**: 用户管理、订单管理、数据统计
- **客服**: 订单管理、查看统计数据

### 5.2 权限注解
使用`@RequiresRoles`或自定义注解进行权限控制:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminPermission {
    String value() default "";
}
```

---

## 六、开发计划

| 阶段 | 任务 | 预计工时 |
|------|------|----------|
| M1 | 技术设计评审 | 0.5天 |
| M2 | 数据库表创建 | 0.5天 |
| M3 | B001 用户管理API | 2天 |
| M4 | B002 订单管理API | 2天 |
| M5 | B003 数据统计API | 2天 |
| M6 | B004 系统配置API | 1天 |
| M7 | 单元测试 | 1天 |

---

## 七、验收标准

- [ ] 用户管理: 支持分页查询、禁用/启用、角色分配
- [ ] 订单管理: 支持管理员订单查询、强制取消、退款
- [ ] 数据统计: 支持销售/订单/用户多维度统计
- [ ] 系统配置: 支持配置CRUD、通知配置
- [ ] API测试覆盖率 > 80%
- [ ] 所有接口需通过Swagger文档化

---

*文档创建时间: 2026-04-17*
*版本: 1.0.0*
