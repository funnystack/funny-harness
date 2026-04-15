## 1. 数据库与基础设施

- [x] 1.1 创建 MySQL DDL 脚本：metrics_raw_events、metrics_users、metrics_sessions、metrics_skill_usage、metrics_agent_traces、metrics_daily_summary、metrics_alerts、metrics_reports 八张表（DDL 见 design.md D4.0 ~ D4.7）
- [x] 1.2 在现有项目 Controller/Service/DAO 分层中新增 metrics 相关类和常量（遵循既有包结构，详见 design.md D7 目录结构）
- [x] 1.3 新增 application.yml 中 metrics 相关配置项（API Key、告警阈值、Cron 表达式）

## 2. Hooks 采集脚本（Shell）

- [x] 2.1 实现 SessionStart Hook 脚本：从 stdin JSON 读取 session_id/cwd/source/model，从 $USER/$(hostname)/$CLAUDE_PROJECT_DIR 获取用户/设备/项目信息，计算 user_id=SHA256($USER@hostname)，写入 ~/.claude/metrics/events.jsonl
- [x] 2.2 实现 PreToolUse Hook 脚本：从 stdin JSON 读取 tool_name/tool_input，提取 command（Bash 工具）或 skill_name（Skill 调用）或 agent_type（subagent），写入 events.jsonl
- [x] 2.3 实现 Stop Hook 脚本：记录 session_id/user_id/timestamp，第二阶段解析 last_assistant_message 提取 task_summary
- [x] 2.4 编写 Hooks 安装脚本：自动配置 settings.json 中的 Hooks 注册项（SessionStart/PreToolUse/Stop）

## 3. 数据上报机制（Shell）

- [x] 3.1 实现本地日志文件扫描和批量打包逻辑（Shell + jq，读 events.jsonl）
- [x] 3.2 实现 HTTPS POST 上报脚本，携带 X-Client-Id（user_id）和 X-Batch-Id（时间戳+随机数）
- [x] 3.3 实现失败重试逻辑：5xx/网络错误保留文件，4xx 移至 failed/ 目录
- [x] 3.4 实现已上报记录清理：成功后从未上报文件中移除已发送行
- [x] 3.5 实现无效记录过滤：缺少 event_type 或 timestamp 的移入 invalid.jsonl
- [x] 3.6 编写 Cron 定时任务安装/卸载脚本（5 分钟间隔）

## 4. 服务端 API — POST /api/metrics/collect（数据收集接口）

- [x] 4.1 实现 MetricsCollectorController：接收 POST /api/metrics/collect，从 HTTP 请求提取 client_ip（X-Forwarded-For 优先），逐条校验 event_type 非空 + timestamp 格式合法，合法记录写入 metrics_raw_events（processed=0，raw_payload 保留完整 JSON），返回 ApiResult<Integer>
- [x] 4.2 实现 API Key 认证拦截器：校验 X-API-Key 请求头，支持 Admin/Viewer 角色

## 5. 定时任务 T1 — 原始事件解析（每 1 分钟）

- [x] 5.1 实现原始事件解析定时任务：扫描 metrics_raw_events WHERE processed=0 LIMIT 500，按 event_type 路由处理：session_start → INSERT metrics_sessions + INSERT IF NOT EXISTS metrics_users；tool_use → UPDATE metrics_sessions 工具计数 + 判断是否写入 metrics_skill_usage；session_stop → UPDATE metrics_sessions ended_at/duration/tokens + 解析 task_summary 写入 metrics_agent_traces。逐条更新 processed=1，异常则 processed=2 + 记录 process_error

## 6. 定时任务 T2 — 每日汇总（每天 02:00）

- [x] 6.1 实现每日汇总定时任务：聚合前一天 metrics_sessions（COUNT DISTINCT user_id=dau, SUM tokens, SUM duration, COUNT sessions, COUNT DISTINCT project_name=active_projects），聚合 metrics_agent_traces（completed/failed 数、一次通过率 loop_count=1 占比、avg tokens），聚合 metrics_skill_usage（top_skill/top_agent/skill_call_count），INSERT ON DUPLICATE KEY UPDATE 写入 metrics_daily_summary

## 7. 定时任务 T3 — 异常检测（每 5 分钟）

- [x] 7.1 实现异常检测定时任务，5 条规则：①TOKEN_OVERUSE 查最近完成 traces，total_tokens > AVG(同类型30天)×5 生成 critical；②LOOP_EXCEEDED loop_count>3 生成 warning；③DAU_DROP 今日 vs 昨日 dau 低于 50% 生成 critical；④FAILURE_RATE 本周失败率>15% 生成 warning；⑤AGENT_STUCK status=running 且最近 Step 距今>5分钟 生成 critical。所有告警写入 metrics_alerts

## 8. 定时任务 T4 — 周报生成（每周一 08:00）

- [x] 8.1 实现周报生成定时任务：聚合上周 7 天 daily_summary 为周汇总，查上周 agent_traces 统计任务数/通过率/Token/修正循环（均带环比变化），查上周 critical 告警取异常任务 Top 3，生成"本周建议"（基于数据模式），同时写入 metrics_reports 的 content_json（供 API 查询）和 content_md（供 IM 推送）

## 9. 定时任务 T5 — 月报生成（每月 1 日 08:00）

