## Context

当前项目是一个 Spring Boot 单体应用，已有 Controller/Service/DAO 分层架构，使用 MyBatis-Plus 访问 MySQL。项目处于极早期阶段（2 个 commit），业务代码由代码生成器产出。

现有基础设施：

- Spring Boot 2.x + MyBatis-Plus
- MySQL 数据源
- Log4j2 日志
- 无全局异常处理、无前端工程

度量体系需要：

- 在开发者机器上通过 Claude Code Hooks 透明采集数据
- 服务端接收、存储、分析数据
- 前端 Dashboard 展示度量结果

## Goals / Non-Goals

**Goals:**

- 实现完整的 Hooks 数据采集层（SessionStart / PreToolUse / Stop），对开发者透明无侵入
- 实现本地缓冲 + 定时批量上报机制，保证数据不丢失
- 实现 Spring Boot 服务端数据收集 API，包含校验、存储、聚合
- 实现 MySQL 五张核心表（users, sessions, skill_usage, agent_traces, daily_summary）
- 实现异常检测规则引擎和自动报告生成
- 实现前端 Dashboard，支持日报/周报/月报三种看板和三种角色视角
- 支持两阶段度量：第一阶段管人（使用行为），第二阶段管 Agent（执行质量）

**Non-Goals:**

- 不做实时秒级监控（当前阶段定时聚合足够）
- 不做 ClickHouse/Redis 等高级存储方案（50 人以下 MySQL 够用）
- 不做自动评分模型训练（先人工评分积累数据）
- 不做 SSO/多租户（当前只服务单团队）
- 不做移动端适配

## Decisions

### D1: 采集端用 Shell 脚本

**选择**: Shell 脚本实现 Hooks 采集逻辑

**替代方案**:

- Python 脚本：文章推荐，处理 JSON 更方便，但需要开发者安装 Python 环境
- Node.js 脚本：与 Claude Code 同生态，但增加运行时依赖

**理由**: Shell 是所有 macOS/Linux 开发者机器自带的，零依赖。JSON 处理用 `jq`（macOS 上 `brew install jq` 一行搞定）。Hooks 的触发上下文已经是 JSON 格式，Shell + jq 的组合足够处理。Cron 也是系统自带的，不需要额外安装调度器。

### D1.1: 用户、设备、项目识别方案

**用户识别**：Hook 上下文中没有用户字段。脚本从系统环境变量 `$USER` 获取用户名，从 `$(hostname)` 获取机器名。生成 `user_id = SHA256("$USER@hostname")` 作为唯一标识。理由：同名用户在不同机器上不会冲突，同一用户在不同机器上可被关联（通过 username 查询）。

**设备识别**：Claude Code 是本地 CLI，Hook 里拿不到 IP。但上报到服务端时，服务端可从 HTTP 请求的 `X-Forwarded-For` 或 `request.getRemoteAddr()` 获取客户端 IP。IP + hostname 组合用于设备识别。

**项目识别**：Hook stdin JSON 的 `cwd` 字段包含当前工作目录，环境变量 `$CLAUDE_PROJECT_DIR` 包含项目根目录。从项目根目录提取最后一级目录名作为 project_name（如 `/Users/zhangsan/projects/harness` → `harness`）。完整路径只在上报时发送，服务端存储时只保留仓库名（脱敏）。

**替代方案**:

- git config user.email：更精确但依赖 git 仓库配置，不是所有项目都有
- OAuth 登录：过重，Claude Code CLI 场景不适合

| 信息         | 采集位置        | 来源                                 | 存储字段                     |
| ------------ | --------------- | ------------------------------------ | ---------------------------- |
| user_id      | Hook 脚本       | SHA256($USER + "@" + hostname)       | 所有表的 user_id             |
| username     | Hook 脚本       | $USER                                | metrics_users.username       |
| hostname     | Hook 脚本       | $(hostname)                          | metrics_users.hostname       |
| client_ip    | 服务端          | HTTP 请求 remoteAddr/X-Forwarded-For | metrics_raw_events.client_ip |
| project_name | Hook 脚本       | basename($CLAUDE_PROJECT_DIR)        | 所有表的 project_name        |
| session_id   | Hook stdin JSON | session_id 字段                      | metrics_sessions.session_id  |

