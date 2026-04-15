---
name: harness-memory
description: >
  项目记忆提取器。扫描本地 Claude Code session 会话记录，自动提取失败经验和教训，
  输出到 harness/memory/lessons-learned.md。支持增量去重，不会重复提取已处理的 session。
  当用户说"提取记忆"、"分析会话"、"总结经验"、"session 分析"、"记忆归档"、"失败经验提取"、
  "回顾会话记录"、"提取教训"时触发。
  也适用于定期维护场景："帮我看看最近几天的 AI 会话有没有什么值得总结的"或"归档一下最近的 session"。
---

# harness-memory：项目记忆提取器

## 定位

从 Claude Code 的原始 session 会话记录中，自动提取失败经验和教训，沉淀到项目知识库。

解决的核心问题：AI 在 session 中犯过的错、走过的弯路，如果不主动提取，下次会话还会重蹈覆辙。

## 与其他技能的边界

| 技能                         | 输入                                         | 输出                                |
| ---------------------------- | -------------------------------------------- | ----------------------------------- |
| **harness-memory**（本技能） | `~/.claude/projects/` 下的原始 session JSONL | `harness/memory/lessons-learned.md` |
| harness-critic               | `harness/trace/` 下的人工失败记录            | 模式分析报告                        |
| harness-update               | `git diff`                                   | 文档同步清单                        |

本技能处理的是原始会话数据，critic 处理的是结构化的 trace 记录，两者互补。

## 前置条件

1. 项目必须有 Claude Code session 记录（`~/.claude/projects/` 下有对应目录）
2. 项目目录下需要有 `harness/memory/` 目录（不存在则自动创建）

## 数据位置

Session 文件存储规则：

- 项目路径 `/Users/xx/Documents/open/my-project` 对应目录
  `~/.claude/projects/-Users-xx-Documents-open-my-project/`
- 每个 session 是一个 `{sessionId}.jsonl` 文件
- 活跃 session 在 `~/.claude/sessions/{pid}.json` 中有索引

## 去重机制

使用 processed-sessions 索引文件避免重复提取：

**索引文件位置**: `harness/memory/.processed-sessions.json`

```json
{
  "lastProcessedAt": "2026-04-14T17:00:00Z",
  "sessions": {
    "ff46023c-9534-4c8f-a73e-7482ad55b9ca": {
      "processedAt": "2026-04-14T17:00:00Z",
      "findingsCount": 3,
      "summary": "发现3条失败经验"
    }
  }
}
```

**活跃 session 过滤**：通过 `~/.claude/sessions/` 目录下的 PID 文件判断 session 是否还在进行中。如果 sessionId 对应的 PID 文件存在，说明该 session 仍活跃，跳过不处理。这样既不会遗漏后续内容，也不会分析不完整的会话。

## 工作流程

### Step 1: 定位项目的 session 目录

根据当前项目路径，计算对应的 session 存储目录：

```
项目路径 /Users/xx/Documents/open/funny-harness
→ session 目录 ~/.claude/projects/-Users-xx-Documents-open-funny-harness/
```

转换规则：将路径中的 `/` 替换为 `-`，前面加 `-`。

列出该目录下所有 `.jsonl` 文件，排除 `subagents/` 子目录下的文件（子代理记录不需要单独处理）。

### Step 2: 加载去重索引

读取 `harness/memory/.processed-sessions.json`。如果文件不存在，视为首次运行，所有 session 都需要处理。

同时读取 `~/.claude/sessions/` 目录下的所有 PID 文件，构建活跃 sessionId 集合。

过滤条件：

1. **已处理** → 跳过（在索引中有记录）
2. **仍活跃** → 跳过（在活跃 PID 文件中有记录）
3. **未处理且已结束** → 需要处理

输出候选列表供用户确认：

```
发现 X 个新 session 待分析（跳过 Y 个已处理，Z 个仍活跃）
Session 列表：
  - a1b2c3d4... (2026-04-12 14:30)
  - e5f6g7h8... (2026-04-13 09:15)
```

### Step 3: 逐 session 提取失败经验

对每个候选 session，按以下逻辑分析：

#### 3.1 读取 session 内容

读取 JSONL 文件，只关注以下消息类型：

