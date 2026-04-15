#!/bin/bash
# 基础端到端功能验证

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 基础功能验证 ==="

ERRORS=0

check_structure() {
    echo "[验证] 项目基础结构..."

    if [ ! -f "CLAUDE.md" ] && [ ! -f "AGENTS.md" ]; then
        echo "  ❌ 缺少 CLAUDE.md 和 AGENTS.md（至少需要其一）"
        ERRORS=$((ERRORS + 1))
    else
        echo "  ✅ 基础结构完整"
    fi

    if [ ! -f "pom.xml" ]; then
        echo "  ❌ 缺少 pom.xml"
        ERRORS=$((ERRORS + 1))
    else
        echo "  ✅ pom.xml 存在"
    fi
}

check_structure

if [ $ERRORS -gt 0 ]; then
    echo ""
    echo "❌ 基础验证失败：$ERRORS 处问题"
    exit 1
else
    echo ""
    echo "✅ 基础验证通过"
    exit 0
fi
