# funny-harness

中文文档|[英文文档](README.md)

AI Agent 工程基建项目，5 分钟完成团队环境标准化搭建。

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-2.0.0-green.svg)](version.json)

## 什么是 funny-harness

funny-harness 是一套面向 AI Agent 的工程基建方案。它解决的核心问题是：如何让 Claude（或其他 AI 编程助手）在团队协作中产出可靠、一致的代码，而不是靠反复调 prompt 碰运气。

它提供知识库搭建、变更管理、自动化验证和自进化能力，把 AI 协作从"个人手艺"变成"团队标准"。

## 为什么需要 funny-harness

一句话：环境设计的投入回报远高于 prompt 调优。

一套好的 Harness 能让普通模型产出可靠的代码，而没有 Harness 的顶级模型照样会在同样的坑里反复栽。funny-harness 围绕三个支柱设计：

**可读性** -- AGENTS.md 做导航地图，详细知识放 docs/，渐进披露，不全量注入上下文窗口。

**防御机制** -- 硬约束（hooks、permissions）优先于软约束（提示词）。状态机锁定执行阶段，写入前预验证，失败循环检测（3 次自动停止）。

**反馈回路** -- 实现和评审分离（不同 Agent），错误经验持久化，Critic 到 Refiner 的自进化循环，重复模式编译成脚本。

## 快速开始

### 前置条件

