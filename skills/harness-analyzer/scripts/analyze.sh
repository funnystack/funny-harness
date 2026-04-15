#!/bin/bash
# Harness 差距分析脚本
# 快速扫描项目文件系统，输出各维度的基础检查结果
# 详细分析和评分由 SKILL.md 中的 AI 工作流完成

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== Harness 差距分析 ==="
echo "项目根目录: $PROJECT_ROOT"
echo ""

# 维度 1: AGENTS.md 完整度
echo "[维度1] AGENTS.md 完整度"
if [ -f "AGENTS.md" ]; then
    echo "  ✅ AGENTS.md 存在"
    grep -q "@" "AGENTS.md" 2>/dev/null && echo "  ✅ 包含文档引用" || echo "  ⚠️  缺少文档引用"
    grep -q "禁区\|禁止\|DENY\|deny" "AGENTS.md" 2>/dev/null && echo "  ✅ 包含禁区声明" || echo "  ⚠️  缺少禁区声明"
else
    echo "  ❌ AGENTS.md 不存在"
fi
echo ""

# 维度 2: docs/ 齐全度
echo "[维度2] docs/ 齐全度"
docs_required=(
    "docs/project-structure.md"
    "docs/architecture.md"
    "docs/architecture/tech-stack.md"
    "docs/architecture/api-design.md"
    "docs/architecture/database.md"
    "docs/business-context.md"
    "docs/implicit-contracts.md"
)
for doc in "${docs_required[@]}"; do
    if [ -f "$doc" ]; then
        lines=$(wc -l < "$doc")
        if [ "$lines" -gt 10 ]; then
            echo "  ✅ $doc (${lines} 行)"
        else
            echo "  ⚠️  $doc (${lines} 行，可能是空模板)"
        fi
    else
        echo "  ❌ $doc 缺失"
    fi
done
echo ""

# 维度 3: 分层 lint 覆盖
echo "[维度3] 分层 lint 覆盖"
lint_files=("scripts/verify/lint-deps.sh" "scripts/verify/lint-quality.sh" "scripts/verify/check-harness.sh")
for lint in "${lint_files[@]}"; do
    if [ -f "$lint" ]; then
        [ -x "$lint" ] && echo "  ✅ $lint (可执行)" || echo "  ⚠️  $lint (不可执行)"
    else
        echo "  ❌ $lint 缺失"
    fi
done
echo ""

# 维度 4: permissions 配置
echo "[维度4] permissions 配置"
if [ -f ".claude/settings.json" ]; then
    grep -q "deny\|DENY" ".claude/settings.json" 2>/dev/null && echo "  ✅ 包含 deny 规则" || echo "  ⚠️  未配置 deny 规则"
else
    echo "  ⚠️  .claude/settings.json 不存在"
fi
echo ""

# 维度 5: hooks 存在性
echo "[维度5] hooks 存在性"
if [ -f ".claude/settings.json" ]; then
    grep -q "hooks\|Hooks" ".claude/settings.json" 2>/dev/null && echo "  ✅ 包含 hooks 配置" || echo "  ⚠️  未配置 hooks"
else
    echo "  ⚠️  无法检查 hooks（settings.json 不存在）"
fi
echo ""

# 维度 6: 记忆机制
echo "[维度6] 记忆机制"
[ -d "harness/memory" ] && echo "  ✅ harness/memory/ 存在" || echo "  ❌ harness/memory/ 缺失"
[ -d "harness/trace" ] && echo "  ✅ harness/trace/ 存在" || echo "  ❌ harness/trace/ 缺失"
if [ -f "harness/memory/lessons-learned.md" ]; then
    lines=$(wc -l < "harness/memory/lessons-learned.md")
    [ "$lines" -gt 5 ] && echo "  ✅ lessons-learned.md 有内容 (${lines} 行)" || echo "  ⚠️  lessons-learned.md 可能是空模板"
fi
echo ""

# 维度 7: 变更流程
echo "[维度7] 变更流程"
[ -d "openspec" ] && echo "  ✅ openspec/ 存在" || echo "  ❌ openspec/ 缺失"
[ -f "openspec/config.yaml" ] && echo "  ✅ openspec/config.yaml 存在" || echo "  ⚠️  openspec/config.yaml 缺失"
[ -d "openspec/specs" ] && echo "  ✅ openspec/specs/ 存在" || echo "  ⚠️  openspec/specs/ 缺失"
echo ""

echo "=== 基础扫描完成 ==="
echo "详细评分和修复建议由 AI 按工作流生成。"
