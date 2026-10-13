# Ticket Pagination Interceptor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 BookingApi 注册 MyBatis-Plus MySQL 分页拦截器，使 `/api/tickets` 和 `/api/tickets/search` 的 `pageNum/pageSize/total/pages/list` 与真实 SQL 分页一致。

**Architecture:** 保留现有 Controller → Service → Repository → XML Mapper 调用链，只在 Spring 配置层新增一个 `MybatisPlusInterceptor` Bean，并注册一个 `PaginationInnerInterceptor(DbType.MYSQL)`。先用独立单元合同锁定 Bean 结构，再构建当前工作树、备份运行中 JAR、精确重启 8088 并用公开 GET 验证真实分页。

**Tech Stack:** Java 17、Spring Boot 3.2.5、MyBatis-Plus 3.5.6、JUnit 5、Maven 3.8.8、MySQL、PowerShell。

**Spec:** `docs/superpowers/specs/2026-08-26-ticket-pagination-interceptor-design.md`

**Workspace constraint:** 工作树已有大量用户修改。本计划只新增下列两个源文件，不还原、不格式化、不提交其他改动。构建和重启必须在所有测试通过后进行。

---

### Task 1: 用失败合同锁定分页插件配置

**Files:**
- Create: `booking-api/src/test/java/com/ticket/app/config/mybatis/MyBatisPlusConfigTest.java`
- Test: `booking-api/src/test/java/com/ticket/app/config/mybatis/MyBatisPlusConfigTest.java`

- [ ] **Step 1: 创建分页配置测试**

写入完整测试：

```java
package com.ticket.app.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MyBatisPlusConfigTest {

    @Test
    void shouldRegisterExactlyOneMysqlPaginationInterceptor() {
        MyBatisPlusConfig config = new MyBatisPlusConfig();

        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();
        List<InnerInterceptor> innerInterceptors = interceptor.getInterceptors();

        assertEquals(1, innerInterceptors.size());
        PaginationInnerInterceptor pagination = assertInstanceOf(
                PaginationInnerInterceptor.class,
                innerInterceptors.get(0)
        );
        assertEquals(DbType.MYSQL, pagination.getDbType());
    }
}
```

- [ ] **Step 2: 用 Java 17 运行测试并确认 RED**

从 `D:\AIProjects\BookingApi` 执行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd' `
  -pl booking-api -am `
  '-Dtest=MyBatisPlusConfigTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' `
  test
```

预期：Maven 退出非 0，编译错误明确指出 `MyBatisPlusConfig` 不存在。若失败原因是 Java 版本、依赖下载或其他文件编译错误，先停止并排查环境，不能把它当成本合同的 RED。

### Task 2: 注册唯一的 MySQL 分页拦截器

**Files:**
- Create: `booking-api/src/main/java/com/ticket/app/config/mybatis/MyBatisPlusConfig.java`
- Test: `booking-api/src/test/java/com/ticket/app/config/mybatis/MyBatisPlusConfigTest.java`

- [ ] **Step 1: 添加最小 Spring 配置**

写入：

```java
package com.ticket.app.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

不增加 LIMIT、不改 XML、不增加 PageHelper，也不改现有 Service。

- [ ] **Step 2: 重跑定向测试并确认 GREEN**

重复 Task 1 Step 2 的 Maven 命令。

预期：`MyBatisPlusConfigTest` 1 passed，Maven exit 0。

- [ ] **Step 3: 运行 booking-api 全量测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd' `
  -pl booking-api -am test
```

预期：Maven exit 0、0 failures、0 errors。若当前工作树中的无关测试失败，记录具体 node 和错误；不修改范围外代码来“顺便修复”。

### Task 3: 构建并安全替换当前 8088 服务

**Files:**
- Generated: `booking-api/target/booking-api-1.0.0-SNAPSHOT.jar`
- Temporary backup: `C:\Users\jrltn\AppData\Local\Temp\booking-api-pagination-backup\booking-api-1.0.0-SNAPSHOT.jar`
- Temporary logs: `C:\Users\jrltn\AppData\Local\Temp\booking-api-pagination-run\stdout.log`, `stderr.log`

- [ ] **Step 1: 重新核对当前监听进程和命令行**

```powershell
$listener = Get-NetTCPConnection -LocalPort 8088 -State Listen -ErrorAction Stop
$process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
$process | Select-Object ProcessId,Name,ExecutablePath,CommandLine | Format-List
```

预期：只有一个 8088 listener；进程为 Java 17，命令行指向 `D:\AIProjects\BookingApi\booking-api\target\booking-api-1.0.0-SNAPSHOT.jar`，参数为 `--spring.profiles.active=dev --server.port=8088`。不满足时停止本任务，不猜 PID。

- [ ] **Step 2: 在构建前备份当前运行 JAR**

确认备份目录不存在后创建，并复制当前 JAR：

```powershell
$backupDir='C:\Users\jrltn\AppData\Local\Temp\booking-api-pagination-backup'
if (Test-Path -LiteralPath $backupDir) { throw "备份目录已存在：$backupDir" }
New-Item -ItemType Directory -Path $backupDir | Out-Null
Copy-Item -LiteralPath 'D:\AIProjects\BookingApi\booking-api\target\booking-api-1.0.0-SNAPSHOT.jar' `
  -Destination "$backupDir\booking-api-1.0.0-SNAPSHOT.jar"
```