### D2: 本地文件缓冲 + Cron 定时批量上报

**选择**: Hook 脚本写本地 JSON 文件（每事件一行），Cron 每 5 分钟批量 HTTPS POST 上报

**替代方案**:

- 实时 HTTP 上报：网络异常时阻塞 Claude Code 或丢数据
- SQLite 本地缓冲：比文件更结构化，但增加复杂度

**理由**: 开发者网络环境不可控（VPN 切换、代理断连）。本地文件写入是原子操作（`echo >> file`），速度快且不会阻塞主进程。Cron 定时批量上报减少网络请求次数，失败时文件保留下次重试。这是文章核心推荐方案。

### D3: 服务端继续使用现有 Spring Boot 单体架构

**选择**: 在现有 Spring Boot 项目中新增 metrics 模块

**替代方案**:

- 独立微服务：部署更灵活，但增加运维复杂度
- Serverless：按量付费，但调试困难

**理由**: 当前团队 50 人以下，单机 MySQL 完全够用。在现有项目中新增包结构（如 `metrics` 包），复用现有的分层架构和基础设施。等数据量真正上来再拆分。

### D4: MySQL 存储，五张核心表

**选择**: 8 张表（1 张原始事件表 + 7 张业务表），遵循项目数据库规范（bigint 主键、creates_stime/modified_stime/is_del 三件套）

数据流：**上报 → 落 raw 表 → 异步拆分到 domain 表 → 定时聚合到 summary 表**

```
客户端上报
    │
    ▼
┌─────────────────────┐
│ metrics_raw_events  │  ← 所有原始事件先落这里，JSON 原文存储
│  (原始事件表)         │
└─────────┬───────────┘
          │ 异步处理（@Async 或定时任务扫描）
          ▼
┌─────────────────────┐    ┌──────────────────────┐    ┌───────────────────┐
│ metrics_sessions    │    │ metrics_skill_usage  │    │ metrics_agent_    │
│  (会话表)            │    │  (能力调用表)         │    │   traces          │
└─────────────────────┘    └──────────────────────┘    └───────────────────┘
          │                          │                          │
          └──────────────┬───────────┘──────────────────────────┘
                         ▼
                ┌─────────────────────┐
                │ metrics_daily_      │  ← 凌晨定时聚合
                │   summary           │
                └─────────────────────┘
```

#### D4.0 metrics_raw_events — 原始事件表（上报入口）

```sql
CREATE TABLE metrics_raw_events (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_id VARCHAR(64) DEFAULT NULL COMMENT '上报批次 ID（来自客户端 X-Batch-Id）',
    client_id VARCHAR(64) NOT NULL COMMENT '客户端标识（SHA256($USER@hostname)）',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '客户端 IP（服务端从 HTTP 请求获取，支持 IPv6）',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型：session_start/tool_use/capability_use/session_stop',
    raw_payload JSON NOT NULL COMMENT '原始事件 JSON 报文，完整保留',
    processed TINYINT(1) DEFAULT 0 COMMENT '处理状态：0=未处理 1=已处理 2=处理失败',
    process_error VARCHAR(512) DEFAULT NULL COMMENT '处理失败原因',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
    creates_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_processed (processed, received_at),
    KEY idx_client (client_id, received_at),
    KEY idx_type (event_type, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原始事件表';
```

**关键设计点：**

- `raw_payload` 存完整 JSON 原文，不丢任何字段，方便后期重新解析
- `processed` 状态机：0 未处理 → 1 已处理 → 2 处理失败（失败的可人工排查后重置为 0 重跑）
- 收集 API 只做一件事：校验格式 → 写 raw 表 → 返回成功。**不做业务解析**
- 异步处理逻辑扫描 `processed=0` 的记录，拆分写入 domain 表（sessions/skill_usage/agent_traces）
- raw 表数据保留 90 天，定时清理（数据已在 domain 表，raw 只做审计用）

#### D4.1 metrics_users — 用户表

