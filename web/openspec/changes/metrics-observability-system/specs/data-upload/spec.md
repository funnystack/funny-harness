## ADDED Requirements

### Requirement: 定时批量上报本地日志

系统 SHALL 通过 Cron 任务每 5 分钟扫描本地日志文件 `~/.claude/metrics/events.jsonl`，将未上报的事件批量打包，通过 HTTPS POST 发送到服务端收集 API。每次上报 SHALL 在请求头中携带 `X-Client-Id`（用户名的 hash）和 `X-Batch-Id`（时间戳 + 随机数）。

#### Scenario: 正常批量上报

- **WHEN** Cron 触发且本地有 50 条未上报事件
- **THEN** 脚本将 50 条事件打包为 JSON 数组，POST 到 `/api/metrics/collect`，服务端返回 200 后删除已上报的记录

#### Scenario: 上报失败时保留数据

- **WHEN** HTTPS POST 请求返回网络错误或服务端 5xx
- **THEN** 脚本 SHALL 保留本地日志文件不做删除，等待下次 Cron 触发时重试

#### Scenario: 服务端返回 4xx 错误

- **WHEN** HTTPS POST 请求返回 400/401/403
- **THEN** 脚本 SHALL 将失败批次移至 `~/.claude/metrics/failed/` 目录，附带错误信息和时间戳，避免无限重试无效数据

### Requirement: 上报数据格式校验

每条待上报的事件 SHALL 包含必填字段：event_type、timestamp。缺少必填字段的事件 SHALL 在上报前被过滤并记录到 `~/.claude/metrics/invalid.jsonl`。

#### Scenario: 过滤无效事件

- **WHEN** 本地日志中有一条缺少 timestamp 字段的损坏记录
- **THEN** 上报脚本 SHALL 过滤掉该条记录，移入 invalid.jsonl，其余正常记录照常上报

### Requirement: 已上报记录清理

上报成功后，脚本 SHALL 只删除已确认上报成功的记录，未上报的记录 SHALL 保持不动。清理方式为：将 `events.jsonl` 中未上报的行写回文件，删除已上报的行。

#### Scenario: 部分上报成功

- **WHEN** 本地有 100 条记录，本次上报了前 50 条且成功
- **THEN** 脚本 SHALL 保留后 50 条未上报记录在 events.jsonl 中
