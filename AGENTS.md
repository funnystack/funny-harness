# harness-init Agent 导航地图

> AI 进入仓库后的第一入口。只做指路，不做知识库。详细知识在 docs/ 中按需加载。

## 项目定位

AI Agent 工程基建项目。 5 分钟完成团队环境标准化搭建，提供知识库、变更管理、自动化验证和自进化能力。

## 可用命令

- `bash scripts/check-docs.sh` — 文档一致性检查
- `bash scripts/validate.sh` — 统一验证管道
- `bash scripts/lint-deps.sh` — 依赖方向检查
- `bash scripts/lint-quality.sh` — 代码质量检查

## 可用Skills

- `/harness-init-java` — Java项目分析和CLAUDE.md生成
- `/review-summary` — 变更审查摘要
- `/spring-architecture-review` — Spring分层架构审查
- `/sql-risk-review` — SQL风险审查
- `/openspec-bridge` — OpenSpec配置桥接

## 可用Agents

- `@reviewer` — 独立只读代码审查
- `@architect` — 架构层面审查
- `@executor` — 按tasks.md执行实现

## OpenSpec工作流

1. `/opsx:propose` — 需求变成工件（proposal/design/tasks）
2. 人工审批 — 审查边界和方案
3. `/opsx:apply` — 按图施工
4. 专项审查 — review-summary + spring-architecture-review + sql-risk-review
5. `/opsx:verify` — 检查实现与工件一致性
6. `/opsx:archive` — 归档，更新specs

## 禁区

- 不在change外直接开发（高风险，必须先propose）
- 不修改application*.yml等配置文件（需审批）
- 不硬编码敏感信息（密码、Token、密钥用环境变量）
- 不执行生产命令（git push/kubectl/terraform需人工确认）

## 版本

当前版本：2.0.0 — 详见 [version.json](version.json)