```sql
CREATE TABLE metrics_users (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id VARCHAR(64) NOT NULL COMMENT '用户唯一标识（SHA256($USER@hostname)）',
    username VARCHAR(128) NOT NULL COMMENT '系统用户名（$USER）',
    username_hash VARCHAR(64) NOT NULL COMMENT '用户名 SHA256，脱敏查询用',
    hostname VARCHAR(128) NOT NULL COMMENT '机器主机名（$(hostname)）',
    team VARCHAR(64) DEFAULT NULL COMMENT '所属团队',
    creates_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_team (team),
    KEY idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='度量用户表';
```

#### D4.2 metrics_sessions — 会话表（第一阶段核心）

```sql
CREATE TABLE metrics_sessions (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id VARCHAR(128) NOT NULL COMMENT '会话唯一标识',
    user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
    project_path VARCHAR(512) NOT NULL COMMENT '项目路径（只保留仓库名，脱敏）',
    started_at DATETIME NOT NULL COMMENT '会话开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '会话结束时间',
    duration_seconds INT DEFAULT 0 COMMENT '会话时长（秒）',
    total_tokens INT DEFAULT 0 COMMENT 'Token 总消耗',
    tool_read_count INT DEFAULT 0 COMMENT 'Read 工具调用次数',
    tool_edit_count INT DEFAULT 0 COMMENT 'Edit 工具调用次数',
    tool_bash_count INT DEFAULT 0 COMMENT 'Bash 工具调用次数',
    tool_grep_count INT DEFAULT 0 COMMENT 'Grep 工具调用次数',
    creates_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_started (user_id, started_at),
    KEY idx_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='度量会话表';
```

#### D4.3 metrics_skill_usage — 能力调用记录表（核心创新）

```sql
CREATE TABLE metrics_skill_usage (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id VARCHAR(128) NOT NULL COMMENT '关联会话',
    user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
    project_path VARCHAR(512) NOT NULL COMMENT '项目路径',
    capability_type VARCHAR(32) NOT NULL COMMENT '能力类型：skill/agent/command/slash_command',
    capability_name VARCHAR(128) NOT NULL COMMENT '能力名称，如 code-review / mvn test / /deploy',
    triggered_at DATETIME NOT NULL COMMENT '触发时间',
    tokens_consumed INT DEFAULT 0 COMMENT '消耗 Token 数',
    duration_ms INT DEFAULT 0 COMMENT '执行耗时（毫秒）',
    result_status VARCHAR(16) DEFAULT 'success' COMMENT '结果状态：success/failed/timeout',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '关联 agent_traces（可选）',
    creates_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_capability (capability_type, capability_name),
    KEY idx_user_project (user_id, project_path),
    KEY idx_triggered (triggered_at),
    KEY idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能力调用记录表';
```

四种 capability_type 的数据来源：

| 类型            | capability_name 示例         | 来源                                   |
| --------------- | ---------------------------- | -------------------------------------- |
| `skill`         | code-review, test-generation | PreToolUse Hook 上下文                 |
| `agent`         | 研发 Agent, 质量 Agent       | SessionStart/Stop Hook 会话元数据      |
| `command`       | mvn test, npm run build      | PreToolUse Hook Bash 工具 command 字段 |
| `slash_command` | /deploy, /review             | 输入层识别自定义命令                   |

#### D4.4 metrics_agent_traces — Agent Trace 表（第二阶段核心）

```sql
CREATE TABLE metrics_agent_traces (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id VARCHAR(128) NOT NULL COMMENT 'Trace 唯一标识',
    session_id VARCHAR(128) NOT NULL COMMENT '关联会话',
    agent_type VARCHAR(32) NOT NULL COMMENT 'Agent 类型：研发/质量/安全/测试',
    task_type VARCHAR(32) NOT NULL COMMENT '任务类型：编码/评审/测试/文档',
    task_desc TEXT COMMENT '任务描述摘要',
    status VARCHAR(16) NOT NULL DEFAULT 'running' COMMENT '状态：running/completed/failed/timeout',
    total_tokens INT DEFAULT 0 COMMENT '总 Token 消耗',
    total_duration_ms INT DEFAULT 0 COMMENT '总耗时（毫秒）',
    files_changed INT DEFAULT 0 COMMENT '变更文件数',
    loop_count INT DEFAULT 0 COMMENT '修正循环次数',
    quality_score INT DEFAULT NULL COMMENT '质量评分（人工打分，1-100）',
    revision_count INT DEFAULT 0 COMMENT '人工修正次数',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    creates_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trace_id (trace_id),
    KEY idx_agent_type (agent_type),
    KEY idx_task_type (task_type),
    KEY idx_started (started_at),
    KEY idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 执行追踪表';
```

