## ADDED Requirements

### Requirement: 每日自动汇总任务

系统 SHALL 在每天凌晨 02:00 执行定时任务，从前一天的所有 metrics_sessions、metrics_skill_usage、metrics_agent_traces 中聚合生成一条 metrics_daily_summary 记录。聚合计算 SHALL 包括：DAU（唯一 user_id 数）、总会话数、总 Token、总时长、活跃项目数、Agent 任务完成/失败数、Agent 一次通过率、平均单任务 Token、Top Skill、Top Agent。

#### Scenario: 正常日汇总

- **WHEN** 定时任务在 02:00 执行，前一天有 45 个会话、38 个唯一用户、5 个活跃项目
- **THEN** 生成 metrics_daily_summary 记录：dau=38, total_sessions=45, active_projects=5，其余字段按聚合规则计算

#### Scenario: 前一天无数据

- **WHEN** 前一天无任何上报数据
- **THEN** 生成一条 metrics_daily_summary 记录，所有数值字段为 0，避免看板查询 null

#### Scenario: 汇总任务幂等执行

- **WHEN** 汇总任务因异常重复执行同一天的数据
- **THEN** 使用 INSERT ON DUPLICATE KEY UPDATE 覆盖已有记录，保证幂等

### Requirement: 每周 Agent 效率周报自动生成

系统 SHALL 每周一早上 08:00 自动生成上周的 Agent 效率周报。周报 SHALL 包含：总任务数（环比变化）、一次通过率（环比变化）、平均单任务 Token（环比变化）、修正循环平均次数（环比变化）、异常任务 Top 3（含 trace_id、异常原因）、本周建议（基于数据模式自动生成）。

#### Scenario: 正常周报生成

- **WHEN** 上周 Agent 执行了 47 个任务，上周 38 个
- **THEN** 生成周报包含：总任务数 47（+23.7%），一次通过率、Token、修正循环均带环比变化

#### Scenario: 本周无异常任务

- **WHEN** 上周所有 Agent 任务均在正常范围内
- **THEN** 周报中异常任务 Top 3 部分显示"本周无异常任务"

### Requirement: 月度 ROI 报告自动生成

系统 SHALL 每月 1 日自动生成上月 ROI 报告。报告 SHALL 包含：Agent 产出等效人力（任务数 × 平均人工耗时）、单任务成本对比（Agent vs 人工）、月度成本趋势、北极星指标趋势（有效交付吞吐量）、围栏指标达标情况。

#### Scenario: 月度报告生成

- **WHEN** 上月 Agent 完成 120 个编码任务，平均人工耗时 4 小时，Agent 单任务成本 ¥12
- **THEN** 报告显示：等效人力 60 工程师日，单任务成本比 1:67，附趋势图表

### Requirement: 报告输出为结构化数据

所有报告 SHALL 以结构化 JSON 存储在数据库中（通过 metrics_reports 表），同时提供 Markdown 文本版本供 IM 发送。metrics_reports 表结构包含：自增主键 id、report_type（ENUM: daily/weekly/monthly）、report_date、content_json（JSON）、content_md（TEXT）、created_at。

#### Scenario: 周报存储和获取

- **WHEN** 周报生成完成
- **THEN** 同时存储 JSON 版本（供 API 查询）和 Markdown 版本（供 IM 推送）
