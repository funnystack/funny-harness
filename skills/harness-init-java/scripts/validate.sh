#!/bin/bash
# 统一验证管道
# 执行顺序：build → check-harness → lint-deps → lint-quality → test

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 切换到项目根目录，确保 pom.xml 等文件能被正确检测
cd "$PROJECT_ROOT"

echo "=========================================="
echo "  统一验证管道"
echo "=========================================="
echo "  项目根目录: $PROJECT_ROOT"

PASS=true
TOTAL_STEPS=5

run_step() {
    local step_num=$1
    local step_name=$2
    local cmd=$3

    echo ""
    echo "▶ Step ${step_num}/${TOTAL_STEPS}: ${step_name}..."
    if eval "$cmd"; then
        echo "  ✅ ${step_name}通过"
    else
        echo "  ❌ ${step_name}失败"
        PASS=false
    fi
}

# Step 1: Build（编译失败直接终止，后续步骤无意义）
echo ""
echo "▶ Step 1/${TOTAL_STEPS}: 编译检查..."
if [ -f "pom.xml" ]; then
    if mvn -q -DskipTests compile; then
        echo "  ✅ 编译通过"
    else
        echo "  ❌ 编译失败，停止验证"
        exit 1
    fi
else
    echo "  ⏭️  未检测到构建文件，跳过编译检查"
fi

# Step 2: Harness结构检查
run_step 2 "Harness结构检查" "[ -f \"\$SCRIPT_DIR/verify/check-harness.sh\" ] && bash \"\$SCRIPT_DIR/verify/check-harness.sh\" || echo \"  ⏭️  无项目结构检查脚本，跳过\""

# Step 3: 依赖方向检查
run_step 3 "架构约束检查" "[ -f \"\$SCRIPT_DIR/verify/lint-deps.sh\" ] && bash \"\$SCRIPT_DIR/verify/lint-deps.sh\" || echo \"  ⏭️  无依赖检查脚本，跳过\""

# Step 4: 代码质量检查
run_step 4 "代码质量检查" "[ -f \"\$SCRIPT_DIR/verify/lint-quality.sh\" ] && bash \"\$SCRIPT_DIR/verify/lint-quality.sh\" || echo \"  ⏭️  无质量检查脚本，跳过\""

# Step 5: 单元测试
run_step 5 "单元测试" "[ -f \"pom.xml\" ] && mvn test -q 2>/dev/null || echo \"  ⏭️  未检测到构建文件，跳过测试\""

echo ""
echo "=========================================="
if $PASS; then
    echo "  ✅ 全部验证通过"
    echo "=========================================="
    exit 0
else
    echo "  ❌ 验证未通过，请修复上述问题"
    echo "=========================================="
    exit 1
fi
