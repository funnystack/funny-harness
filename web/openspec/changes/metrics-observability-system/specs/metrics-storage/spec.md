## ADDED Requirements

### Requirement: 原始事件表存储

系统 SHALL 创建 `metrics_raw_events` 表作为所有上报数据的唯一入口。每条上报事件 SHALL 先写入此表，保留完整 JSON 原文。表结构包含：自增主键 id、batch_id（VARCHAR(64)）、client_id（VARCHAR(64)，SHA256($USER@hostname)）、client_ip（VARCHAR(45)，服务端从 HTTP 请求获取）、event_type（VARCHAR(32)）、raw_payload（JSON，完整原始报文）、processed（TINYINT，0=未处理 1=已处理 2=处理失败）、process_error（VARCHAR(512)）、received_at、creates_stime、modified_stime、is_del。SHALL 建立 (processed, received_at)、(client_id, received_at)、(event_type, received_at) 三个索引。

#### Scenario: 上报事件先落 raw 表

- **WHEN** 收集 API 收到 50 条上报事件（HTTP 请求来源 IP 为 192.168.1.100）
- **THEN** 50 条全部写入 metrics_raw_events，processed=0，raw_payload 保留原始 JSON，client_ip 填入 192.168.1.100

#### Scenario: 按处理状态扫描待处理记录

- **WHEN** 异步处理任务查询 processed=0 的记录
- **THEN** 利用 (processed, received_at) 索引高效返回未处理事件

### Requirement: 用户表存储

系统 SHALL 创建 `metrics_users` 表存储用户信息。表结构包含：自增主键 id、user_id（VARCHAR(64)，SHA256($USER@hostname)）、username（VARCHAR(128)，系统用户名）、username_hash（VARCHAR(64)，用户名 SHA256 脱敏查询用）、hostname（VARCHAR(128)，机器主机名）、team（VARCHAR(64)）、creates_stime、modified_stime、is_del。主键 SHALL 使用 bigint(20) 自增，另加 user_id 唯一索引。同一用户在不同机器上 SHALL 生成不同的 user_id（因为 hostname 不同），视为不同设备。

#### Scenario: 新用户首次上报数据

- **WHEN** 收集服务收到一个不存在的 user_id 的上报数据，username=zhangsan，hostname=zhangsan-macbook
- **THEN** 自动在 metrics_users 表中插入一条新用户记录，user_id=SHA256("zhangsan@zhangsan-macbook")

#### Scenario: 同一用户在另一台机器上报

- **WHEN** 同一个 zhangsan 在 zhangsan-imac 上首次上报
- **THEN** 生成不同的 user_id=SHA256("zhangsan@zhangsan-imac")，插入新记录。通过 username 字段可关联同一人的多台设备

#### Scenario: 已有用户再次上报

- **WHEN** 收集服务收到一个已存在的 user_id 的上报数据
- **THEN** 不重复插入用户记录，更新 modified_stime

### Requirement: 会话表存储

系统 SHALL 创建 `metrics_sessions` 表存储会话数据。表结构包含：session_id（VARCHAR(128) 主键）、user_id、project_path、started_at、ended_at、duration_seconds、total_tokens、tool_read_count、tool_edit_count、tool_bash_count、tool_grep_count、creates_stime、modified_stime、is_del。SHALL 建立 (user_id, started_at) 和 (started_at) 两个索引。

#### Scenario: SessionStart 事件写入

- **WHEN** 收集服务收到 session_start 事件
- **THEN** 在 metrics_sessions 表插入一条记录，started_at 设为事件时间戳，ended_at 为 null

#### Scenario: Stop 事件更新会话

- **WHEN** 收集服务收到 session_stop 事件，匹配到 session_id
- **THEN** 更新该记录的 ended_at、duration_seconds、total_tokens

### Requirement: 能力调用记录表存储

系统 SHALL 创建 `metrics_skill_usage` 表存储能力调用数据。表结构包含：自增主键 id、session_id、user_id、project_path、capability_type（ENUM: skill/agent/command/slash_command）、capability_name（VARCHAR(128)）、triggered_at、tokens_consumed、duration_ms、result_status（ENUM: success/failed/timeout）、trace_id（可选，关联 agent_traces）、creates_stime、modified_stime、is_del。SHALL 建立 (capability_type, capability_name)、(user_id, project_path)、(triggered_at) 三个索引。SHALL 通过 session_id 外键关联 metrics_sessions。

#### Scenario: Skill 调用记录写入

- **WHEN** 收集服务收到 capability_type=skill, capability_name=code-review 的事件
- **THEN** 在 metrics_skill_usage 表插入一条记录

#### Scenario: 按类型和名称查询能力使用热度

- **WHEN** 分析任务查询过去 7 天 skill 类型的调用频次
- **THEN** 利用 (capability_type, capability_name) 索引高效返回各 Skill 的调用次数排行

### Requirement: Agent Trace 表存储

系统 SHALL 创建 `metrics_agent_traces` 表存储 Agent 执行追踪数据。表结构包含：trace_id（VARCHAR(128) 主键）、session_id、agent_type（VARCHAR(32)）、task_type（VARCHAR(32)）、task_desc（TEXT）、status（ENUM: running/completed/failed/timeout）、total_tokens、total_duration_ms、files_changed、loop_count、quality_score（INT）、revision_count、started_at、completed_at、creates_stime、modified_stime、is_del。SHALL 建立 (agent_type)、(task_type)、(started_at) 三个索引。

#### Scenario: Agent 任务开始时写入

- **WHEN** 收集服务收到 Agent 任务的 task_summary 且 task_result 为 running
- **THEN** 在 metrics_agent_traces 插入一条记录，status=running

#### Scenario: Agent 任务完成后更新

- **WHEN** 收集服务收到 Agent 任务的完成事件
- **THEN** 更新对应 trace 的 status、total_tokens、total_duration_ms、files_changed、loop_count、completed_at

### Requirement: 每日汇总表存储

系统 SHALL 创建 `metrics_daily_summary` 表存储每日聚合数据。表结构包含：summary_date（DATE 主键）、dau、total_sessions、total_tokens、total_duration_seconds、active_projects、agent_tasks_completed、agent_tasks_failed、agent_first_pass_rate（DECIMAL(5,2)）、avg_tokens_per_task、top_skill、top_agent、skill_call_count、creates_stime、modified_stime、is_del。SHALL 使用 UNIQUE KEY 保证每天只有一条汇总记录。

#### Scenario: 每日汇总定时任务执行

- **WHEN** 定时任务在凌晨执行，聚合前一天所有 sessions 和 agent_traces
- **THEN** 生成一条 metrics_daily_summary 记录，使用 INSERT ON DUPLICATE KEY UPDATE 保证幂等

### Requirement: 所有表遵循项目数据库规范

所有表 SHALL 包含 creates_stime（默认当前时间）、modified_stime（自动更新）、is_del（逻辑删除）字段。主键 SHALL 使用 bigint(20) 自增。敏感字段（username）SHALL 存储加密版本。

#### Scenario: 记录插入时间戳自动填充

- **WHEN** 任何一条度量记录被插入
- **THEN** creates_stime 自动填充为当前时间，modified_stime 自动更新