#### D4.5 metrics_daily_summary — 每日汇总表（看板直接读）

```sql
CREATE TABLE metrics_daily_summary (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    summary_date DATE NOT NULL COMMENT '汇总日期',
    dau INT NOT NULL DEFAULT 0 COMMENT '日活跃用户数',
    total_sessions INT DEFAULT 0 COMMENT '总会话数',
    total_tokens BIGINT DEFAULT 0 COMMENT '总 Token 消耗',
    total_duration_seconds BIGINT DEFAULT 0 COMMENT '总时长（秒）',
    active_projects INT DEFAULT 0 COMMENT '活跃项目数',
    agent_tasks_completed INT DEFAULT 0 COMMENT 'Agent 任务完成数',
    agent_tasks_failed INT DEFAULT 0 COMMENT 'Agent 任务失败数',
    agent_first_pass_rate DECIMAL(5,2) DEFAULT NULL COMMENT 'Agent 一次通过率',
    avg_tokens_per_task INT DEFAULT NULL COMMENT '平均单任务 Token',
    top_skill VARCHAR(64) DEFAULT NULL COMMENT '当日最热 Skill',
    top_agent VARCHAR(64) DEFAULT NULL COMMENT '当日最热 Agent',
    skill_call_count INT DEFAULT 0 COMMENT 'Skill 总调用次数',
    creates_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_date (summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日汇总表';
```

#### D4.6 metrics_alerts — 告警表

```sql
CREATE TABLE metrics_alerts (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    alert_type VARCHAR(32) NOT NULL COMMENT '告警类型：TOKEN_OVERUSE/LOOP_EXCEEDED/DAU_DROP/FAILURE_RATE/AGENT_STUCK',
    severity VARCHAR(16) NOT NULL DEFAULT 'warning' COMMENT '严重级别：info/warning/critical',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '关联 Trace（可选）',
    details JSON DEFAULT NULL COMMENT '告警详情（JSON）',
    notified TINYINT(1) DEFAULT 0 COMMENT '是否已通知：0=未通知 1=已通知',
    creates_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_type_time (alert_type, creates_stime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='度量告警表';
```

#### D4.7 metrics_reports — 报告表

```sql
CREATE TABLE metrics_reports (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    report_type VARCHAR(16) NOT NULL COMMENT '报告类型：daily/weekly/monthly',
    report_date DATE NOT NULL COMMENT '报告日期',
    content_json JSON NOT NULL COMMENT '结构化报告内容（JSON）',
    content_md TEXT COMMENT 'Markdown 文本版本（IM 推送用）',
    creates_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_type_date (report_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='度量报告表';
```

#### 索引策略说明

| 表                    | 核心索引                             | 用途                         |
| --------------------- | ------------------------------------ | ---------------------------- |
| metrics_sessions      | `(user_id, started_at)`              | 查某用户某时间段的所有会话   |
| metrics_sessions      | `(started_at)`                       | 按时间范围聚合（日汇总任务） |
| metrics_skill_usage   | `(capability_type, capability_name)` | 能力热度排行查询             |
| metrics_skill_usage   | `(triggered_at)`                     | 按时间范围过滤               |
| metrics_agent_traces  | `(agent_type)`, `(task_type)`        | 按 Agent 类型/任务类型聚合   |
| metrics_daily_summary | `UNIQUE (summary_date)`              | 看板直接按日期查询，保证幂等 |

**理由**: 日处理上千次会话，MySQL 单表百万级数据无压力。看板直接读 daily_summary 聚合表，不查原始明细。skill_usage 表是核心创新，支持四种能力类型的统一追踪。所有表遵循项目规范（bigint 主键 + 三件套字段）。

