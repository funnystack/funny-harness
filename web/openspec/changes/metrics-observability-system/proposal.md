## Why

AI 编程工具落地后，团队 Token 成本是黑箱、使用渗透率不可见、Agent 执行质量无法追踪。没有度量体系，管理者无法回答"效果怎么样"、"钱花得值不值"、"Agent 靠不靠谱"三个核心问题。当前项目已有完整的 Harness 工程体系（Rules + Skills），但缺少度量层——这是从"可追踪"到"可观测"的关键一步。

## What Changes

- 新增 Claude Code Hooks 采集层：SessionStart / PreToolUse / Stop 三个 Hook，通过 Shell 脚本在开发者机器上透明采集会话数据
- 新增本地缓冲 + 定时上报机制：Shell 脚本写入本地 JSON 日志，Cron 定时批量 HTTPS 上报，失败重试不丢数据
- 新增 Spring Boot 收集服务端：接收上报数据、校验、存储、聚合分析
- 新增 MySQL 存储层：users、sessions、skill_usage、agent_traces、daily_summary 五张核心表
- 新增异常检测规则引擎：阈值告警（Token 超标、修正循环过多、DAU 突降、任务失败率突增）
- 新增自动报告生成：每日汇总、每周 Agent 效率周报、每月 ROI 报告
- 新增前端 Dashboard：日报/周报/月报三种看板，支持开发者、管理者、决策者三种角色视角
- 新增前端能力调用热力图：展示 Skill/Agent/Command 的调用频次和使用分布

## Capabilities

### New Capabilities

- `hooks-collection`: Claude Code Hooks 数据采集（SessionStart / PreToolUse / Stop），Shell 脚本实现，本地 JSON 日志缓冲
- `data-upload`: 本地数据定时批量上报（Cron 调度、HTTPS 批量传输、失败重试）
- `metrics-api`: 服务端数据收集、校验、存储、聚合的 REST API（Spring Boot）
- `metrics-storage`: MySQL 存储方案（五张核心表设计、索引策略、聚合查询优化）
- `anomaly-detection`: 异常检测规则引擎（阈值告警、即时通知、周报标注）
- `auto-report`: 自动报告生成（日报汇总、周报、月度 ROI 报告）
- `metrics-dashboard`: 前端看板（日报/周报/月报 + 三种角色视角 + 能力调用热力图）

### Modified Capabilities

（无现有 capability 需要修改）

## Impact

- **新增代码**：服务端 Java 代码（Controller/Service/DAO/Mapper），前端 Dashboard 工程，Shell 采集脚本
- **新增依赖**：前端框架（待设计确认），可能需要图表库（ECharts/Chart.js）
- **数据库**：新增 5 张表，需在 MySQL 中执行 DDL
- **开发者机器**：需要部署 Hooks 脚本和 Cron 任务，对开发者透明无侵入
- **安全影响**：上报数据包含用户名和项目路径，传输层必须 HTTPS，存储层需脱敏处理
