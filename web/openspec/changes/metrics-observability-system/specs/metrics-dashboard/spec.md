## ADDED Requirements

### Requirement: 日报看板

系统 SHALL 提供日报看板页面，展示当日团队使用概览。页面 SHALL 包含：顶部四个核心指标卡片（DAU、Token 消耗、会话总数、人均时长，每个卡片显示环比变化）、Token 消耗按小时趋势图（ECharts 折线图）、项目使用分布 Top 5（ECharts 饼图）、用户活跃排行（表格）、能力调用热力图（ECharts 热力图，展示 Skill/Agent/Command 今日调用频次）。

#### Scenario: 管理者查看今日日报

- **WHEN** 管理者打开日报看板页面
- **THEN** 页面展示今日 DAU=35（较昨日 +3）、Token=2.1M（较昨日 -5%）、会话=52、人均时长=1.8h，以及下方四个图表模块

#### Scenario: 点击用户排行中的某用户

- **WHEN** 管理者在用户排行中点击某用户
- **THEN** 展开该用户的详细使用数据：今日会话数、Token、工具分布、使用的 Skill 列表

### Requirement: 周报看板

系统 SHALL 提供周报看板页面，展示本周趋势。页面 SHALL 包含：周汇总指标（周活跃人数、周 Token、周会话、周人均时长，标注目标值和达成率）、DAU 日活趋势图（周一到周六折线图）、能力使用趋势图（本周每天各 Skill/Agent 调用量变化）、新增安装用户列表、使用效率提升评估（对比使用前后数据）。

#### Scenario: 管理者查看本周周报

- **WHEN** 管理者打开周报看板页面
- **THEN** 页面展示本周活跃人数 42（目标 45，达成率 93%），DAU 趋势显示周三有下降

#### Scenario: 发现 Skill 调用量异常下降

- **WHEN** 周报显示 code-review Skill 周三从日均 20 次掉到 2 次
- **THEN** 管理者可点击该数据点查看周三的详细日志，定位原因

### Requirement: 月报看板

系统 SHALL 提供月报看板页面，展示月度 ROI。页面 SHALL 包含：安装覆盖率进度条、Token 消耗成本分析（总成本、人均成本、单会话成本、预算使用率）、项目使用热度矩阵（每个项目的高/中/低频使用人数表格）、自动生成的推广建议列表。

#### Scenario: 老板查看月度 ROI

- **WHEN** 老板打开月报看板
- **THEN** 页面展示安装覆盖率 72%、人均月成本 ¥60、等效人力 60 工程师日、Agent vs 人工成本比 1:67

#### Scenario: 查看项目热度矩阵

- **WHEN** 管理者查看项目热度矩阵
- **THEN** 矩阵显示每个项目的使用频次分类（绿色=高频、黄色=中频、红色=低频），一眼看出哪些项目深度接入、哪些还没动

### Requirement: Agent 运营看板

系统 SHALL 提供 Agent 运营看板页面，面向管理者。页面 SHALL 包含：顶部四个指标卡片（今日任务数、一次通过率、平均单任务成本、活跃 Agent 数）、任务完成率 30 天趋势图、各类 Agent 利用率分布、成本趋势图（按天/周/月切换）。

#### Scenario: 管理者查看 Agent 运营状态

- **WHEN** 管理者打开 Agent 运营看板
- **THEN** 页面展示今日任务 23 个、一次通过率 72%、平均成本 ¥15、活跃 Agent 3 个

### Requirement: Agent Trace 链路详情页

系统 SHALL 提供 Agent Trace 链路详情页面，面向开发者。页面 SHALL 以时间线/流程图形式展示单次 Agent 任务的完整执行过程。每个 Step 显示为一个节点，标注工具名称、耗时、Token 消耗。绿色表示成功，红色表示失败，黄色表示重试。点击任意 Step SHALL 展开输入输出摘要（前 200 字符）。

#### Scenario: 开发者复盘 Agent 任务

- **WHEN** 开发者打开 Trace T-20260315-0042 的详情页
- **THEN** 看到时间线：Step1 Read(0.8s, 1.2K tokens) → Step2 Read(1.2s, 2.8K tokens) → Step3 Grep(0.6s, 1.5K tokens) → Step4 Edit(8.2s, 9.4K tokens) → Step5 Bash(3.1s, 1.6K tokens) → Step6 Edit(9.5s, 1.7K tokens)，Step4 为绿色，Step6 为黄色（重试）

#### Scenario: 点击 Step 查看详情

- **WHEN** 开发者点击 Step4 Edit 节点
- **THEN** 展开面板显示：输入文件路径、修改摘要（前 200 字符）、输出结果摘要

### Requirement: 前端技术栈和工程结构

Dashboard 前端 SHALL 使用 Vue 3 + ECharts + Element Plus + Axios，全部通过 CDN 引入。页面 SHALL 作为静态 HTML 放在 `src/main/resources/static/` 目录下，由 Spring Boot 自动托管。SHALL 支持响应式布局（桌面端为主）。SHALL 使用 Axios 作为 HTTP 客户端，统一封装 API 请求和错误处理。不需要 Node.js 构建链路。

#### Scenario: 前端随 Spring Boot 一起部署

- **WHEN** Spring Boot 应用启动在 8080 端口
- **THEN** Dashboard 页面可通过 `http://localhost:8080/index.html` 直接访问，无需独立的前端服务

### Requirement: Dashboard 访问认证

Dashboard 页面 SHALL 要求登录后访问。登录方式为 API Key 输入（简单方案，非完整用户系统）。登录后 API Key 存储在 localStorage 中，每次请求自动携带。未登录访问任意页面 SHALL 重定向到登录页。

#### Scenario: 未登录访问看板

- **WHEN** 用户直接访问 `/dashboard/daily` 但未携带有效 API Key
- **THEN** 页面重定向到登录页，提示输入 API Key

#### Scenario: API Key 验证通过

- **WHEN** 用户在登录页输入有效的 API Key
- **THEN** 验证通过后跳转到日报看板，后续请求自动携带 API Key