### D5: 前端使用静态 HTML + Vue 3 + ECharts + Element Plus（CDN）

**选择**: Vue 3 + ECharts + Element Plus 通过 CDN 引入，页面作为静态 HTML 放在 `src/main/resources/static/` 下，Spring Boot 自动托管

**替代方案**:

- Vue 3 + Vite 独立前端工程：前后端分离更彻底，但多一个部署单元，对 Java 团队维护成本高
- React + Ant Design：生态更大，但团队以 Java 为主，Vue 上手更快
- 纯后端渲染（Thymeleaf）：简单但交互能力差，不适合图表密集的 Dashboard

**理由**: 静态 HTML 放 `resources/static` 是 Spring Boot 的内置能力，零配置直接访问。通过 CDN 引入 Vue 3、ECharts、Element Plus，不需要 Node.js 构建链路，Java 团队直接改 HTML 就能迭代。一个部署单元，一个服务进程，运维简单。

### D6: 三层指标体系设计

**选择**: 北极星指标 + 围栏指标 + 群星指标

- 北极星：有效交付吞吐量（人均月有效交付数）
- 围栏（4 个）：单任务 Token 消耗上限 > 5 倍均值、月度 Token 预算使用率 > 90%、修正循环次数 > 3、安全违规次数 > 0
- 群星：DAU、安装覆盖率、人均 Token、项目覆盖率、人均时长、Skill 热度、Agent 分布等

**理由**: 文章核心方法论。北极星定方向，围栏保底线，群星找机会。避免"为了度量而度量"。

### D7: 项目包结构设计

**选择**: 在现有项目的 Controller/Service/DAO 分层中直接新增 metrics 相关类，前端放 `src/main/resources/static/`

```
src/main/java/com/funny/harness/
├── web/controller/
│   ├── HealthController.java          # 现有
│   ├── MovieYinfansController.java    # 现有
│   ├── MetricsCollectorController.java  # 新增：数据收集 API
│   └── MetricsDashboardController.java  # 新增：Dashboard API
├── service/
│   ├── IMetricsCollectorService.java     # 新增：数据收集逻辑
│   ├── IMetricsAnalysisService.java      # 新增：分析聚合
│   ├── IMetricsDashboardService.java     # 新增：Dashboard 查询
│   └── IAnomalyDetectionService.java     # 新增：异常检测
├── service/impl/
│   ├── MetricsCollectorServiceImpl.java
│   ├── MetricsAnalysisServiceImpl.java
│   ├── MetricsDashboardServiceImpl.java
│   └── AnomalyDetectionServiceImpl.java
├── dao/entity/
│   ├── MetricsRawEventDO.java          # 新增：原始事件
│   ├── MetricsUserDO.java              # 新增
│   ├── MetricsSessionDO.java           # 新增
│   ├── MetricsSkillUsageDO.java        # 新增
│   ├── MetricsAgentTraceDO.java        # 新增
│   ├── MetricsDailySummaryDO.java      # 新增
│   ├── MetricsAlertDO.java             # 新增
│   └── MetricsReportDO.java            # 新增
├── dao/mapper/
│   ├── MetricsRawEventMapper.java      # 新增
│   ├── MetricsUserMapper.java          # 新增
│   ├── MetricsSessionMapper.java       # 新增
│   ├── MetricsSkillUsageMapper.java    # 新增
│   ├── MetricsAgentTraceMapper.java    # 新增
│   ├── MetricsDailySummaryMapper.java  # 新增
│   ├── MetricsAlertMapper.java         # 新增
│   └── MetricsReportMapper.java        # 新增
└── common/
    └── consts/
        └── MetricsConsts.java           # 新增：度量相关常量

src/main/resources/
├── static/                              # 新增：Dashboard 前端页面
│   ├── index.html                       # 登录/入口页
│   ├── dashboard/
│   │   ├── daily.html                   # 日报看板
│   │   ├── weekly.html                  # 周报看板
│   │   ├── monthly.html                 # 月报看板
│   │   └── agent/
│   │       ├── dashboard.html           # Agent 运营看板
│   │       └── trace-detail.html        # Trace 链路详情
│   └── assets/
│       ├── css/
│       └── js/
│           ├── common.js                # 公共 API 封装、路由守卫
│           └── charts.js                # ECharts 图表封装
├── mapper/                              # 现有 + 新增 Mapper XML
│   ├── MetricsSessionMapper.xml         # 新增
│   ├── MetricsSkillUsageMapper.xml      # 新增
│   ├── MetricsAgentTraceMapper.xml      # 新增
│   └── MetricsDailySummaryMapper.xml    # 新增
```

