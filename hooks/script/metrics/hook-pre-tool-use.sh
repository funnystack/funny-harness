#!/usr/bin/env bash
# Hook: PreToolUse
# 触发时机：Claude Code 调用工具前
# 输入：从 stdin 读取 JSON（含 tool_name/tool_input/session_id）
# 输出：追加事件到 ~/.claude/metrics/events.jsonl
#
# @author funny2048
# @since 2026-04-11

# 不用 set -e，避免 python3 子进程非零退出导致整个 hook 失败
set -uo pipefail

EVENTS_FILE="$HOME/.claude/metrics/events.jsonl"
mkdir -p "$(dirname "$EVENTS_FILE")"

# 从 stdin 读取 JSON
INPUT=$(cat)

# 从环境变量获取用户/项目
USERNAME="${USER:-unknown}"
HOSTNAME_VAL="$(hostname 2>/dev/null || echo 'unknown')"
PROJECT_DIR="${CLAUDE_PROJECT_DIR:-}"
PROJECT_NAME="$(basename "$PROJECT_DIR" 2>/dev/null || echo 'unknown')"
USER_ID=$(printf '%s@%s' "$USERNAME" "$HOSTNAME_VAL" | shasum -a 256 | cut -d' ' -f1)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# 用 Python 一次性完成所有解析和事件构造，避免 bash 变量注入问题
python3 -c "
import sys, json

input_str = sys.argv[1]

try:
    d = json.loads(input_str)
except json.JSONDecodeError:
    sys.exit(0)

tool_name = d.get('tool_name', '')
tool_input = d.get('tool_input', {})
session_id = d.get('session_id', '')

username = sys.argv[2]
hostname_val = sys.argv[3]
project_name = sys.argv[4]
user_id = sys.argv[5]
timestamp = sys.argv[6]

event_type = 'tool_use'
extra = {}

# Bash 工具：提取 command
if tool_name == 'Bash':
    cmd = tool_input.get('command', '')
    if cmd:
        extra['command'] = cmd

# Skill 调用识别
if 'skill' in tool_name.lower():
    skill_name = tool_input.get('skill', '') or tool_input.get('name', '')
    if skill_name:
        event_type = 'capability_use'
        extra['capability_type'] = 'skill'
        extra['capability_name'] = skill_name

# Agent 调用识别
if 'agent' in tool_name.lower():
    agent_type = tool_input.get('subagent_type', '') or tool_input.get('agent_type', '')
    if agent_type:
        event_type = 'capability_use'
        extra['capability_type'] = 'agent'
        extra['capability_name'] = agent_type

event = {
    'event_type': event_type,
    'timestamp': timestamp,
    'session_id': session_id,
    'user_id': user_id,
    'project_name': project_name,
    'tool_name': tool_name,
    'tool_input': tool_input,
    **extra
}

print(json.dumps(event, ensure_ascii=False))
" "$INPUT" "$USERNAME" "$HOSTNAME_VAL" "$PROJECT_NAME" "$USER_ID" "$TIMESTAMP" >> "$EVENTS_FILE" 2>/dev/null
