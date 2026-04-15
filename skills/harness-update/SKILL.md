---
name: harness-update
description: >
  知识同步器。检测代码变更（git diff），输出需要同步更新的文档清单。
  解决"代码改了但文档没跟着改"的僵尸文档问题。
  当用户说"更新文档"、"sync docs"、"知识同步"、"变更后更新"、"MR检查"、
  "文档同步"、"代码变更后更新文档"、"帮我看看文档需不需要更新"时触发。
  也适用于 MR/CR 阶段自动检测："检查一下这次变更要不要更新文档"。
---

# harness-update：知识同步器

## 定位

知识维护三步法中的第一步（即时捕获）。每次代码变更后，自动判断哪些项目文档需要同步更新。

核心逻辑：**变更类型 → 需更新的文档** 是一组确定性映射，不依赖 AI 判断，用规则驱动。

## 前置条件

项目必须已有 `docs/` 目录和 AGENTS.md / CLAUDE.md。

## 变更类型与文档映射

读取 `references/mapping-rules.md` 获取完整的映射规则。核心映射如下：

| 变更类型 | 检测方式 | 需更新的文档 |
|---------|---------|------------|
| 新增/删除 Java 包 | diff 中出现新 package 声明 | `docs/project-structure.md` |
| 新增 Controller/端点 | `@RequestMapping` 等 diff | `docs/architecture/api-design.md` |
| 新增/修改 Entity 字段 | entity 类 diff | `docs/architecture/database.md` |
| 新增依赖（pom.xml/build.gradle） | 构建文件 diff | `docs/architecture/tech-stack.md` |
| 新增中间件使用 | import RedisTemplate、KafkaTemplate 等 | `docs/architecture/tech-stack.md` |
| 新增跨层 import | service import dao 等 | `docs/architecture.md` 分层章节 |
| 新增枚举/常量 | enum 类 diff | `docs/business-context.md` |
| 新增配置项 | `application*.yml` diff | `docs/architecture/tech-stack.md` |

## 工作流程

### Step 1: 获取变更范围

```bash
# 对比当前分支与主分支的差异
git diff --name-status main...HEAD

# 获取变更的文件列表和变更类型（A=新增, M=修改, D=删除）
```

如果没有指定基准分支，默认使用 `main`。用户可通过参数指定基准分支。

如果项目不在 Git 仓库中，要求用户手动提供变更文件列表。

### Step 2: 逐文件匹配规则

对每个变更文件，按 `references/mapping-rules.md` 中的规则匹配：

1. **按路径匹配** — 文件路径匹配特定模式（如 `*Controller.java` → api-design.md）
2. **按内容匹配** — diff 内容包含特定关键词（如 `import RedisTemplate` → tech-stack.md）
3. **按元数据匹配** — pom.xml 变更 → tech-stack.md

对匹配到的规则，收集：
- 变更文件
- 匹配规则
- 需更新的目标文档
- 更新原因（一句话说明为什么需要更新）

### Step 3: 去重和排序

同一文档可能有多个变更触发更新，合并为一条：
- 按目标文档分组
- 合并更新原因
- 按优先级排序：涉及架构约束的 > 涉及 API 的 > 涉及技术栈的 > 其他

### Step 4: 输出清单

输出结构化清单：

```markdown
## 文档同步检查报告

**变更范围**: X 个文件变更（基准: main）

### 需要更新

| 优先级 | 目标文档 | 触发变更 | 更新原因 |
|-------|---------|---------|---------|
| P0 | docs/architecture/api-design.md | OrderController.java (新增) | 新增 3 个 POST 端点 |
| P1 | docs/architecture/database.md | Order.java (修改) | 新增 status_code 字段 |
| P2 | docs/architecture/tech-stack.md | pom.xml (修改) | 新增 Elasticsearch 依赖 |

### 无需更新

- src/main/java/.../utils/DateHelper.java (内部工具类，不影响项目文档)
```

### Step 5: 执行更新（可选）

用户确认后，按清单逐项更新文档。更新原则：
- **增量补充**，不重写整篇文档
- 每处更新标注来源变更（便于回溯）
- 更新完成后验证文档格式正确

## 注意事项

1. **只读扫描，不自动修改**。Step 5 必须用户确认后才执行。
2. **不读取配置文件中的敏感信息**（密码、Token 等），只关注结构变更。
3. **映射规则可扩展**：用户可在项目的 AGENTS.md 中自定义映射规则。
4. **CI 集成**：可作为 MR 合并前的检查步骤，输出清单作为 CR 的一项检查项。
