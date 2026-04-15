## ADDED Requirements

### Requirement: 单任务 Token 消耗超标告警

系统 SHALL 检测单任务 Token 消耗超过近 30 天同类型任务平均值 5 倍的情况。检测到异常时 SHALL 记录告警日志并触发即时通知（通知方式为配置化，默认写日志，可扩展为 IM 通知）。

#### Scenario: 编码任务 Token 超标

- **WHEN** 一个编码类 Agent 任务消耗了 180,000 Token，而近 30 天编码任务平均消耗为 25,000 Token（比值 7.2 倍）
- **THEN** 系统生成告警记录：alert_type=TOKEN_OVERUSE、trace_id、actual=180000、threshold=125000、ratio=7.2

#### Scenario: Token 在正常范围内

- **WHEN** 一个编码类 Agent 任务消耗了 30,000 Token，平均 25,000 Token（比值 1.2 倍）
- **THEN** 不触发告警

### Requirement: 修正循环次数超标告警

系统 SHALL 检测 Agent 修正循环次数超过 3 次的情况。超过阈值时 SHALL 标记该 Trace 为异常，并触发即时通知。

#### Scenario: Agent 修正循环过多

- **WHEN** 一个 Agent 任务的 loop_count=8，超过阈值 3
- **THEN** 生成告警记录：alert_type=LOOP_EXCEEDED、trace_id、actual=8、threshold=3

### Requirement: 日活突降告警

系统 SHALL 在每日汇总时检测 DAU 下降幅度。当日 DAU 低于昨日 DAU 的 50% 时 SHALL 触发即时告警。

#### Scenario: DAU 突降

- **WHEN** 昨日 DAU 为 40，今日 DAU 为 15（低于 40 × 50% = 20）
- **THEN** 生成告警记录：alert_type=DAU_DROP、yesterday=40、today=15、ratio=0.375

#### Scenario: DAU 正常波动

- **WHEN** 昨日 DAU 为 40，今日 DAU 为 35（高于 40 × 50% = 20）
- **THEN** 不触发告警

### Requirement: 任务失败率突增告警

系统 SHALL 每周检测 Agent 任务失败率。周失败率超过 15% 时 SHALL 在周报中标注。

#### Scenario: 周失败率超标

- **WHEN** 本周 Agent 任务 50 个，其中 9 个失败，失败率 18%
- **THEN** 在周报中标注：agent_failure_rate_alert=true，附带失败任务 Top 3 列表

### Requirement: Agent 无响应告警

系统 SHALL 检测 Agent 任务执行超过 5 分钟无新 Step 产出的情况。检测到时 SHALL 触发即时告警。

#### Scenario: Agent 执行卡住

- **WHEN** 一个 Agent 任务最近一个 Step 距今已超过 5 分钟，且任务状态仍为 running
- **THEN** 生成告警记录：alert_type=AGENT_STUCK、trace_id、last_step_time、elapsed_minutes

### Requirement: 告警记录持久化

所有告警记录 SHALL 存储在 `metrics_alerts` 表中。表结构包含：自增主键 id、alert_type（VARCHAR(32)）、severity（ENUM: info/warning/critical）、trace_id（可选）、details（JSON）、notified（BOOLEAN，默认 false）、created_at。SHALL 建立 (alert_type, created_at) 索引。

#### Scenario: 告警写入数据库

- **WHEN** 检测到 Token 超标告警
- **THEN** 在 metrics_alerts 表插入一条记录，severity=critical，notified=false
