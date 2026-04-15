#!/usr/bin/env bash
# Hook: Stop
# 触发时机：Claude Code 会话结束时
# 输入：从 stdin 读取 JSON（含 session_id/last_assistant_message）
# 输出：追加 session_stop 事件到 ~/.claude/metrics/events.jsonl
#
# @author funny2048
# @since 2026-04-11

set -uo pipefail

EVENTS_FILE="$HOME/.claude/metrics/events.jsonl"
mkdir -p "$(dirname "$EVENTS_FILE")"

INPUT=$(cat)

USERNAME="${USER:-unknown}"
HOSTNAME_VAL="$(hostname 2>/dev/null || echo 'unknown')"
PROJECT_DIR="${CLAUDE_PROJECT_DIR:-}"
PROJECT_NAME="$(basename "$PROJECT_DIR" 2>/dev/null || echo 'unknown')"
USER_ID=$(printf '%s@%s' "$USERNAME" "$HOSTNAME_VAL" | shasum -a 256 | cut -d' ' -f1)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

python3 -c "
import sys, json

try:
    d = json.loads(sys.argv[1])
except json.JSONDecodeError:
    sys.exit(0)

msg = d.get('last_assistant_message', '')
if isinstance(msg, str):
    summary = msg[:200]
elif isinstance(msg, dict):
    summary = str(msg.get('content', msg.get('text', '')))[:200]
else:
    summary = str(msg)[:200]

event = {
    'event_type': 'session_stop',
    'timestamp': sys.argv[6],
    'session_id': d.get('session_id', ''),
    'user_id': sys.argv[5],
    'project_name': sys.argv[4],
    'task_summary': {
        'task_desc': summary,
        'agent_type': 'claude-code',
        'loops': 1
    }
}

print(json.dumps(event, ensure_ascii=False))
" "$INPUT" "$USERNAME" "$HOSTNAME_VAL" "$PROJECT_NAME" "$USER_ID" "$TIMESTAMP" >> "$EVENTS_FILE" 2>/dev/null
