## ADDED Requirements

### Requirement: Hook 上下文字段映射

所有 Hook 脚本通过 stdin 接收 JSON 格式的上下文数据。系统 SHALL 按以下映射从 Hook 上下文中提取核心字段：

| 需要的信息 | 来源                                     | 说明                             |
| ---------- | ---------------------------------------- | -------------------------------- |
| session_id | stdin JSON `session_id`                  | Claude Code 生成的会话唯一标识   |
| 项目路径   | stdin JSON `cwd`                         | Hook 触发时的当前工作目录        |
| 项目根目录 | 环境变量 `$CLAUDE_PROJECT_DIR`           | 项目根目录，用于提取仓库名       |
| 用户名     | 系统环境变量 `$USER`                     | macOS/Linux 系统用户名           |
| 机器标识   | `$(hostname)`                            | 开发者机器主机名，用于区分多设备 |
| agent_type | stdin JSON `agent_type`（subagent 场景） | 仅 subagent 触发时存在           |
| tool_name  | stdin JSON `tool_name`（PreToolUse）     | 工具名称                         |
| tool_input | stdin JSON `tool_input`（PreToolUse）    | 工具输入参数                     |

用户唯一标识 SHALL 由脚本生成：`user_id = SHA256($USER + "@" + hostname)`，确保不同机器的同名用户不冲突，同一用户多台机器可区分。

项目名称 SHALL 从 `$CLAUDE_PROJECT_DIR` 提取最后一级目录名（如 `/Users/zhangsan/projects/harness` → `harness`）。

#### Scenario: 从 Hook 上下文提取完整信息

- **WHEN** SessionStart Hook 触发，stdin JSON 包含 `{"session_id":"abc123","cwd":"/Users/zhangsan/projects/harness"}`
- **THEN** 脚本提取 session_id=abc123、cwd、$CLAUDE_PROJECT_DIR=/Users/zhangsan/projects/harness、$USER=zhangsan、hostname=zhangsan-macbook、user_id=SHA256("zhangsan@zhangsan-macbook")、project_name=harness

#### Scenario: subagent 场景下获取 agent_type

- **WHEN** PreToolUse Hook 在 subagent 内触发，stdin JSON 包含 `"agent_type":"Explore"`
- **THEN** 脚本额外记录 agent_type=Explore

### Requirement: SessionStart Hook 采集会话启动事件

系统 SHALL 通过 Claude Code 的 SessionStart Hook 自动记录每次会话启动事件。采集字段 SHALL 包含：session_id（stdin JSON）、username（`$USER`）、hostname（`$(hostname)`）、user_id（SHA256 计算值）、project_path（`cwd`）、project_name（从 `$CLAUDE_PROJECT_DIR` 提取）、source（stdin JSON `source`：startup/resume/clear/compact）、model（stdin JSON `model`）、timestamp（ISO 8601）。数据 SHALL 以单行 JSON 格式追加写入 `~/.claude/metrics/events.jsonl`。

#### Scenario: 正常启动 Claude Code 会话

- **WHEN** 开发者启动一个 Claude Code 会话
- **THEN** Hook 脚本自动触发，在 `~/.claude/metrics/events.jsonl` 中追加一行 JSON，包含 event_type=session_start、session_id、username、hostname、user_id、project_path、project_name、source、model、timestamp

#### Scenario: 恢复已有会话

- **WHEN** 开发者恢复（resume）一个已有会话
- **THEN** Hook 脚本记录 source=resume，其余字段同上

#### Scenario: 本地日志目录不存在

- **WHEN** SessionStart Hook 触发且 `~/.claude/metrics/` 目录不存在
- **THEN** 脚本 SHALL 自动创建目录后再写入日志

### Requirement: PreToolUse Hook 采集工具调用事件

系统 SHALL 通过 Claude Code 的 PreToolUse Hook 记录每次工具调用事件。采集字段 SHALL 包含：session_id、user_id、username、hostname、project_name、tool_name（stdin JSON `tool_name`）、tool_input（stdin JSON `tool_input`，只保留前 200 字符摘要）、timestamp。对于 Bash 工具，SHALL 从 tool_input 提取 `command` 字段。对于 subagent 场景，SHALL 额外记录 `agent_type`。

#### Scenario: 开发者使用 Edit 工具修改代码

- **WHEN** 开发者在 Claude Code 中触发 Edit 工具
- **THEN** Hook 脚本记录 event_type=tool_use、session_id、user_id、tool_name=Edit、file_path（从 tool_input 提取）、timestamp

#### Scenario: 开发者执行 Bash 命令

- **WHEN** 开发者在 Claude Code 中触发 Bash 工具执行 `mvn test`
- **THEN** Hook 脚本记录 event_type=tool_use、session_id、user_id、tool_name=Bash、command="mvn test"（从 tool_input 提取）、timestamp

#### Scenario: subagent 内调用工具

- **WHEN** Explore 类型的 subagent 内触发 Read 工具
- **THEN** Hook 脚本额外记录 agent_type=Explore

### Requirement: Stop Hook 采集会话结束事件

系统 SHALL 通过 Claude Code 的 Stop Hook 记录会话结束事件。采集字段 SHALL 包含：session_id、user_id、username、hostname、project_name、timestamp。第二阶段 SHALL 增加 `task_summary` 结构化字段（task_type、task_result、files_changed、loops、token_total）。

#### Scenario: 正常结束会话（第一阶段）

- **WHEN** 开发者关闭一个 Claude Code 会话
- **THEN** Hook 脚本记录 event_type=session_stop、session_id、user_id、timestamp

#### Scenario: Agent 任务完成（第二阶段）

- **WHEN** Agent 完成一个编码任务后会话结束
- **THEN** Hook 脚本记录 event_type=session_stop，包含 task_summary: { task_type: "编码", task_result: "completed", files_changed: 3, loops: 1, token_total: 18200 }

### Requirement: Hook 脚本不阻塞 Claude Code 主进程

所有 Hook 脚本 SHALL 在后台异步执行，不阻塞 Claude Code 的正常使用。脚本执行超时 SHALL 不超过 3 秒。脚本失败时 SHALL 静默退出，不影响 Claude Code 功能。

#### Scenario: 日志文件写入失败

- **WHEN** 磁盘空间不足导致日志写入失败
- **THEN** Hook 脚本 SHALL 静默退出（exit 0），不向 Claude Code 输出任何错误信息

#### Scenario: stdin JSON 解析失败

- **WHEN** stdin 的 JSON 格式异常导致 jq 解析失败
- **THEN** Hook 脚本 SHALL 静默退出（exit 0），不向 Claude Code 输出任何错误信息
