#!/usr/bin/env bash
# 数据上报脚本
# 功能：扫描本地 events.jsonl → 过滤无效记录 → 批量 HTTPS POST 上报 → 处理失败重试
# 用法：bash upload-metrics.sh
# 建议由 Cron 每 5 分钟调用一次
#
# @author funny2048
# @since 2026-04-11

set -eo pipefail

# ========== 配置 ==========
METRICS_DIR="$HOME/.claude/metrics"
EVENTS_FILE="$METRICS_DIR/events.jsonl"
UPLOADED_FILE="$METRICS_DIR/uploaded.jsonl"
INVALID_FILE="$METRICS_DIR/invalid.jsonl"
FAILED_DIR="$METRICS_DIR/failed"
BATCH_DIR="$METRICS_DIR/batches"

# 服务端地址（默认 localhost:8090，可通过环境变量覆盖）
METRICS_SERVER="${METRICS_SERVER:-http://localhost:8090}"
METRICS_API_KEY="${METRICS_API_KEY:-}"
METRICS_API_ENDPOINT="$METRICS_SERVER/api/metrics/collect"

mkdir -p "$FAILED_DIR" "$BATCH_DIR"

# ========== 1. 过滤无效记录 ==========
filter_invalid() {
    if [ ! -f "$EVENTS_FILE" ] || [ ! -s "$EVENTS_FILE" ]; then
        return 0
    fi

    python3 -c "
import sys, json

valid = []
invalid = []

with open('$EVENTS_FILE', 'r') as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        try:
            d = json.loads(line)
            if d.get('event_type') and d.get('timestamp'):
                valid.append(line)
            else:
                invalid.append(line)
        except:
            invalid.append(line)

with open('$EVENTS_FILE', 'w') as f:
    f.write('\n'.join(valid) + '\n' if valid else '')

if invalid:
    with open('$INVALID_FILE', 'a') as f:
        f.write('\n'.join(invalid) + '\n')
" 2>/dev/null
}

# ========== 2. 扫描并打包 ==========
prepare_batch() {
    if [ ! -f "$EVENTS_FILE" ] || [ ! -s "$EVENTS_FILE" ]; then
        return 1
    fi

    BATCH_ID="$(date +%s)_$$"
    cp "$EVENTS_FILE" "$BATCH_DIR/batch_${BATCH_ID}.jsonl"
    : > "$EVENTS_FILE"
    return 0
}

# ========== 3. HTTPS POST 上报 ==========
upload_batch() {
    local batch_file="$1"
    local batch_id
    batch_id=$(basename "$batch_file" | sed 's/batch_\(.*\)\.jsonl/\1/')

    if [ ! -f "$batch_file" ] || [ ! -s "$batch_file" ]; then
        rm -f "$batch_file"
        return 0
    fi

    # 将 JSONL 转为 JSON 数组
    local json_array
    json_array=$(python3 -c "
import json
events = []
with open('$batch_file') as f:
    for line in f:
        line = line.strip()
        if line:
            try:
                events.append(json.loads(line))
            except:
                pass
print(json.dumps(events))
" 2>/dev/null)

    if [ -z "$json_array" ] || [ "$json_array" = "[]" ]; then
        rm -f "$batch_file"
        return 0
    fi

    # 计算 client_id
    local username="${USER:-unknown}"
    local hostname_val
    hostname_val=$(hostname 2>/dev/null || echo 'unknown')
    local client_id
    client_id=$(printf '%s@%s' "$username" "$hostname_val" | shasum -a 256 | cut -d' ' -f1)

    # 发送 HTTPS POST
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$METRICS_API_ENDPOINT" \
        -H "Content-Type: application/json" \
        -H "X-API-Key: $METRICS_API_KEY" \
        -H "X-Client-Id: $client_id" \
        -H "X-Batch-Id: $batch_id" \
        -d "$json_array" \
        --connect-timeout 10 \
        --max-time 30 2>/dev/null || echo "000")

    if [ "$http_code" -ge 200 ] 2>/dev/null && [ "$http_code" -lt 300 ] 2>/dev/null; then
        cat "$batch_file" >> "$UPLOADED_FILE"
        rm -f "$batch_file"
        echo "上报成功，batch=$batch_id，http_code=$http_code"
        return 0
    elif [ "$http_code" -ge 400 ] 2>/dev/null && [ "$http_code" -lt 500 ] 2>/dev/null; then
        mv "$batch_file" "$FAILED_DIR/"
        echo "上报失败（客户端错误），batch=$batch_id，http_code=$http_code"
        return 1
    else
        echo "上报失败（可重试），batch=$batch_id，http_code=$http_code"
        return 1
    fi
}

# ========== 主流程 ==========
main() {
    # Step 1: 过滤无效记录
    filter_invalid

    # Step 2: 准备新批次
    prepare_batch || true

    # Step 3: 上报所有批次
    for batch_file in "$BATCH_DIR"/batch_*.jsonl; do
        [ -f "$batch_file" ] || continue
        upload_batch "$batch_file" || true
    done
}

main