- [x] 9.1 实现月报生成定时任务：聚合上月 30 天 daily_summary，计算 Agent 产出等效人力（任务数 × 平均人工耗时）、单任务成本对比（Agent Token 折人民币 vs 人工估算）、月度成本趋势、北极星指标趋势 + 围栏指标达标情况，同时生成 JSON 和 Markdown 两个版本写入 metrics_reports

## 10. 定时任务 T6 — 原始数据清理（每天 03:00）

- [x] 10.1 实现原始数据清理定时任务：DELETE FROM metrics_raw_events WHERE processed=1 AND received_at < NOW() - INTERVAL 90 DAY

## 11. Dashboard API — MetricsDashboardController（6 个接口）

- [x] 11.1 实现 GET /api/metrics/daily：查 daily_summary 取当日汇总，查 sessions 按小时聚合 Token 趋势、按项目聚合 Top 5、按用户聚合排行，查 skill_usage 聚合能力热力图，返回 DailyVO
- [x] 11.2 实现 GET /api/metrics/weekly：汇总 7 天 daily_summary 为周汇总，取 7 天 DAU 折线，查 skill_usage 7 天能力调用趋势，查 users 本周新增，对比使用前后效率数据，返回 WeeklyVO
- [x] 11.3 实现 GET /api/metrics/monthly：计算安装覆盖率（安装用户数/团队总人数），聚合 30 天 Token 成本（总量/人均/单会话），按项目×用户聚合热度矩阵，查 reports 取推广建议，返回 MonthlyVO
- [x] 11.4 实现 GET /api/metrics/agent/dashboard：今日任务数（COUNT traces WHERE today）、一次通过率（loop_count=1 占比）、平均单任务 Token、活跃 Agent 数（DISTINCT agent_type running）、30 天任务完成率趋势、按 agent_type 聚合利用率、按 Token 折人民币成本趋势，返回 AgentDashboardVO
- [x] 11.5 实现 GET /api/metrics/agent/trace/{traceId}：查 traces 取基本信息，从 raw_payload 解析 Step 链路（按时间排序），每个 Step 含 tool_name/duration_ms/tokens/status（颜色编码），关联 skill_usage 能力调用记录，返回 TraceDetailVO
- [x] 11.6 实现 GET /api/metrics/alerts：查 metrics_alerts 按 type/severity/日期范围过滤，支持 WebConsole 管理员查看告警历史，返回 List<AlertVO>

## 12. 数据存储层（DAO）

- [x] 12.1 创建八张表的 Entity 类（MetricsRawEventDO/MetricsUserDO/MetricsSessionDO/MetricsSkillUsageDO/MetricsAgentTraceDO/MetricsDailySummaryDO/MetricsAlertDO/MetricsReportDO），遵循项目 DO 命名和字段规范（bigint 主键 + creates_stime/modified_stime/is_del）
- [x] 12.2 创建对应的 MyBatis-Plus Mapper 接口（8 个）
- [x] 12.3 编写复杂查询的 Mapper XML（MetricsSessionMapper：按小时聚合 Token、按项目/用户聚合；MetricsSkillUsageMapper：能力热度排行、7 天趋势；MetricsAgentTraceMapper：一次通过率、成本分布；MetricsDailySummaryMapper：日期范围汇总）

## 13. 前端 Dashboard（静态 HTML）

- [x] 13.1 在 src/main/resources/static/ 下创建 Dashboard 目录结构（index.html、dashboard/、assets/），通过 CDN 引入 Vue 3 + Element Plus + ECharts + Axios
- [x] 13.2 实现 API Key 登录页（index.html）：输入框 + 调用后端验证接口 + localStorage 存储 + 页面跳转
- [x] 13.3 实现公共 JS 模块（common.js）：Axios 封装（自动携带 X-API-Key、401 跳转登录）+ 侧边栏导航布局
- [x] 13.4 实现公共图表模块（charts.js）：ECharts 折线图/饼图/热力图封装
- [x] 13.5 实现日报看板页（daily.html）：调用 GET /daily，渲染四个指标卡片 + Token 小时趋势图 + 项目分布饼图 + 用户排行表 + 能力热力图
- [x] 13.6 实现周报看板页（weekly.html）：调用 GET /weekly，渲染周汇总卡片 + DAU 趋势折线图 + 能力使用趋势图 + 新增用户表 + 效率评估
- [x] 13.7 实现月报看板页（monthly.html）：调用 GET /monthly，渲染覆盖率进度条 + 成本分析 + 项目热度矩阵 + 推广建议列表
- [x] 13.8 实现 Agent 运营看板页（agent/dashboard.html）：调用 GET /agent/dashboard，渲染四个指标卡片 + 任务完成率趋势 + Agent 利用率分布 + 成本趋势图
- [x] 13.9 实现 Agent Trace 链路详情页（agent/trace-detail.html）：调用 GET /agent/trace/{id}，渲染时间线流程图 + Step 节点（颜色编码）+ 点击展开详情面板

## 14. 集成测试与部署

- [x] 14.1 编写 Hooks 采集脚本的单元测试（Shell 测试框架）
- [x] 14.2 编写 POST /api/metrics/collect 接口的集成测试
- [x] 14.3 编写 6 个 Dashboard API 接口的集成测试
- [x] 14.4 编写 6 个定时任务的集成测试（使用 @Scheduled 测试策略）
- [x] 14.5 编写部署文档：Hooks 安装步骤、服务端部署步骤（前端随 Spring Boot 一起部署，无需独立构建）