**理由**: web 目录本身就是整个项目的根，不需要再套一层 metrics 域。直接在现有的 Controller/Service/DAO 分层中新增类，符合项目既有规范，学习成本低。前端用 `resources/static` 托管静态 HTML，Spring Boot 零配置自带。

### D8: API 接口清单

共 2 个 Controller、7 个接口。所有接口统一返回 `ApiResult<T>`。

#### MetricsCollectorController（数据收集，1 个接口）

| #   | 方法 | 路径                   | 入参                                                                                       | 出参                          | 核心逻辑                                                                                                                                                                                                    |
| --- | ---- | ---------------------- | ------------------------------------------------------------------------------------------ | ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | POST | `/api/metrics/collect` | Header: `X-API-Key`, `X-Client-Id`, `X-Batch-Id`<br>Body: `List<RawEventDTO>`（JSON 数组） | `ApiResult<Integer>` 接收条数 | ① 从 HTTP 请求提取 client_ip（X-Forwarded-For 优先）<br>② 逐条校验 event_type 非空、timestamp 格式合法<br>③ 合法记录写入 metrics_raw_events（processed=0，raw_payload 保留完整 JSON）<br>④ 返回成功接收条数 |

#### MetricsDashboardController（Dashboard 查询，6 个接口）

| #   | 方法 | 路径                                 | 入参                                                             | 出参                          | 核心逻辑                                                                                                                                                                                                                                                                                                              |
| --- | ---- | ------------------------------------ | ---------------------------------------------------------------- | ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2   | GET  | `/api/metrics/daily`                 | Query: `date`（可选，默认今天）                                  | `ApiResult<DailyVO>`          | ① 查 metrics_daily_summary 取当日汇总<br>② 查 metrics_sessions 按小时聚合 Token 趋势<br>③ 查 metrics_sessions 按项目聚合 Top 5<br>④ 查 metrics_sessions 按用户聚合排行<br>⑤ 查 metrics_skill_usage 按能力聚合热力图数据                                                                                               |
| 3   | GET  | `/api/metrics/weekly`                | Query: `weekStart`（可选，默认本周一）                           | `ApiResult<WeeklyVO>`         | ① 汇总 7 天的 daily_summary 为周汇总<br>② 取 7 天 DAU 折线<br>③ 查 metrics_skill_usage 7 天能力调用趋势<br>④ 查 metrics_users 本周新增用户<br>⑤ 对比使用前后效率数据                                                                                                                                                  |
| 4   | GET  | `/api/metrics/monthly`               | Query: `month`（可选，默认本月）                                 | `ApiResult<MonthlyVO>`        | ① 计算 安装用户数/团队总人数 = 覆盖率<br>② 聚合 30 天 Token 成本（总量、人均、单会话）<br>③ 查 metrics_sessions 按项目×用户聚合热度矩阵<br>④ 查 metrics_reports 取本月月报中的推广建议                                                                                                                                |
| 5   | GET  | `/api/metrics/agent/dashboard`       | Query: `dateRange`（可选，默认近 30 天）                         | `ApiResult<AgentDashboardVO>` | ① 今日任务数：COUNT metrics_agent_traces WHERE DATE(started_at)=today<br>② 一次通过率：loop_count=1 的占比<br>③ 平均单任务 Token：AVG(total_tokens)<br>④ 活跃 Agent 数：COUNT DISTINCT agent_type WHERE status=running<br>⑤ 任务完成率 30 天趋势<br>⑥ 按 agent_type 聚合利用率分布<br>⑦ 按 Token 折算人民币的成本趋势 |
| 6   | GET  | `/api/metrics/agent/trace/{traceId}` | Path: `traceId`                                                  | `ApiResult<TraceDetailVO>`    | ① 查 metrics_agent_traces 取 Trace 基本信息<br>② 从 raw_payload 中解析 Step 链路（按时间排序）<br>③ 每个 Step 包含：tool_name、duration_ms、tokens、status（颜色编码）<br>④ 关联 metrics_skill_usage 中的能力调用记录                                                                                                 |
| 7   | GET  | `/api/metrics/alerts`                | Query: `type`（可选）、`severity`（可选）、`startDate`/`endDate` | `ApiResult<List<AlertVO>>`    | ① 查 metrics_alerts 按条件过滤<br>② 支持 WebConsole 管理员查看告警历史                                                                                                                                                                                                                                                |

