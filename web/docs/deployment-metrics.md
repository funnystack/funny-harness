# Metrics Observability System 部署文档

## 1. 架构概览

```
Claude Code 客户端                    服务端（Spring Boot）
┌─────────────────┐                  ┌──────────────────────┐
│ Hooks 采集脚本   │                  │ CollectorController  │
│  - SessionStart  │  HTTPS POST     │  POST /collect       │
│  - PreToolUse    │ ──────────────▶ │  → metrics_raw_events│
│  - Stop          │  (5min cron)    │                      │
│  events.jsonl    │                  │ 定时任务 T1-T6       │
│  upload-metrics  │                  │  → 解析/汇总/告警     │
└─────────────────┘                  │                      │
                                     │ DashboardController  │
                                     │  GET /daily,weekly.. │
                                     │  → 静态 HTML 看板    │
                                     └──────────────────────┘
```

## 2. 服务端部署

### 2.1 数据库初始化

```bash
# 执行 DDL 脚本创建 8 张表
mysql -u root -p harness < sql/metrics_ddl.sql
```

### 2.2 配置项

在 `application.yml` 中配置以下项：

```yaml
metrics:
  api-keys:
    admin: your-admin-api-key
    viewer: your-viewer-api-key
  alert:
    token-overuse-ratio: 5
    loop-exceeded-threshold: 3
    dau-drop-ratio: 0.5
    failure-rate-threshold: 0.15
    agent-stuck-minutes: 5
  token-price-per-k: 0.03
  manual-cost-per-hour: 200
  raw-retention-days: 90
```

### 2.3 构建与启动

```bash
mvn clean package -DskipTests
java -jar target/web.jar
```

服务启动后：

- 数据收集接口：`POST /api/metrics/collect`
- Dashboard 看板：`GET /index.html`（浏览器访问）
- 静态资源随 Spring Boot 一起部署，无需独立构建

### 2.4 验证部署

```bash
# 验证数据收集接口
curl -X POST http://localhost:8080/api/metrics/collect \
  -H "Content-Type: application/json" \
  -d '[{"event_type":"session_start","timestamp":"2026-04-11T10:00:00Z","session_id":"test"}]'

# 验证 Dashboard（浏览器访问）
open http://localhost:8080/index.html
```

## 3. Hooks 客户端安装

Hooks 脚本已集成到项目 `hooks/` 目录，通过 `install.sh` 统一安装。

### 3.1 一键安装（推荐）

```bash
# 在项目根目录执行 install.sh
# 脚本会自动完成以下步骤：
#   1. 复制 hooks 脚本到 ~/.claude/hooks/script/
#   2. 合并 hooks/metrics.json 等配置到 ~/.claude/settings.json
#   3. 可选安装 Cron 定时上报任务
bash install.sh
```

### 3.2 手动安装

```bash
# 1. 复制 hooks 脚本
mkdir -p ~/.claude/hooks/script/metrics
cp hooks/script/metrics/*.sh ~/.claude/hooks/script/metrics/
chmod +x ~/.claude/hooks/script/metrics/*.sh

# 2. 将 metrics.json 中的 hooks 配置合并到 settings.json（需 jq）
# 或手动将 hooks/metrics.json 中的内容添加到 ~/.claude/settings.json 的 hooks 字段
```

安装完成后，`~/.claude/settings.json` 中会注册：

- `SessionStart` → `hooks/script/metrics/hook-session-start.sh`
- `PreToolUse` → `hooks/script/metrics/hook-pre-tool-use.sh`
- `Stop` → `hooks/script/metrics/hook-stop.sh`

### 3.3 配置上报地址

```bash
# 设置环境变量（添加到 ~/.zshrc 或 ~/.bashrc）
export METRICS_SERVER="https://your-server.com"
export METRICS_API_KEY="your-admin-api-key"
```

### 3.4 安装定时上报

```bash
# install.sh 第八步会提示安装，也可手动执行：
bash ~/.claude/hooks/script/metrics/install-cron.sh install

# 查看日志
tail -f ~/.claude/metrics/upload.log
```

### 3.5 卸载

```bash
# 卸载 Cron
bash ~/.claude/hooks/script/metrics/install-cron.sh uninstall

# 卸载 Hooks（从 settings.json 中移除 metrics 相关条目）
# 手动编辑 ~/.claude/settings.json，删除 SessionStart/PreToolUse/Stop 中的 metrics 条目
```

## 4. 数据流说明

| 阶段 | 存储                                              | 保留时间         |
| ---- | ------------------------------------------------- | ---------------- |
| 采集 | `~/.claude/metrics/events.jsonl`                  | 上报后清理       |
| 上报 | 服务端 `metrics_raw_events`                       | 90 天（T6 清理） |
| 解析 | `metrics_sessions/users/skill_usage/agent_traces` | 永久             |
| 汇总 | `metrics_daily_summary`                           | 永久             |
| 告警 | `metrics_alerts`                                  | 永久             |
| 报告 | `metrics_reports`                                 | 永久             |

## 5. 定时任务说明

| 任务            | Cron          | 说明                                      |
| --------------- | ------------- | ----------------------------------------- |
| T1 原始事件解析 | `*/1 * * * *` | 每分钟扫描 raw 表，按 event_type 路由处理 |
| T2 每日汇总     | `0 2 * * *`   | 每天凌晨 2 点聚合前一天数据               |
| T3 异常检测     | `*/5 * * * *` | 每 5 分钟检测 5 类异常规则                |
| T4 周报生成     | `0 8 ? * MON` | 每周一早 8 点生成周报                     |
| T5 月报生成     | `0 8 1 * ?`   | 每月 1 日早 8 点生成月报                  |
| T6 原始数据清理 | `0 3 * * *`   | 每天凌晨 3 点清理 90 天前的已处理数据     |

## 6. 前端看板页面

| 页面       | URL                                              | 说明                                |
| ---------- | ------------------------------------------------ | ----------------------------------- |
| 登录       | `/index.html`                                    | API Key 认证                        |
| 日报       | `/dashboard/daily.html`                          | DAU/Token 趋势/项目分布/用户排行    |
| 周报       | `/dashboard/weekly.html`                         | 7 天 DAU 趋势/能力调用趋势/新增用户 |
| 月报       | `/dashboard/monthly.html`                        | 覆盖率/成本分析/项目热度矩阵        |
| Agent 运营 | `/dashboard/agent/dashboard.html`                | 任务数/通过率/利用率/成本趋势       |
| Trace 详情 | `/dashboard/agent/trace-detail.html?traceId=xxx` | 执行链路时间线                      |
| 告警历史   | `/dashboard/alerts.html`                         | 告警过滤查询                        |
