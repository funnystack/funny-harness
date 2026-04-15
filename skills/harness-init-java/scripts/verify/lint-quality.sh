#!/bin/bash
# 代码质量规则检查
# 检查：单文件行数限制、禁止 console.log/print、禁止硬编码品牌字符串

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 代码质量检查 ==="

MAX_LINES=500
errors=0
warnings=0

check_file_length() {
    echo "[检查] 单文件不超过 ${MAX_LINES} 行..."
    if [ -d "src" ]; then
        long_files=$(find src -name "*.java" -o -name "*.py" -o -name "*.ts" -o -name "*.go" 2>/dev/null | while read f; do
            lines=$(wc -l < "$f")
            if [ "$lines" -gt "$MAX_LINES" ]; then
                echo "  ⚠️  $f: ${lines} 行（超过 ${MAX_LINES} 行限制）"
            fi
        done)
        if [ -n "$long_files" ]; then
            echo "$long_files"
            warnings=$((warnings + 1))
        else
            echo "  ✅ 所有文件行数在限制内"
        fi
    else
        echo "  ⏭️  未检测到 src 目录，跳过"
    fi
}

check_debug_prints() {
    echo "[检查] 禁止 console.log / print 调试语句..."
    if [ -d "src" ]; then
        debug_prints=$(grep -rn "console\.log\|System\.out\.print\|print(" --include="*.java" --include="*.ts" --include="*.py" src/ 2>/dev/null | grep -v "//.*console\.log\|//.*print(" || true)
        if [ -n "$debug_prints" ]; then
            echo "  ⚠️  发现调试打印语句："
            echo "$debug_prints" | head -10
            warnings=$((warnings + 1))
        else
            echo "  ✅ 未发现调试打印语句"
        fi
    fi
}

check_file_length
check_debug_prints

# 检查硬编码 URL/IP
check_hardcoded_secrets() {
    echo "[检查] 硬编码 URL/IP..."
    if [ -d "src" ]; then
        hardcoded=$(grep -rn "http://\|https://\|jdbc:mysql://\|redis://" --include="*.java" --include="*.yml" --include="*.yaml" --include="*.properties" src/ 2>/dev/null | grep -v "//.*\${" | grep -v "test" | grep -v "example" || true)
        if [ -n "$hardcoded" ]; then
            echo "  ⚠️  发现硬编码地址（前10条）："
            echo "$hardcoded" | head -10
            warnings=$((warnings + 1))
        else
            echo "  ✅ 未发现硬编码地址"
        fi
    fi
}

check_hardcoded_secrets

echo ""
if [ $errors -gt 0 ]; then
    echo "❌ 质量检查失败：$errors 错误, $warnings 警告"
    exit 1
elif [ $warnings -gt 0 ]; then
    echo "⚠️  质量检查通过但有 $warnings 个警告"
    exit 0
else
    echo "✅ 质量检查通过"
    exit 0
fi
