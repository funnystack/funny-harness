---
name: harness-analyzer
description: >
  差距分析器。诊断项目 Harness 知识体系的健康度，对比满分标准输出差距报告和修复优先级。
  当用户说"诊断项目"、"健康度检查"、"差距分析"、"harness audit"、"评分"、"检查 harness"、
  "项目健康度"、"知识体系诊断"时触发。
  也适用于定期校准场景："帮我检查一下项目的文档是不是最新的"或"看看 harness 还缺什么"。
---

# harness-analyzer：差距分析器

## 定位

知识维护三步法中的第三步（定期校准）。对比当前仓库状态和 Harness 满分要求，输出差距报告和修复优先级。

七个检测维度，每个维度有明确的检查项和评分标准。

## 评分维度

| 维度 | 权重 | 满分要求 |
|------|------|---------|
| AGENTS.md 完整度 | 15% | 存在且包含项目定位、技术栈索引、禁区声明 |
| docs/ 齐全度 | 25% | 必需文档全部存在且非空模板 |
| 分层 lint 覆盖 | 20% | lint 脚本存在且可执行 |
| permissions 配置 | 15% | 受保护路径已配置 deny 规则 |
| hooks 存在性 | 10% | 写入保护 / 上下文检查 / 自动验证 hook 存在 |
| 记忆机制 | 5% | harness/memory/ 和 harness/trace/ 存在 |
| 变更流程 | 10% | openspec/ 目录和 config 存在 |

## 工作流程

### Step 1: 逐维度检查

按维度逐一检查，每个维度内部的检查逻辑见下文。

检查过程只读取文件是否存在、是否有实质内容（非空模板），不读取文件具体内容中的敏感信息。

#### 1.1 AGENTS.md 完整度 (15%)

| 检查项 | 分值 | 检查方式 |
|-------|------|---------|
| 文件存在 | 5 | 检查根目录是否有 AGENTS.md |
| 项目定位描述 | 3 | grep 是否包含项目定位相关关键词 |
| 技术/架构索引 | 4 | grep 是否包含 `@docs/` 引用或技术栈章节 |
| 禁区声明 | 3 | grep 是否包含"禁区"或"禁止"相关描述 |

#### 1.2 docs/ 齐全度 (25%)

| 检查项 | 分值 | 检查方式 |
|-------|------|---------|
| project-structure.md | 5 | 存在且 > 10 行 |
| architecture.md | 5 | 存在且 > 10 行 |
| architecture/tech-stack.md | 5 | 存在且 > 10 行 |
| architecture/api-design.md | 4 | 存在且 > 10 行 |
| architecture/database.md | 3 | 存在且 > 10 行 |
| business-context.md | 2 | 存在且 > 5 行 |
| implicit-contracts.md | 1 | 存在 |

> 10 行阈值用于排除空模板（模板骨架通常只有 3-5 行占位符）。

#### 1.3 分层 lint 覆盖 (20%)

| 检查项 | 分值 | 检查方式 |
|-------|------|---------|
| lint-deps 脚本存在 | 8 | `scripts/verify/lint-deps.sh` 或 `scripts/lint-deps.*` 存在 |
| lint-quality 脚本存在 | 7 | `scripts/verify/lint-quality.sh` 或 `scripts/lint-quality.*` 存在 |
| 脚本可执行 | 5 | 文件有执行权限 |

#### 1.4 permissions 配置 (15%)

| 检查项 | 分值 | 检查方式 |
|-------|------|---------|
| 配置文件保护 | 5 | `.claude/settings.json` 或 AGENTS.md 中包含 application*.yml deny 规则 |
| 数据库脚本保护 | 5 | 包含 db/ sql/ migration/ deny 规则 |
| 凭证文件保护 | 5 | 包含 .env secrets/ .mcp.json deny 规则 |

#### 1.5 hooks 存在性 (10%)

| 检查项 | 分值 | 检查方式 |
|-------|------|---------|
| 写入保护 hook | 4 | PostToolUse hook 配置中包含文件写入相关拦截 |
| 自动验证 hook | 4 | PostToolUse hook 配置中包含 validate 相关命令 |
| 上下文检查 hook | 2 | PreToolUse hook 配置中包含 change 检查逻辑 |

#### 1.6 记忆机制 (5%)

| 检查项 | 分值 | 检查方式 |
|-------|------|---------|
| harness/memory/ 存在 | 2 | 目录存在 |
| harness/trace/ 存在 | 2 | 目录存在 |
| lessons-learned.md 非空 | 1 | 文件存在且 > 5 行 |

#### 1.7 变更流程 (10%)

| 检查项 | 分值 | 检查方式 |
|-------|------|---------|
| openspec/ 目录 | 5 | 目录存在 |
| config.yaml 存在 | 3 | openspec/config.yaml 存在 |
| specs/ 目录 | 2 | openspec/specs/ 存在 |

### Step 2: 计算加权总分

每个维度的得分 × 权重，加总得到 0-100 的综合评分。

评级：

| 分数 | 评级 | 说明 |
|------|------|------|
| 90-100 | A | 知识体系完善，可进入持续维护模式 |
| 70-89 | B | 骨架完成，部分维度需要补全 |
| 50-69 | C | 基础存在，有明显缺口 |
| < 50 | D | 建议重新运行 harness-init-java |

### Step 3: 输出报告

输出到终端，结构：

```markdown
## Harness 健康度报告

**项目**: {project_name}
**评分**: {score}/100 ({grade})
**检测时间**: {timestamp}

### 维度详情

| 维度 | 得分 | 满分 | 状态 |
|------|------|------|------|
| AGENTS.md 完整度 | x/15 | 15 | ✅/⚠️/❌ |
| docs/ 齐全度 | x/25 | 25 | ✅/⚠️/❌ |
| ... | ... | ... | ... |

### 修复优先级（按得分率排序）

1. **[P0] docs/ 齐全度** — 缺少 architecture/database.md
   → 修复方式: 运行 harness-init-java 或手动补充
2. **[P1] permissions 配置** — 未配置 application*.yml deny 规则
   → 修复方式: 在 .claude/settings.json 中添加 deny 规则
3. ...

### 亮点

- AGENTS.md 完整度高，项目定位和禁区声明清晰
- lint 脚本齐全且可执行
```

### Step 4: 修复建议

对每个失分项，给出具体修复方式：

| 失分原因 | 建议修复 |
|---------|---------|
| 文档缺失 | 运行 harness-init-java 或手动创建 |
| 文档为空模板 | 补充实际内容（参考 harness-init-java 模板） |
| lint 脚本缺失 | 从 harness-init-java 的 scripts/ 复制 |
| permissions 未配置 | 参考 AGENTS.md 禁区列表配置 deny 规则 |
| hooks 未配置 | 参考 harness 文档配置 guard hooks |
| openspec 未初始化 | 运行 openspec init |

## 注意事项

1. **只读诊断，不修改任何文件**。
2. **不读取敏感配置内容**，只检查文件是否存在和行数。
3. **评分标准可定制**：用户可在 AGENTS.md 中声明权重覆盖。
4. **CI 友好**：可集成到 CI 管道中，评分低于阈值时阻断合并。
