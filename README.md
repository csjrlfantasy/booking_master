# 票务App后端服务 (Booking API)

## 项目介绍

票务App后端服务，采用Spring Boot 3.2 + MyBatis Plus架构，提供用户、票务、订单、支付等核心功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 核心框架 |
| Spring Cloud | 2023.0.1 | 微服务框架 |
| Java | 17 | 运行环境 |
| MyBatis Plus | 3.5.6 | ORM框架 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.x | 缓存 |
| JWT | 0.12.5 | 认证 |
| Swagger | 2.8.0 | API文档 |

## 项目结构

```
D:\AIProjects\BookingApi
├── pom.xml                          # 父POM
├── booking-common/                  # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/ticket/app/common/
│       ├── Result.java              # 统一响应
│       ├── ResultCode.java          # 响应码
│       ├── PageResult.java          # 分页结果
│       ├── exception/               # 异常定义
│       └── util/                    # 工具类
├── booking-api/                     # 主应用模块
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ticket/app/
│       │   ├── BookingApiApplication.java  # 启动类
│       │   ├── controller/         # 控制器
│       │   ├── service/           # 业务层
│       │   ├── repository/        # 数据访问层
│       │   ├── entity/            # 实体类
│       │   └── config/            # 配置类
│       └── resources/
│           ├── application.yml    # 主配置
│           └── application-dev.yml # 开发配置
└── scripts/
    └── db/                         # 数据库脚本
        ├── 01_user_ddl.sql        # 用户表
        ├── 02_venue_ddl.sql       # 场馆表
        ├── 03_ticket_ddl.sql      # 票务表
        ├── 04_order_ddl.sql       # 订单表
        ├── 05_coupon_ddl.sql      # 优惠券表
        ├── init_data.sql          # 初始数据
        └── init_database.sql      # 数据库初始化
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.x

### 2. 初始化数据库

```bash
# 1. 创建数据库并导入表结构
mysql -u root -p < scripts/db/init_database.sql

# 或逐个执行SQL文件
mysql -u root -p booking_db < scripts/db/01_user_ddl.sql
mysql -u root -p booking_db < scripts/db/02_venue_ddl.sql
# ... 其他DDL文件
```

### 3. 配置修改

修改 `booking-api/src/main/resources/application-dev.yml` 中的数据库和Redis配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/booking_db
    username: root
    password: your_password
  
  data:
    redis:
      host: 127.0.0.1
      port: 6379
```

### 4. 编译运行

```bash
# 编译项目
mvn clean install

# 启动服务
cd booking-api
mvn spring-boot:run
```

### 5. 访问接口文档

- Swagger UI: http://localhost:8080/api/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/v3/api-docs

## 分层架构

```
┌─────────────────────────────────────────┐
│           Controller Layer              │
│    (参数校验、路由、日志、鉴权)            │
├─────────────────────────────────────────┤
│            Service Layer                │
│       (业务逻辑、事务、缓存)              │
├─────────────────────────────────────────┤
│          Repository Layer               │
│      (数据访问、SQL优化)                 │
├─────────────────────────────────────────┤
│            Common Layer                 │
│    (工具类、异常、常量、枚举)             │
└─────────────────────────────────────────┘
```

## API规范

### 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1709980800000,
  "requestId": "abc123"
}
```

### 分页响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "pages": 10
  }
}
```

### 错误码规范

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1001xx | 用户模块错误 |
| 1002xx | 票务模块错误 |
| 1003xx | 订单模块错误 |
| 1004xx | 支付模块错误 |
| 2001xx | 系统错误 |

## 开发规范

### 代码风格

- 遵循阿里巴巴Java开发规范
- 使用Lombok简化代码
- 使用MapStruct进行对象映射
- 接口必须添加Swagger注解

### 命名规范

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| 包名 | 小写+下划线 | com.ticket.app.controller |
| 类名 | 大驼峰 | UserController |
| 方法名 | 小驼峰 | getUserById |
| 常量 | 全大写+下划线 | MAX_RETRY_COUNT |
| 数据库表 | 小写+下划线 | user_address |
| 数据库字段 | 小写+下划线 | created_at |

### 分支管理

- `main` - 主分支
- `develop` - 开发分支
- `feature/*` - 功能分支
- `hotfix/*` - 热修复分支

## 注意事项

1. 所有配置文件中的敏感信息不要提交到版本库
2. 生产环境使用密钥管理服务存储敏感配置
3. 定期更新依赖版本，修复安全漏洞
4. 使用`git hooks`进行代码提交前的检查

## 许可证

MIT License
