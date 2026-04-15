# AGENTS.md

## 项目结构及技术栈

<!-- architecture.md 由 harness-init skill 自动填充 -->

@docs/project-structure.md

## 架构模式

@docs/architecture.md

## 业务上下文

<!-- 半自动，需要人工补充 -->

@docs/business-context.md

## 隐性约定

<!-- 半自动，需要人工补充-->

@docs/implicit-contracts.md

## 禁区（项目雷区）

<!-- 需要人工补充-->

1. 这个项目最核心的业务流程是什么？

2. 新人最容易犯的错误是什么？

3. 哪些代码绝对不能动，为什么？

4. 性能瓶颈在哪里？

5. 上下游系统的依赖关系是什么？

6. 最近做过哪些重要架构决策？

## 可用命令

| 命令                                   | 说明                                                 |
| -------------------------------------- | ---------------------------------------------------- |
| `bash scripts/validate.sh`             | 统一验证管道（编译→结构检查→依赖检查→质量检查→测试） |
| `bash scripts/verify/lint-deps.sh`     | 依赖方向检查                                         |
| `bash scripts/verify/lint-quality.sh`  | 代码质量检查                                         |
| `bash scripts/verify/check-harness.sh` | Harness 结构检查                                     |
