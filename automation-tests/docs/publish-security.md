# 发布前凭据整改

管理员首次初始化必须通过进程环境提供 `BOOKING_ADMIN_PASSWORD`。缺失或空白时启动失败；数据库中已存在 `admin` 时保持原账号、密码和权限不变，不读取该值来重置账号。原先已公开的密码应在部署时通过受控流程轮换；本次源码整改不会自动轮换数据库账户。

`scripts/full-smoke-test.ps1` 同样从 `BOOKING_ADMIN_PASSWORD` 读取密码，未设置时在访问网络前失败。不要将密码写入命令记录、脚本或仓库。

`legacy/api-tests-v1` 是历史教学归档，其中原测试密码已替换为 `<ARCHIVED_PASSWORD_UNSET>`，不可作为当前业务回归执行。默认 pytest 的 `testpaths = tests` 保持不变；显式指定归档目录仍可收集历史用例，但归档中的占位值不应替换为真实凭据后提交。

在仓库根执行受限扫描：

```powershell
python automation-tests/scripts/scan-publish-secrets.py
```

扫描当前工作区中 Git 跟踪的 UTF-8 文本文件，检查私钥头、常见 GitHub/AWS/JWT token、指定敏感变量的非空 fallback、已整改的管理员/归档配置/冒烟脚本凭据常量。输出只含文件、行号、规则和数量，发现候选返回非零。它不是全格式熵扫描，也不读取 Git 历史、未跟踪/忽略文件或图片、视频等二进制内容。通过该检查不能替代对最终发布文件清单和历史的审计。

旧提交及 `api-tests-v1-baseline` 标签仍保留整改前内容，因此不得直接将现有完整历史或该标签发布到公开仓库。发布前应采用经审计的干净快照或处理待公开历史，并轮换曾暴露的管理员密码、课程清理密钥和其他真实凭据。文档中的 token 已改为运行时登录占位符，未尝试验证或使用旧 token。