#### 请求/响应示例

**POST /api/metrics/collect 请求体：**

```json
[
  {
    "event_type": "session_start",
    "timestamp": "2026-04-11T09:15:32+08:00",
    "session_id": "abc123",
    "username": "zhangsan",
    "hostname": "zhangsan-macbook",
    "user_id": "a1b2c3d4...",
    "project_name": "harness",
    "cwd": "/Users/zhangsan/projects/harness",
    "source": "startup",
    "model": "claude-sonnet-4-6"
  }
]
```

**GET /api/metrics/daily 响应体：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "date": "2026-04-11",
    "dau": 35,
    "dauChange": "+3",
    "totalTokens": 2100000,
    "totalTokensChange": "-5%",
    "totalSessions": 52,
    "avgDurationMinutes": 108,
    "hourlyTokens": [{"hour":"09:00","tokens":120000}, ...],
    "projectTop5": [{"name":"harness","sessions":18}, ...],
    "userRanking": [{"username":"zhang***","sessions":8,"tokens":180000}, ...],
    "capabilityHeatmap": [{"type":"skill","name":"code-review","count":45}, ...]
  }
}
```

### D9: 定时任务清单

共 6 个定时任务，均使用 Spring `@Scheduled` 注解。

| #   | 任务名       | Cron 表达式                    | 扫描表                                                            | 写入表                                                                           | 核心逻辑                                                                                                                                                                                                                                                                                                                                                                                                                       |
| --- | ------------ | ------------------------------ | ----------------------------------------------------------------- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1   | 原始事件解析 | `0 */1 * * *`（每 1 分钟）     | metrics_raw_events                                                | metrics_sessions<br>metrics_skill_usage<br>metrics_agent_traces<br>metrics_users | ① WHERE processed=0 LIMIT 500<br>② 按 event_type 路由：<br>　- session_start → INSERT metrics_sessions + INSERT IF NOT EXISTS metrics_users<br>　- tool_use → UPDATE metrics_sessions 工具计数 + 判断是否写入 metrics_skill_usage<br>　- session_stop → UPDATE metrics_sessions ended_at/duration/tokens + 解析 task_summary 写入 metrics_agent_traces<br>③ 逐条更新 processed=1，异常则 processed=2 + 记录 process_error      |
| 2   | 每日汇总     | `0 2 * * *`（每天 02:00）      | metrics_sessions<br>metrics_skill_usage<br>metrics_agent_traces   | metrics_daily_summary                                                            | ① 聚合前一天的 sessions：COUNT DISTINCT user_id=dau, SUM tokens, SUM duration, COUNT sessions<br>② COUNT DISTINCT project_name=active_projects<br>③ 聚合 agent_traces：completed/failed 数、一次通过率（loop_count=1）、avg tokens<br>④ 聚合 skill_usage：top_skill/top_agent/skill_call_count<br>⑤ INSERT ON DUPLICATE KEY UPDATE 保证幂等                                                                                    |
| 3   | 异常检测     | `0 */5 * * *`（每 5 分钟）     | metrics_agent_traces<br>metrics_daily_summary<br>metrics_sessions | metrics_alerts                                                                   | 5 条规则逐条检测：<br>① **TOKEN_OVERUSE**: 查最近完成的 traces，total_tokens > AVG(同类型 30 天) × 5 的生成 critical 告警<br>② **LOOP_EXCEEDED**: loop_count > 3 的生成 warning 告警<br>③ **DAU_DROP**: 查今日 vs 昨日 dau（从 daily_summary），低于 50% 生成 critical 告警<br>④ **FAILURE_RATE**: 查本周失败率 > 15% 生成 warning 告警<br>⑤ **AGENT_STUCK**: status=running 且最近 Step 时间距今 > 5 分钟的生成 critical 告警 |
| 4   | 周报生成     | `0 8 ? * MON`（每周一 08:00）  | metrics_daily_summary<br>metrics_agent_traces<br>metrics_alerts   | metrics_reports                                                                  | ① 聚合上周 7 天 daily_summary 为周汇总<br>② 查上周 agent_traces 统计任务数/通过率/Token/修正循环<br>③ 查上周 critical 告警取异常任务 Top 3<br>④ 生成"本周建议"（基于数据模式，如遗漏场景占比高则建议 Prompt 增加边界条件检查）<br>⑤ 同时生成 JSON 和 Markdown 两个版本                                                                                                                                                         |
| 5   | 月报生成     | `0 8 1 * ?`（每月 1 日 08:00） | metrics_daily_summary<br>metrics_agent_traces<br>metrics_users    | metrics_reports                                                                  | ① 聚合上月 30 天 daily_summary<br>② 计算 Agent 产出等效人力（任务数 × 平均人工耗时）<br>③ 单任务成本对比（Agent Token 折人民币 vs 人工成本估算）<br>④ 月度成本趋势（按天）<br>⑤ 北极星指标趋势 + 围栏指标达标情况<br>⑥ 同时生成 JSON 和 Markdown 两个版本                                                                                                                                                                      |
| 6   | 原始数据清理 | `0 3 * * *`（每天 03:00）      | metrics_raw_events                                                | —                                                                                | ① DELETE WHERE processed=1 AND received_at < NOW() - INTERVAL 90 DAY<br>② 已处理的 raw 数据超过 90 天的清理，domain 表保留不动                                                                                                                                                                                                                                                                                                 |

#### 执行依赖关系

```
T1(事件解析, 每1min)
  ↓ 产出 domain 表数据