- [ ] **Step 3: 使用 Java 17 构建新 JAR**

只在 Task 2 全绿后执行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd' `
  -pl booking-api -am package
```

预期：Maven exit 0，新 JAR 存在且修改时间晚于构建开始时间。构建失败时保留当前 8088 进程，不执行停止操作。

- [ ] **Step 4: 只停止重新核对过的 8088 Java PID**

再次读取 8088 listener，确认 PID 与 Step 1 相同、命令行仍指向目标 JAR，然后执行：

```powershell
Stop-Process -Id $listener.OwningProcess -ErrorAction Stop
```

轮询直至 8088 不再监听；不停止其他 Java 进程。

- [ ] **Step 5: 隐藏启动新 JAR并等待 readiness**

```powershell
$runDir='C:\Users\jrltn\AppData\Local\Temp\booking-api-pagination-run'
if (Test-Path -LiteralPath $runDir) { throw "日志目录已存在：$runDir" }
New-Item -ItemType Directory -Path $runDir | Out-Null
$newProcess = Start-Process `
  -FilePath 'C:\Program Files\Java\jdk-17\bin\java.exe' `
  -ArgumentList @(
    '-jar',
    'D:\AIProjects\BookingApi\booking-api\target\booking-api-1.0.0-SNAPSHOT.jar',
    '--spring.profiles.active=dev',
    '--server.port=8088'
  ) `
  -WorkingDirectory 'D:\AIProjects\BookingApi\booking-api' `
  -RedirectStandardOutput "$runDir\stdout.log" `
  -RedirectStandardError "$runDir\stderr.log" `
  -WindowStyle Hidden `
  -PassThru
```

最多轮询 30 秒请求 `http://127.0.0.1:8088/api/categories`，要求 HTTP 成功且业务 `code=0`。超时或进程提前退出时读取两份日志，并用备份 JAR和原参数恢复服务；不得继续执行分页验收。

### Task 4: 验证真实分页和回归边界

**Files:**
- No source changes

- [ ] **Step 1: 验证第一页、第二页和不同 pageSize**

对 `/api/tickets` 请求：

```text
pageNum=1&pageSize=1
pageNum=1&pageSize=2
pageNum=2&pageSize=2
pageNum=1&pageSize=3
```

每次断言：

```text
HTTP 200
body.code == 0
data.pageNum == 请求 pageNum
data.pageSize == 请求 pageSize
len(data.list) <= pageSize
data.total >= len(data.list)
data.pages == ceil(data.total / pageSize)
```

额外断言第一页和第二页 ID 集合不相交；本机当前 7 条售票中数据在 `pageSize=2` 时预期 `total=7`、`pages=4`，但自动化判断应按返回 total 计算，不把 7 写成永久数据合同。

- [ ] **Step 2: 验证 `/tickets/search` 共用分页能力**

请求 `/api/tickets/search?pageNum=1&pageSize=2`，执行与上一步相同的稳定断言。

- [ ] **Step 3: 记录最终服务状态**

确认：

- 8088 只存在一个 listener，PID 等于 Step 3 启动的新进程；
- `/api/categories` 仍返回业务 code 0；
- 未启动第二个服务实例；
- 未清理或修改数据库。

- [ ] **Step 4: 清理临时备份与日志**

只在全部验证通过后，先解析并核对以下两个精确临时目录位于 `C:\Users\jrltn\AppData\Local\Temp`：

```text
C:\Users\jrltn\AppData\Local\Temp\booking-api-pagination-backup
C:\Users\jrltn\AppData\Local\Temp\booking-api-pagination-run
```

删除备份目录；运行日志若没有失败证据也删除。失败时保留日志并在交付中给出路径。

### Task 5: 最终复核与课程同步决策

**Files:**
- Inspect only: `D:\Education\自动化测试课程\course-materials\lesson-02\*`
- Inspect only: `D:\Education\自动化测试课程\course-materials\lesson-06\*`
- Inspect only: `D:\Education\自动化测试课程\course-materials\lesson-08\*`

- [ ] **Step 1: 重新运行分页配置测试和 HTTP 矩阵**

用全新的命令输出确认配置测试绿色、HTTP 分页绿色，不能依赖先前结果。

- [ ] **Step 2: 检查课程中“分页异常”说明是否已经过期**

真实服务修复后，课程不应继续把“当前 8088 未分页”作为现状。只列出需要同步的课程文件和具体段落；后端修复任务本身不擅自改课程，课程同步作为紧随其后的独立变更处理。

- [ ] **Step 3: 工作树边界复核**

运行 `git status --short`，确认本任务新增的源文件只有：

```text
booking-api/src/main/java/com/ticket/app/config/mybatis/MyBatisPlusConfig.java
booking-api/src/test/java/com/ticket/app/config/mybatis/MyBatisPlusConfigTest.java
```

设计规格和本计划位于 `docs/superpowers/`。已有用户修改仍保持原状；不提交、不 reset、不 checkout。
