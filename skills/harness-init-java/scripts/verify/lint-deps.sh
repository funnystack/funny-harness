#!/bin/bash
# 依赖方向检查脚本
# 检查是否存在跨层 import（高层 import 低层是合规的，反过来不行）
# 分层规则：Layer 0(类型) → Layer 1(工具) → Layer 2(配置) → Layer 3(业务) → Layer 4(接口)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 依赖方向检查 ==="

errors=0

# 检查 Controller 层不能被其他层 import
check_controller_imports() {
    echo "[检查] Controller 层被其他层 import..."
    if [ -d "src" ]; then
        violations=$(grep -rn "import.*controller" --include="*.java" src/main/java/*/service/ src/main/java/*/dao/ src/main/java/*/mapper/ 2>/dev/null || true)
        if [ -n "$violations" ]; then
            echo "  ❌ 发现 Controller 层被非法 import："
            echo "$violations"
            errors=$((errors + 1))
        else
            echo "  ✅ Controller 层依赖方向正确"
        fi
    else
        echo "  ⏭️  非 Java 项目，跳过 Java 分层检查"
    fi
}

# 检查 DAO 层不能 import Service
check_dao_imports() {
    echo "[检查] DAO 层是否 import Service..."
    if [ -d "src" ]; then
        violations=$(grep -rn "import.*service" --include="*.java" src/main/java/*/dao/ src/main/java/*/mapper/ 2>/dev/null || true)
        if [ -n "$violations" ]; then
            echo "  ❌ 发现 DAO 层 import Service："
            echo "$violations"
            errors=$((errors + 1))
        else
            echo "  ✅ DAO 层依赖方向正确"
        fi
    fi
}

check_controller_imports
check_dao_imports

if [ $errors -gt 0 ]; then
    echo ""
    echo "❌ 依赖方向检查失败：发现 $errors 处违规"
    exit 1
else
    echo ""
    echo "✅ 依赖方向检查通过"
    exit 0
fi