T2(日汇总, 每天02:00) ← 依赖 T1 已将前一天数据处理完
  ↓ 产出 daily_summary
T3(异常检测, 每5min) ← 可与 T1/T2 并行，但 T2 完成后 DAU_DROP 检测更准确
T4(周报, 周一08:00) ← 依赖 T2 已将上周数据汇总完
T5(月报, 1日08:00) ← 依赖 T2 已将上月数据汇总完
T6(清理, 每天03:00) ← 在 T2 之后执行，确保日汇总不丢数据
```

## Risks / Trade-offs

### [数据安全风险] → 传输层 HTTPS + 存储层脱敏

上报数据包含用户名和项目路径。传输层强制 HTTPS。服务端存储时对用户名做 hash 处理，项目路径只保留仓库名不保留完整路径。

### [上报失败风险] → 本地保留 + 下次重试

Cron 脚本只删除已确认上报成功的日志文件。POST 失败时文件原封不动保留，下次重试。极端情况下数据延迟上报但不丢失。

### [性能风险] → 聚合表 + 索引优化

看板查询直接读 daily_summary 聚合表，不查原始明细。原始表建立时间索引和用户索引。当日均 Trace 超过 10,000 条（约 200+ 人团队）时再考虑 ClickHouse。

### [前端工程独立部署] → 静态 HTML 单体部署

前端 Dashboard 作为静态 HTML 放在 `src/main/resources/static/`，由 Spring Boot 直接托管。好处：一个部署单元、一个服务进程、无需 Node.js 构建链路。代价：大型 SPA 场景下 CDN 方案不如 Vite 构建灵活，但当前 Dashboard 页面数量有限（5-6 个页面），静态 HTML 完全够用。

### [采集粒度权衡] → Step 输入输出只保留摘要

Agent Trace 的每一步记录完整输入输出数据量过大。只保留前 200 字符摘要，完整数据通过 Git 历史回溯。
