# Ticket 接口分页失效修复设计

## 问题

`GET /api/tickets?pageNum=1&pageSize=2` 返回的 `data.pageSize` 为 2，但 `data.list` 仍包含全部 7 条记录，且 `total=0`、`pages=0`。

重复请求结果：

| 请求 pageSize | 响应 pageSize | list 数量 | total | pages |
|---:|---:|---:|---:|---:|
| 1 | 1 | 7 | 0 | 0 |
| 2 | 2 | 7 | 0 | 0 |
| 3 | 3 | 7 | 0 | 0 |
| 10 | 10 | 7 | 0 | 0 |

Controller、Service 和 Repository 已正确传递 `pageNum/pageSize`，Service 也创建了 `Page<Ticket>`。项目依赖 MyBatis-Plus 3.5.6，但全项目没有注册 `MybatisPlusInterceptor` 和 `PaginationInnerInterceptor`。因此自定义 Mapper SQL 没有被追加分页 LIMIT，也没有执行 count 查询。

## 修复方案

新增 `booking-api/src/main/java/com/ticket/app/config/mybatis/MyBatisPlusConfig.java`：

- 使用 Spring `@Configuration`；
- 暴露唯一的 `MybatisPlusInterceptor` Bean；
- 注册一个 `PaginationInnerInterceptor(DbType.MYSQL)`；
- 不修改现有 Controller、Service、Repository 或 XML SQL；
- 让其他已经传入 `Page` 的 MyBatis-Plus 查询共享同一正确分页能力。

不采用以下方案：

- 不在 `TicketMapper.xml` 手写 LIMIT/OFFSET 与独立 count SQL，以免和 MyBatis-Plus 分页模型重复；
- 不在 Service 对完整列表做 `subList`，避免全表读取、错误 total 和大数据性能问题；
- 不只把响应中的 pageSize 改成 list 数量，因为这只能掩盖查询未分页。

## 测试设计

先新增失败测试 `booking-api/src/test/java/com/ticket/app/config/mybatis/MyBatisPlusConfigTest.java`：

1. 配置能够创建 `MybatisPlusInterceptor`；
2. 内部拦截器恰好包含 `PaginationInnerInterceptor`；
3. 分页拦截器数据库类型为 MySQL，或通过行为/类型证明已按 MySQL 注册。

测试在新增配置前必须因类或 Bean 不存在而失败；新增配置后转绿。

随后运行：

- 分页配置定向单元测试；
- booking-api 现有测试集；
- Maven package；
- 重启当前 8088 服务后的真实 HTTP 分页矩阵。

真实 HTTP 验收：

- `pageNum=1&pageSize=2`：list 数量为 2、total 为当前售票中记录总数、pages 按总数计算；
- `pageNum=2&pageSize=2`：list 数量不超过 2，且记录 ID 与第一页不重复；
- `pageSize=1/2/3`：返回数量不超过 pageSize；
- `data.pageNum/pageSize/total/pages/hasPrevious/hasNext` 互相一致；
- `/tickets/search` 使用同一 Service，也获得相同分页行为。

## 运行与安全边界

- 当前 BookingApi 工作树已有大量用户修改。本次只新增上述两个文件，若测试需要最小调整也仅限这两个新文件；不格式化、不还原、不覆盖其他文件。
- 重新构建会把当前工作树状态编译进 JAR，但不会提交或重置任何现有修改。
- 重启前精确核对当前 8088 监听 PID；只停止该 Java 进程，再以新 JAR 和原参数 `--spring.profiles.active=dev --server.port=8088` 隐藏启动。
- 启动失败时保留构建输出和错误日志，不伪造绿色结果；必要时使用原 JAR 备份恢复当前服务。
- 不修改或清理数据库。分页验证只执行公开 GET，不创建业务数据。

## 完成标准

- RED/GREEN 配置测试证据完整；
- Maven 测试与构建成功；
- 8088 重启后真实分页矩阵通过；
- Swagger/OpenAPI 的 pageNum/pageSize 描述与实际行为一致；
- 本次没有覆盖现有未提交修改，没有新增无关生成文件或提交。
