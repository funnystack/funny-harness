## ADDED Requirements

### Requirement: 批量数据收集接口

系统 SHALL 提供 `POST /api/metrics/collect` 接口接收客户端上报的度量数据。请求体为 JSON 数组，每条记录包含 event_type、timestamp 及事件特定字段。接口 SHALL 只做格式校验后将原始事件写入 `metrics_raw_events` 表（不做业务解析），同时从 HTTP 请求中提取客户端 IP 写入 `client_ip` 字段（优先取 `X-Forwarded-For` 头，其次取 `request.getRemoteAddr()`），返回 `ApiResult<Integer>` 表示成功接收的记录数。

#### Scenario: 接收一批有效数据并记录 IP

- **WHEN** 客户端 POST 50 条有效事件到 `/api/metrics/collect`，请求来源 IP 为 192.168.1.100
- **THEN** 服务端校验格式通过后写入 metrics_raw_events（processed=0，client_ip=192.168.1.100），返回 `ApiResult.success(50)`

#### Scenario: 通过代理转发时获取真实 IP

- **WHEN** 客户端通过反向代理访问，X-Forwarded-For 头为 10.0.0.5
- **THEN** client_ip SHALL 取 X-Forwarded-For 的第一个值（10.0.0.5）而非代理 IP

#### Scenario: 接收包含无效记录的批次

- **WHEN** 客户端 POST 50 条事件，其中 3 条缺少必填字段
- **THEN** 服务端 SHALL 写入 47 条有效记录到 raw 表，过滤 3 条无效记录，返回 `ApiResult.success(47)`

#### Scenario: 空批次请求

- **WHEN** 客户端 POST 空数组到 `/api/metrics/collect`
- **THEN** 服务端 SHALL 返回 `ApiResult.success(0)`

### Requirement: 原始事件异步处理

系统 SHALL 异步处理 metrics_raw_events 中 `processed=0` 的记录，按 event_type 解析 raw_payload 并写入对应的 domain 表（metrics_sessions / metrics_skill_usage / metrics_agent_traces / metrics_users）。处理成功的记录 SHALL 更新 `processed=1`，处理失败的 SHALL 更新 `processed=2` 并记录错误原因。系统 SHALL 支持将 `processed=2` 的记录重置为 `0` 进行重跑。

#### Scenario: SessionStart 事件异步处理

- **WHEN** 异步任务扫描到一条 event_type=session_start 的 raw 记录
- **THEN** 解析 raw_payload 提取 username、project_path，写入 metrics_sessions（started_at），自动注册新用户到 metrics_users，更新 raw 记录 processed=1

#### Scenario: 事件处理失败可重跑

- **WHEN** 一条 raw 记录解析时因数据格式异常失败
- **THEN** 更新 processed=2，记录 process_error='JSON parse error: ...'；后续人工修正后可重置 processed=0 重新处理

### Requirement: 单条记录校验规则

每条上报事件 SHALL 包含非空的 event_type 和有效的 timestamp（ISO 8601 格式，且不超过当前时间 5 分钟）。SessionStart 事件 SHALL 包含 username 和 project_path。PreToolUse 事件 SHALL 包含 tool_type。Stop 事件 SHALL 包含 session_id。

#### Scenario: 缺少必填字段

- **WHEN** 一条事件的 event_type 为空
- **THEN** 该条记录 SHALL 被过滤，不计入成功数

#### Scenario: 时间戳格式错误

- **WHEN** 一条事件的 timestamp 不是有效的 ISO 8601 格式
- **THEN** 该条记录 SHALL 被过滤

### Requirement: Dashboard 数据查询接口

系统 SHALL 提供以下 Dashboard 查询接口：

1. `GET /api/metrics/daily` — 日报数据（当日 DAU、Token、会话数、人均时长、小时级趋势、项目分布、用户排行、能力调用热力图）
2. `GET /api/metrics/weekly` — 周报数据（周汇总、日活趋势、能力使用趋势、新增用户、效率评估）
3. `GET /api/metrics/monthly` — 月报数据（安装覆盖率、成本分析、项目热度矩阵、推广建议）
4. `GET /api/metrics/agent/dashboard` — Agent 运营看板（今日任务数、一次通过率、单任务成本、活跃 Agent 数、任务完成率趋势、成本趋势）
5. `GET /api/metrics/agent/trace/{traceId}` — 单次 Agent 任务 Trace 链路详情

所有接口 SHALL 返回 `ApiResult<T>` 格式。

#### Scenario: 查询日报数据

- **WHEN** 前端请求 `GET /api/metrics/daily`
- **THEN** 返回当日 DAU、Token 消耗、会话总数、人均时长，以及小时级 Token 趋势、项目分布 Top 5、用户活跃排行、能力调用热力图

#### Scenario: 查询 Agent Trace 详情

- **WHEN** 前端请求 `GET /api/metrics/agent/trace/T-20260315-0042`
- **THEN** 返回该 Trace 的完整步骤链路，每个 Step 包含工具名称、耗时、Token 消耗、状态

#### Scenario: 查询日期范围的数据不存在

- **WHEN** 前端请求某天的日报但该天无数据
- **THEN** 返回 `ApiResult.success(null)` 而非错误

### Requirement: 接口权限控制

所有 `/api/metrics/**` 接口 SHALL 要求携带有效的 API Key（通过 `X-API-Key` 请求头）。Dashboard 查询接口 SHALL 支持 Admin 和 Viewer 两种角色：Admin 可看所有数据，Viewer 只能看脱敏汇总数据。

#### Scenario: 无 API Key 访问

- **WHEN** 请求 `/api/metrics/daily` 不携带 `X-API-Key`
- **THEN** 返回 401 Unauthorized

#### Scenario: Viewer 角色请求用户排行

- **WHEN** Viewer 角色请求日报数据
- **THEN** 用户排行中的用户名 SHALL 脱敏显示（如 "zhang\*\*\*"）
