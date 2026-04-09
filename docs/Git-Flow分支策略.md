# 票多星 TicketMore Git Flow 分支策略

| 文档信息 | 内容 |
|---------|------|
| 项目名称 | 票多星 TicketMore |
| 版本号 | V1.0 |
| 文档状态 | 正式发布 |
| 创建日期 | 2026-04-09 |

---

## 1. 分支模型概述

本项目采用 Git Flow 工作流模型，结合团队实际情况进行优化。核心思想是将开发工作与发布工作分离，通过功能分支（feature）、发布分支（release）、修复分支（hotfix）来管理不同阶段的代码。

### 1.1 分支结构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           develop (开发分支)                             │
│                                                                         │
│    ┌──────────────┐          ┌──────────────┐          ┌────────────┐  │
│    │ feature/xxx  │ ───▶     │ release/v1.0  │ ───▶    │   master   │  │
│    └──────────────┘          └──────────────┘          └────────────┘  │
│                                                                         │
│         ▲                        ▲                         ▲            │
│         │                        │                         │            │
│    ┌──────────────┐       ┌──────────────┐        ┌──────────────┐   │
│    │  hotfix/xxx  │ ─────▶│    master    │◀───────│  develop     │   │
│    └──────────────┘       └──────────────┘        └──────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 分支类型说明

| 分支类型 | 分支名称 | 用途 | 生命周期 | 合并目标 |
|----------|----------|------|----------|----------|
| master | 主分支 | 生产环境代码 | 长期存在 | - |
| develop | 开发分支 | 开发主分支 | 长期存在 | master |
| feature | feature/* | 新功能开发 | 短期 | develop |
| release | release/* | 版本发布准备 | 短期 | master + develop |
| hotfix | hotfix/* | 生产环境紧急修复 | 短期 | master + develop |

---

## 2. 分支命名规范

### 2.1 命名格式

| 分支类型 | 命名格式 | 示例 |
|----------|----------|------|
| feature | feature/功能描述-开发者 | feature/user-login-zhangsan |
| release | release/版本号 | release/v1.0.0 |
| hotfix | hotfix/问题描述-开发者 | hotfix/fix-login-bug-zhangsan |
| bugfix | bugfix/问题描述-开发者 | bugfix/fix-order-error-lisi |

### 2.2 命名规则

- 使用小写字母和连字符（-）
- 功能描述使用英文，控制在3-5个单词
- 开发者姓名拼音缩写（例：张三 -> zhangsan，李四 -> lisi）

---

## 3. 分支管理流程

### 3.1 功能开发流程 (Feature)

```
1. 从 develop 分支创建功能分支
   git checkout -b feature/user-login-zhangsan develop

2. 在功能分支上开发
   git add .
   git commit -m "feat: 添加用户登录功能"

3. 定期同步 develop 分支更新
   git fetch origin
   git merge origin/develop

4. 功能开发完成，合并到 develop
   git checkout develop
   git merge --no-ff feature/user-login-zhangsan
   git push origin develop

5. 删除功能分支（可选）
   git branch -d feature/user-login-zhangsan
```

### 3.2 版本发布流程 (Release)

```
1. 从 develop 分支创建发布分支
   git checkout -b release/v1.0.0 develop

2. 在发布分支上进行最终测试和修复
   - 修复测试中发现的问题
   - 更新版本号
   - 更新 CHANGELOG

3. 合并到 master 分支
   git checkout master
   git merge --no-ff release/v1.0.0
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin master --tags

4. 合并回 develop 分支
   git checkout develop
   git merge --no-ff release/v1.0.0
   git push origin develop

5. 删除发布分支
   git branch -d release/v1.0.0
```

### 3.3 紧急修复流程 (Hotfix)

```
1. 从 master 分支创建修复分支
   git checkout -b hotfix/fix-login-bug-zhangsan master

2. 修复问题并提交
   git commit -m "fix: 修复登录超时问题"

3. 合并到 master 分支
   git checkout master
   git merge --no-ff hotfix/fix-login-bug-zhangsan
   git tag -a v1.0.1 -m "Hotfix v1.0.1"
   git push origin master --tags

4. 合并回 develop 分支
   git checkout develop
   git merge --no-ff hotfix/fix-login-bug-zhangsan
   git push origin develop

5. 删除修复分支
   git branch -d hotfix/fix-login-bug-zhangsan
```

---

## 4. 分支环境对应

| 分支 | 环境 | 说明 |
|------|------|------|
| feature/* | 开发环境 | 开发人员进行功能开发 |
| develop | 测试环境 | 供测试人员进行功能测试 |
| release/* | 预发布环境 | 预发布验证 |
| master | 生产环境 | 正式生产环境 |

---

## 5. 提交信息规范

### 5.1 提交格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 5.2 Type 类型说明

| Type | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | feat(user): 添加手机号登录 |
| fix | Bug修复 | fix(order): 修复订单超时问题 |
| docs | 文档更新 | docs: 更新API文档 |
| style | 代码格式 | style: 格式化代码 |
| refactor | 代码重构 | refactor(user): 重构用户模块 |
| perf | 性能优化 | perf: 优化接口响应时间 |
| test | 测试相关 | test: 添加单元测试 |
| chore | 构建/工具 | chore: 更新依赖版本 |

### 5.3 提交示例

```
feat(user): 添加用户注册短信验证码功能

- 添加短信验证码发送接口
- 添加验证码校验逻辑
- 添加验证码有效期验证

Closes #123
```

---

## 6. 代码审查 (Code Review) 规范

### 6.1 合并请求 (Merge Request) 要求

- 必须经过 Code Reviewer 审核通过后才能合并
- 合并请求必须关联对应的需求/任务编号
- 代码必须通过自动化测试才能提交合并请求
- 合并请求必须有详细的描述说明

### 6.2 Code Review 检查点

- [ ] 代码是否符合编码规范
- [ ] 是否有明显的性能问题
- [ ] 是否有安全隐患
- [ ] 是否有未处理的异常
- [ ] 是否有未使用的代码
- [ ] 单元测试是否覆盖

---

## 7. 版本号管理

### 7.1 版本号格式

```
主版本号.次版本号.修订号
v1.0.0
```

- **主版本号 (Major)**：不兼容的API变更
- **次版本号 (Minor)**：向后兼容的功能新增
- **修订号 (Patch)**：向后兼容的Bug修复

### 7.2 版本发布规则

- 每次发布到生产环境必须打标签
- 标签信息包含：版本号、发布时间、发布内容
- 紧急修复使用修订号递增

---

## 8. Git Hooks 配置

### 8.1 预提交钩子 (pre-commit)

- 代码格式检查
- 敏感信息检查
- 单元测试检查

### 8.2 提交前钩子 (pre-push)

- 集成测试检查
- 静态代码分析

---

## 9. 常见问题处理

### 9.1 合并冲突

```
1. 拉取最新代码
   git fetch origin
   git rebase origin/develop

2. 解决冲突
   编辑冲突文件
   git add <resolved-files>

3. 继续rebase
   git rebase --continue

4. 强制推送（谨慎使用）
   git push --force-with-lease
```

### 9.2 误删分支恢复

```
1. 查看删除的分支
   git reflog

2. 恢复分支
   git checkout -b <branch-name> <commit-id>
```

---

## 10. 版本历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| V1.0 | 2026-04-09 | 初始版本 | 项目经理 |

---

**文档签署**

| 角色 | 姓名 | 日期 | 签字 |
|------|------|------|------|
| 项目经理 | | 2026-04-09 | |
| 开发负责人 | | 2026-04-09 | |
| 代码审查 | | 2026-04-09 | |
