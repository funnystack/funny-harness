#!/usr/bin/env bash
# Hooks 采集脚本单元测试
# 使用 bash 简易测试框架，验证 Hook 脚本输出格式
# 用法：bash test-hooks.sh
#
# @author funny2048
# @since 2026-04-11

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
METRICS_DIR="$HOME/.claude/metrics"
TEST_EVENTS="$METRICS_DIR/events.jsonl"

PASS=0
FAIL=0

assert_contains() {
    local description="$1"
    local haystack="$2"
    local needle="$3"
    if echo "$haystack" | grep -q "$needle"; then
        echo "  PASS: $description"
        ((PASS++))
    else
        echo "  FAIL: $description (期望包含: $needle)"
        ((FAIL++))
    fi
}

assert_json_field() {
    local description="$1"
    local json="$2"
    local field="$3"
    local expected="$4"
    local actual=$(echo "$json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$field',''))" 2>/dev/null || echo "")
    if [ "$actual" = "$expected" ]; then
        echo "  PASS: $description"
        ((PASS++))
    else
        echo "  FAIL: $description (字段=$field 期望=$expected 实际=$actual)"
        ((FAIL++))
    fi
}

# ========== 测试 SessionStart Hook ==========
echo "=== 测试 hook-session-start.sh ==="

# 准备测试输入
TEST_INPUT='{"session_id":"test-session-001","cwd":"/tmp/project","source":"cli","model":"claude-sonnet-4"}'

# 清空 events 文件
mkdir -p "$METRICS_DIR"
: > "$TEST_EVENTS"

# 执行 Hook
echo "$TEST_INPUT" | bash "$SCRIPT_DIR/../hook-session-start.sh"

# 读取输出
OUTPUT=$(tail -1 "$TEST_EVENTS")
echo "输出: $OUTPUT"

assert_json_field "session_start 包含 event_type" "$OUTPUT" "event_type" "session_start"
assert_json_field "session_start 包含 session_id" "$OUTPUT" "session_id" "test-session-001"
assert_json_field "session_start 包含 user_id (SHA256)" "$OUTPUT" "user_id" ""
assert_contains "session_start 包含 timestamp" "$OUTPUT" "20[0-9][0-9]-[0-9][0-9]-[0-9][0-9]T"

# ========== 测试 PreToolUse Hook ==========
echo ""
echo "=== 测试 hook-pre-tool-use.sh ==="

: > "$TEST_EVENTS"

TOOL_INPUT='{"session_id":"test-session-001","tool_name":"Read","tool_input":{"file_path":"/tmp/test.java"}}'
echo "$TOOL_INPUT" | bash "$SCRIPT_DIR/../hook-pre-tool-use.sh"

OUTPUT=$(tail -1 "$TEST_EVENTS")
echo "输出: $OUTPUT"

assert_json_field "tool_use 包含 event_type" "$OUTPUT" "event_type" "tool_use"
assert_json_field "tool_use 包含 tool_name" "$OUTPUT" "tool_name" "Read"
assert_json_field "tool_use 包含 session_id" "$OUTPUT" "session_id" "test-session-001"

# ========== 测试 Stop Hook ==========
echo ""
echo "=== 测试 hook-stop.sh ==="

: > "$TEST_EVENTS"

STOP_INPUT='{"session_id":"test-session-001","last_assistant_message":"Task completed successfully"}'
echo "$STOP_INPUT" | bash "$SCRIPT_DIR/../hook-stop.sh"

OUTPUT=$(tail -1 "$TEST_EVENTS")
echo "输出: $OUTPUT"

assert_json_field "session_stop 包含 event_type" "$OUTPUT" "event_type" "session_stop"
assert_json_field "session_stop 包含 session_id" "$OUTPUT" "session_id" "test-session-001"
assert_contains "session_stop 包含 task_summary" "$OUTPUT" "task_summary"

# ========== 测试结果 ==========
echo ""
echo "==============================="
echo "测试结果: PASS=$PASS FAIL=$FAIL"
echo "==============================="

# 清理测试数据
: > "$TEST_EVENTS"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
