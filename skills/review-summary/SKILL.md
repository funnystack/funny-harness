---
name: review-summary
description: >
  生成变更审查摘要并输出评审报告。读取 REVIEW.md 检查清单，收集 git 变更信息，
  生成结构化评审报告到 openspec/changes/{change-id}/review.md。
  当用户说"生成review摘要"、"整理变更"、"准备review"、"review-summary"、
  "审查变更"、"帮我review一下"时触发。
---

# review-summary：变更审查报告生成器

## 为什么需要这个 Skill

Code Review 的效率取决于信息呈现质量。这个 Skill 做两件事:

1. 收集变更上下文，生成变更摘要
2. 基于 REVIEW.md 检查清单逐项审查，输出评审报告

最终产出到 `openspec/changes/{change-id}/review.md`，形成评审闭环。

## 配套文件

本 Skill 目录下的 `REVIEW.md` 是审查检查清单，定义了评审目标、忽略项、必须指出的问题。执行前必须先读取。

## 工作流程

### Step 1: 确定 change-id

1. 检查 `openspec/changes/` 下是否有活跃变更目录
2. 如果有，取当前活跃的 change-id（有且仅有一个未归档的变更）
3. 如果没有，基于当前分支名或用户输入生成 change-id
4. 确保目录存在: `mkdir -p openspec/changes/{change-id}`

### Step 2: 收集变更信息

1. 执行 `git diff --name-status` 获取变更文件列表
2. 执行 `git diff --stat` 获取变更统计
3. 读取 `openspec/changes/{change-id}/` 下的 proposal.md、design.md、tasks.md（如存在）
4. 读取本 Skill 目录下的 `REVIEW.md` 获取检查清单

### Step 3: 分类整理

按以下维度分类:

- **新增文件**：标注是否为预期的（对照 tasks.md）
- **修改文件**：标注修改原因
- **删除文件**：标注删除原因和风险评估
- **高危文件**：涉及 SQL、配置、认证、权限的变更

### Step 4: 基于 REVIEW.md 逐项审查

严格按照 `REVIEW.md` 中的:

- "评审目标" — 逐项检查，每项给出结论
- "忽略项" — 跳过
- "必须指出的问题" — 重点标注

对每项审查结论，给出具体文件和行号证据。

### Step 5: 生成评审报告

将审查报告写入 `openspec/changes/{change-id}/review.md`，格式:

```markdown
# 评审报告：{change_name}

## 变更概览

- 变更文件数：{count}
- 新增/修改/删除：{a}/{m}/{d}
- 关联 OpenSpec：{有/无}

## 变更文件列表

### 新增文件

| 文件 | 用途 | 风险等级 |
| ---- | ---- | -------- |

### 修改文件

| 文件 | 修改内容 | 风险等级 |
| ---- | -------- | -------- |

### 删除文件

| 文件 | 删除原因 | 风险评估 |
| ---- | -------- | -------- |

## 高风险变更（需重点关注）

- {列出高危文件和原因}

## 检查清单审查

> 检查清单来源: skills/review-summary/REVIEW.md

{逐项列出 REVIEW.md 中的每个评审目标，标注: PASS / FAIL / N/A，FAIL 的给出具体位置和建议}

## 与 design.md 的一致性检查

- 已实现：{列表}
- 未实现：{列表}
- 额外变更：{列表}

## 评审结论

- 阻塞问题数：{count}
- 建议改进数：{count}
- 结论：{通过 / 有条件通过 / 不通过}
```

### Step 6: 通知用户

提示用户:

> 评审报告已生成到 `openspec/changes/{change-id}/review.md`。
> 如需深度审查，可调用 `@reviewer` 代理，它会读取同一份 REVIEW.md 检查清单进行独立审查。
