# 票多星票务平台

票多星是一套票务教学项目，包含 Spring Boot 后端、React Web 客户端/管理端，以及 React Native Android 客户端。

## 默认访问地址

| 服务 | 地址 |
| --- | --- |
| 客户端首页 | http://127.0.0.1:3000/shop |
| 客户端登录 | http://127.0.0.1:3000/shop/login |
| 管理端登录 | http://127.0.0.1:3000/login |
| 后端 API 根地址 | http://127.0.0.1:8088/api |
| 前端代理 API | http://127.0.0.1:3000/api |
| 健康检查 | http://127.0.0.1:8088/api/actuator/health |
| Swagger UI | http://127.0.0.1:8088/api/swagger-ui.html |
| OpenAPI JSON | http://127.0.0.1:8088/api/v3/api-docs |

浏览器访问页面时使用 `3000` 端口；直接调用后端接口时使用 `8088/api`。Web 前端会把 `/api` 请求代理到后端。

## CentOS 服务器

当前生产环境入口：

| 服务 | 地址 |
| --- | --- |
| 客户端首页 | http://154.12.20.250/shop |
| 客户端登录 | http://154.12.20.250/shop/login |
| 管理端登录 | http://154.12.20.250/login |
| 后端 API 根地址 | http://154.12.20.250/api |
| 健康检查 | http://154.12.20.250/api/actuator/health |

生产环境已关闭 Swagger。项目通过 Docker Compose 运行，API 只绑定服务器本机的 `127.0.0.1:8088`，由 Nginx 统一提供 Web 页面和 `/api` 反向代理。

服务器目录：

- 部署入口：`/opt/ticketmore`
- 实际数据目录：`/mnt/data/ticketmore`
- Web 静态文件：`/www/wwwroot/ticketmore`
- Nginx 配置：`/www/server/panel/vhost/nginx/ticketmore.conf`
- 环境密钥：`/opt/ticketmore/.env`，不得提交到 Git

常用运维命令：

```bash
cd /opt/ticketmore
docker compose ps
docker compose logs --tail=200 api
docker compose logs --tail=200 db
docker compose up -d
docker compose restart api
```

更新前应先备份 `/opt/ticketmore/.env`、`/opt/ticketmore/uploads` 和 MySQL 数据。数据库容器未暴露公网端口，不要为了管理方便直接开放 `3306`。

## 一键启动

环境要求：

- JDK 17（脚本优先使用 `C:\Program Files\Java\jdk-17`）
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- 数据库名 `ticket_booking`

确认 MySQL 和 Redis 已启动。首次运行 `start-all.ps1` 会自动创建一个不提交到 Git 的 `.env` 文件，并生成本地 JWT 和支付回调密钥；如果本地 MySQL 的 `root` 用户设置了密码，请在 `.env` 中填写 `DB_PASSWORD`。然后在项目根目录双击：

```text
start-all.bat
```

或在 PowerShell 中运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

脚本会执行以下操作：

1. 强制使用 JDK 17，并重新打包后端，避免运行过期 JAR 或旧依赖。
2. 启动后端并等待健康检查通过。
3. 检查 Web 前端依赖并启动前端。
4. 把日志和进程号写入 `logs` 目录。

本地启动失败时，脚本会在报错中附带后端的最后日志。不要把 `.env` 上传或提交到仓库。

默认只启动 Web 前后端。需要同时启动 Android Metro 时运行：

```powershell
.\start-all.ps1 -IncludeMetro
```

## 分别启动

只启动后端：

```text
scripts\start-backend-jdk17.cmd
```

只启动前端：

```text
scripts\start-frontend-web.cmd
```

## 数据库初始化

首次运行时依次执行 `init_database.sql`、用户、场馆、分类、票务、订单、优惠券、购物车 DDL，最后按需导入 `init_data.sql`。所有初始化脚本统一使用数据库 `ticket_booking` 和 `utf8mb4` 字符集。开发库已有旧表时，应用启动阶段还会补齐项目新增的兼容字段。

## 故障排查

启动失败时优先查看：

- `logs/backend.out.log`
- `logs/backend.err.log`
- `logs/frontend.out.log`
- `logs/frontend.err.log`

后端代码或依赖修改后无需手工删除旧 JAR；新版一键脚本每次都会重新打包。不要再使用仓库根目录中早期遗留的 `start.bat`、`startup.ps1` 等实验脚本作为标准启动入口。

## 技术栈

- 后端：Java 17、Spring Boot 3.2.5、MyBatis Plus、MySQL
- Web：React、TypeScript、Vite
- Android：React Native
