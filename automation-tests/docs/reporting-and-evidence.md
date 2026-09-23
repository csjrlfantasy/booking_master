# 测试报告与证据口径

## 输出目录

`scripts/run-suite.ps1` 每次使用 UTC 时间、环境、随机 runId 生成新目录。普通 `python -m pytest` 也自动生成隔离目录；需要指定位置时，`--evidence-dir` 必须指向空目录。不要给多个执行复用同一个目录。套件脚本使用参数数组调用 Python，保留测试进程退出码。

```text
report/<UTC>-<environment>-<runId>/
  summary.json                 # 分阶段结果与统计口径
  environment.properties      # 环境及报告 runId
  junit.xml                    # Jenkins JUnit
  pytest.html                  # 自包含 pytest-html
  allure-results/              # Allure 原始结果，不等于生成的 HTML
  browser/test-<hash>-<browser>-<id>/
    context-1-page-0.png
    context-1-sanitized-action-trace.json
    capture-diagnostics.json
    video/*.webm
```

套件报告 runId 标识一次命令，夹具 `run_id` 标识单个测试的数据归属，两者用途不同。JUnit 的 `booking.metadata` 属性和 Allure 标签关联环境、用例 nodeid、浏览器以及数据 runId。API 附件为递归脱敏后的请求参数/请求体、响应业务数据或错误码；任意响应字符串默认省略，只保留白名单状态。E2E 另有业务 ID、订单/支付状态、金额等允许字段的步骤附件。

`cleanup pending: registered resource IDs` 表示开始回收已登记资源；`cleanup observed: response` 才是服务返回的清理结果；`cleanup observed: ERROR` 表示清理抛错或拒绝。接口返回成功仍需校验 rejected 为零。单个测试的业务断言失败与 teardown error 各自记录，前面的失败不会把清理错误变为通过。

## 统计口径

| 字段/状态 | 含义 |
| --- | --- |
| planned | 外部计划用例总量，当前为 null；不能由收集总量推断 |
| collected | 选中的参数化 nodeid 数量；浏览器参数分别计数 |
| attempted | setup 阶段报告数，包含 setup 失败和重复尝试 |
| executed | 到达 call 阶段的报告数，包含 xfail、xpass |
| passed / failed | call 正常通过/失败 |
| error | setup 或 teardown 失败 |
| skipped | pytest 原始 skipped |
| xfail / xpass | 预期失败/意外通过；strict XPASS 仍保留非零退出码 |

`statuses` 是 call 结果加非 passed 的 setup/teardown 结果，因此一个“断言失败且清理也失败”的用例会同时产生 failed 和 error，状态数量不能简单当作唯一用例总量。`reports` 保留 nodeid、阶段、耗时及 worker，供后续比对。当前没有安装自动重试插件，不推算重试后通过率。若以后开启重试，需进一步按环境、版本、nodeid 与 attempt 关联首次及最终结果。

`--collect-only` 的 summary 明确标记 collectionOnly，attempted/executed/passed 均为零。只收集成功不能证明接口、浏览器或公网清理可用。JUnit/HTML/Allure 各自的聚合口径可能不同，诊断时以分阶段报告和退出码共同判断。没有把通过率用作需求覆盖率，也没有设定任意百分比发布门槛。

## 浏览器诊断和脱敏边界

所有当前 UI/E2E 自建 context 在创建时启用视频，并在关闭前尝试截图及动作摘要。最终 teardown 完成后，仅失败用例保留这些产物；通过/跳过用例的临时产物移除。setup 出错但 context 已创建时也尽量留证；浏览器无法启动或 context 已提前失效时只能记录诊断。截图、trace 读取或清理产物的失败不覆盖原有断言；实际 context.close 的错误仍作为 teardown error。

**完整 Playwright trace 默认不归档。** 原始 trace 可能记录令牌注入、请求头和存储内容。框架在系统临时目录中短暂导出 trace，提取协议动作 ID/名称/时间后删除原始 ZIP；归档的 `sanitized-action-trace.json` 只是裁剪后的 JSON 动作时间线，不是可用 Trace Viewer 打开的完整 trace。params、result、error 文本、network、resources、storage、源代码和 DOM snapshots 均不导出。完整 trace 只能由教师在受控本机短期调试，使用后清理，不能直接放入 Jenkins 归档。

JSON 附件会递归遮盖令牌、Authorization/Cookie、密码、课程密钥、管理员凭据和手机号、身份证、姓名、邮箱等身份字段。任意对象字符串不序列化，不输出 localStorage/storage_state。测试 ID、参数名称、断言文本禁止放入真实密码或令牌；pytest 自身生成的异常文本和截图/视频不能视为已全量脱敏。截图遮盖 input/textarea，页面其他位置和录像仍可能出现教学身份数据；只允许合成教学数据。

## Jenkins 保留建议

建议 JUnit/HTML/summary 保留 14 天或最近 20 次构建；失败截图/动作摘要/视频保留 7 天或最近 10 次失败构建。实际清理需由 Jenkins buildDiscarder/归档策略配置，本脚本不会删除历史运行目录。限制报告访问权限，不上传 `.env`、原始 trace、存储状态文件或系统临时目录。只归档本次 runDirectory，并将清理失败设为门禁失败。

生成 Allure HTML：

```powershell
./scripts/generate-report.ps1 -ResultsDirectory 'report/<本次运行>/allure-results'
```

需要预先安装 Allure CLI。缺少 CLI、没有测试结果、CLI 返回非零或缺少 index.html 都会报错；不会声称原始结果已经是完整报告。默认输出到新的 `allure-html-<id>`，已有报告不覆盖。

## 验证范围

本阶段验证的是离线报告逻辑、状态台账、脱敏、mock context 捕获韧性、PowerShell 参数/门禁/退出码与双环境收集。真实截图、录像、实际浏览器、公网接口及 Jenkins 归档仍需后续在线验收，不由离线通过替代。