- macOS 或 Linux
- 已安装 [jq](https://stedolan.github.io/jq/)（用于 hooks 配置合并）

### 安装

```bash
git clone https://github.com/funny2048/funny-harness.git
cd funny-harness
bash install.sh
```

安装脚本会引导你完成 7 个步骤：

1. 输入用户名
2. 复制团队级 CLAUDE.md 到系统目录（macOS: `/Library/Application Support/ClaudeCode/CLAUDE.md`，Linux: `/etc/claude-code/CLAUDE.md`）
3. 复制用户级 CLAUDE.md 到 `~/.claude/CLAUDE.md`，替换模板中的用户名
4. 安装 hooks（合并 JSON 配置到 `~/.claude/settings.json`）
5. 选择语言规则（golang / java / python / typescript，可多选）
6. 安装 rules 到 `~/.claude/rules/`（common 必装 + 选装语言规则）
7. 安装 skills 到 `~/.claude/skills/`

安装完成后，Claude Code 启动时会自动加载这些规则、技能和安全防护。

## 项目结构

```
funny-harness/
├── install.sh                  # 一键安装脚本
├── agents/                     # 代理角色定义
│   ├── reviewer.md             # 独立只读审查员
│   ├── architect.md            # 架构审查
│   └── executor.md             # 任务执行者
├── hooks/                      # 安全防护钩子
│   ├── check-dangerous.json    # 危险操作检测配置
│   ├── protect-files.json      # 敏感文件保护配置
│   ├── ensure_change_context.py# OpenSpec 变更上下文检查
│   ├── prettier-lint.json      # 代码格式检查
│   ├── run-test.json           # 测试执行钩子
│   └── script/                 # 钩子脚本
├── skills/                     # 专项技能
│   ├── harness-init-java/      # Java 项目初始化
│   ├── review-summary/         # 变更审查报告
│   ├── spring-architecture-review/ # Spring 分层审查
│   ├── sql-risk-review/        # SQL 风险审查
│   └── openspec-bridge/        # OpenSpec 配置桥接
├── rules/                      # 编码规则
│   ├── common/                 # 通用规则（必装）
│   ├── golang/                 # Go 语言规则
│   ├── java/                   # Java 语言规则
│   ├── python/                 # Python 语言规则
│   └── typescript/             # TypeScript 语言规则
├── templates/                  # 模板文件
│   ├── CLAUDE-team-template.md # 团队级配置模板
│   ├── CLAUDE-user-template.md # 用户级配置模板
│   ├── exec-plan-template.md   # 执行计划模板
│   └── skill-bundle.json       # Skill 套装清单
├── openspec/                   # OpenSpec 工作流目录
│   ├── config.yaml             # 项目配置
│   ├── changes/                # 变更记录
│   └── specs/                  # 规格文档
├── scripts/                    # 验证脚本
│   ├── check-docs.sh           # 文档一致性检查
│   ├── validate.sh             # 统一验证管道
│   ├── lint-deps.sh            # 依赖方向检查
│   └── lint-quality.sh         # 代码质量检查
├── AGENTS.md                   # Agent 导航地图
├── harness-design.md           # 设计哲学
└── version.json                # 版本信息
```

## 核心组件

### Skills

5 个专项技能，每个解决一个具体的工程问题。

| Skill | 说明 | 触发方式 |
|-------|------|----------|
| harness-init-java | Java 项目初始化脚手架，扫描项目生成 AGENTS.md + CLAUDE.md + docs/ 知识体系 + harness/ 自进化目录。覆盖项目结构、技术栈、API 设计、数据访问、事务、异常处理、隐性约定、Git 历史、业务上下文 | `/harness-init-java` 或"初始化项目" |
| review-summary | 变更审查报告生成器，收集 git 变更信息，基于 REVIEW.md 检查清单生成结构化评审报告 | `/review-summary` 或"帮我 review 一下" |
| spring-architecture-review | Spring Boot 分层架构审查，检查 Controller/Service/DAO 分层是否合规，Controller 是否包含业务逻辑，Service 是否过胖 | `/spring-architecture-review` 或"架构审查" |
| sql-risk-review | SQL 风险审查，检查无 WHERE 的 UPDATE/DELETE FROM、SELECT *、跨库联表、索引风险 | `/sql-risk-review` 或"SQL 审查" |
| openspec-bridge | OpenSpec 配置桥接，解决 AI 不知道要读 config.yaml 的链路断裂问题 | `/openspec-bridge` |

### Agents

3 个代理角色，各司其职。

| Agent | 说明 | 调用方式 |
|-------|------|----------|
| @reviewer | 独立只读代码审查员。不修改任何文件，只输出审查报告。按 REVIEW.md 检查清单逐项审查，覆盖逻辑正确性、规范一致性、安全合规、架构合规、可维护性 | `@reviewer` |
| @architect | 架构层面审查。关注分层架构、数据流、接口设计、依赖管理、可扩展性，而非单行代码的正确性 | `@architect` |
| @executor | 按 tasks.md 执行实现。严格在任务范围内工作，每完成一个里程碑跑验证，同一错误循环 3 次自动停下来报告 | `@executor` |

### Hooks

安全防护层，在工具执行前后自动拦截和检查。

| Hook | 类型 | 触发时机 | 说明 |
|------|------|----------|------|
| check-dangerous.sh | PreToolUse | 执行 Bash 命令前 | 检测危险操作：`rm -rf`、`DROP TABLE`、`git push --force` 等 |
| protect-files.sh | PreToolUse | 写入/编辑文件前 | 保护敏感文件：`.env`、`.git`、`*.pem`、`*.key`、`secrets/` |
| ensure_change_context.py | PreToolUse | 执行高风险 Bash 命令前 | 检查是否存在活跃的 OpenSpec change，无活跃 change 时发出警告 |
| prettier-lint.json | PostToolUse | 写入/编辑文件后 | 自动运行 prettier 格式化和 eslint 检查 |
| run-test.json | PostToolUse | 写入/编辑文件后 | 自动运行测试 |

### Rules

编码规则，安装到 `~/.claude/rules/` 后由 Claude 自动加载。

- **common/** -- 通用规则（必装），覆盖 agents、coding-style、development-workflow、git-workflow、hooks、patterns、performance、security、testing
- **golang/** -- Go 语言规则
- **java/** -- Java 语言规则
- **python/** -- Python 语言规则
- **typescript/** -- TypeScript 语言规则

## OpenSpec 工作流

OpenSpec 提供了一套结构化的变更管理流程，确保每次改动有迹可循、有据可查。

1. `/opsx:propose` -- 将需求转化为工件（proposal.md、design.md、tasks.md）
2. 人工审批 -- 审查变更边界和技术方案
3. `/opsx:apply` -- 按设计文档和任务清单执行实现
4. 专项审查 -- 调用 review-summary + spring-architecture-review + sql-risk-review 进行多维度审查
5. `/opsx:verify` -- 检查实现与工件的一致性
6. `/opsx:archive` -- 归档变更，更新 specs 索引

核心约束：不在 change 外直接开发。高风险操作（git push、kubectl、terraform 等）需要活跃的 change 上下文，否则 hooks 会拦截。

## 自进化机制

funny-harness 内建了自进化能力，让 AI 在项目运行过程中不断积累经验和改进。

### 三层记忆

- **情景记忆**（episodic.md）-- 记录具体事件和教训，比如"某次修改导致线上故障，原因是..."
- **程序记忆**（procedural.md）-- 记录成功的操作步骤模式，供后续复用
- **失败经验**（lessons-learned.md）-- 供 Critic 分析，避免重复犯错

### 轨迹编译

当同一类任务成功执行 3 次以上且步骤高度一致时，自动编译为确定性脚本。具有棘轮效应：每个被编译的模式都变成永久基础设施，不会退化。

### 进化循环

```
Agent 执行 -> 验证抓到问题 -> Critic 分析模式 -> Refiner 更新规则 -> 下一个 Agent 受益
```

## 可用命令

| 命令 | 说明 |
|------|------|
| `bash scripts/validate.sh` | 统一验证管道（build -> lint-arch -> test -> verify） |
| `bash scripts/check-docs.sh` | 文档一致性检查 |
| `bash scripts/lint-deps.sh` | 依赖方向检查 |
| `bash scripts/lint-quality.sh` | 代码质量检查 |

## 参与贡献

1. Fork 本仓库
2. 创建特性分支（`git checkout -b feature/your-feature`）
3. 提交变更（遵循 conventional commits 格式）
4. 发起 Pull Request

## 许可证

[MIT License](LICENSE), Copyright (c) 2026 funny2048
