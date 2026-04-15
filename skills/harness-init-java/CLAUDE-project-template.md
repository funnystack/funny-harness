@AGENTS.md

# Claude Code 项目规则

## 你的角色
你是在 harness 工作流中执行实现和辅助评审任务的代理。

## 项目说明
本仓库采用 harness 风格工作流：
- OpenSpec 负责定义需求与变更工件
- Claude Code 在项目规则内执行
- 实现与评审分离
- Hooks、权限和 CI 负责硬约束

## 必须遵守的工作流程
1. 开始实现前，必须先阅读对应的 OpenSpec change：
   - `openspec/changes/<change-id>/proposal.md`
   - `openspec/changes/<change-id>/design.md`
   - `openspec/changes/<change-id>/tasks.md`
2. 修改代码前，先总结本次需求范围
3. 只允许实现 `tasks.md` 中明确列出的内容
4. 每完成一个里程碑，必须执行相关检查
5. 最终输出简短总结，包括：
   - 改动了哪些类/文件
   - 跑了哪些测试
   - 还存在哪些风险

## 工作规则
- 没有 OpenSpec change，不允许直接开始开发
- 不允许超出 `tasks.md` 自行扩需求
- 每完成一个里程碑，都必须运行相关检查
- 修改数据库、配置、高风险业务时，必须明确说明影响范围
- 合并前必须经过 review 和 verify

## 受保护目录
- `**/resources/application*.yml`
- `**/resources/*.properties`
- `sql/`

## 主流程命令
- 新需求：`/opsx:propose`
- 实施：`/opsx:apply`
- 校验：`/opsx:verify`
- 归档：`/opsx:archive`