- `type: "user"` — 用户输入和反馈
- `type: "assistant"` — AI 的响应和工具调用
- `type: "user"` 且包含 `tool_result` — 工具执行结果

跳过 `permission-mode`、`file-history-snapshot`、`attachment` 等元数据消息。

#### 3.2 识别失败模式

在 session 内容中搜索以下模式：

| 模式          | 识别信号                                                              |
| ------------- | --------------------------------------------------------------------- |
| 编译/构建失败 | assistant 消息中出现 error、failed、compilation error，之后有修复尝试 |
| 测试失败      | 工具调用包含 test、spec、pytest、junit，结果为失败                    |
| 分层违规      | 跨层 import、Controller 直接调用 DAO 等                               |
| 配置错误      | 修改了受保护文件、硬编码配置值                                        |
| 用户纠正      | 用户消息包含"不对"、"不是这样"、"重做"、"不要"等否定词                |
| 反复尝试      | 同一工具连续调用 3 次以上，参数不断调整                               |
| 安全问题      | 泄露敏感信息、绕过认证、未验证输入                                    |
| 方案推翻      | 用户明确拒绝了 AI 的方案，要求换一种方式                              |

#### 3.3 提取结构化经验

对每个识别到的失败模式，提取：

```
- 日期: session 发生时间
- 问题类型: 编译失败/测试失败/分层违规/配置错误/业务逻辑/安全/用户纠正
- 场景描述: 一句话说明什么情况下发生的
- 根因分析: 为什么会失败（从对话上下文推断）
- 修复方式: 最终是怎么解决的
- 教训提炼: 一句话总结，应该怎么做
- 严重度: 高（安全/数据问题）/ 中（影响功能）/ 低（效率问题）
```

### Step 4: 终端输出分析报告

将提取结果格式化输出到终端：

```markdown
## Session 记忆提取报告

**分析范围**: X 个 session（从 YYYY-MM-DD 到 YYYY-MM-DD）
**提取结果**: Y 条失败经验

### 按严重度分组

#### 高严重度

1. **[安全] 未验证用户输入导致 SQL 注入风险**
   - 场景: 在实现搜索接口时直接拼接 SQL
   - 根因: 未使用参数化查询
   - 修复: 改用 MyBatis 的 #{} 占位符
   - 教训: 所有外部输入必须参数化，禁止字符串拼接 SQL

#### 中严重度

2. **[分层] Controller 直接调用 Mapper**
   ...

#### 低严重度

3. **[效率] 反复尝试安装依赖 5 次**
   ...
```

如果某个 session 没有发现值得记录的失败经验，简要说明即可，不必硬凑。

### Step 5: 确认后追加到文件

将用户确认的经验追加到 `harness/memory/lessons-learned.md`。

文件格式：

```markdown
# 失败经验总结

> 由 harness-memory 自动提取，经人工确认

## YYYY-MM-DD 提取

### [安全] 未验证用户输入导致 SQL 注入风险

- **场景**: 实现搜索接口时直接拼接 SQL
- **根因**: 未使用参数化查询
- **修复**: 改用 MyBatis #{} 占位符
- **教训**: 所有外部输入必须参数化

### [分层] Controller 直接调用 Mapper

- **场景**: ...
```

追加规则：

- 新经验按日期分组追加到文件末尾
- 不修改已有的经验条目
- 同一天多次提取合并到同一个日期分组下

### Step 6: 更新去重索引

处理完成后，更新 `harness/memory/.processed-sessions.json`：

- 将已处理的 sessionId 加入索引
- 更新 `lastProcessedAt` 时间戳
- 记录每个 session 的发现数量

## 注意事项

1. **只提取，不自动修改代码**。所有经验必须用户确认后才写入文件。
2. **跳过活跃 session**。正在进行的 session 内容不完整，不纳入分析。
3. **隐私保护**。不提取用户消息中的敏感信息（密码、Token、个人数据），只关注技术层面的失败模式。
4. **最小读取原则**。大 session 文件（>1MB）只读取关键消息，不全文加载。
5. **增量处理**。每次只处理新增的 session，已处理的不会重新分析。
6. **与 harness-critic 互补**。本技能从原始 session 提取，critic 从结构化 trace 分析，不重复。
