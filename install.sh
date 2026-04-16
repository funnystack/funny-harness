#!/bin/bash

# ============================================
# 配置区域 - 源文件位置（脚本所在目录）
# ============================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLAUDE_STRICT_SRC="$SCRIPT_DIR/templates/CLAUDE-team-template.md"
CLAUDE_USER_SRC="$SCRIPT_DIR/templates/CLAUDE-user-template.md"
HOOKS_SRC_DIR="$SCRIPT_DIR/hooks"
RULES_SRC_DIR="$SCRIPT_DIR/rules"
SKILLS_SRC_DIR="$SCRIPT_DIR/skills"

# 可选的语言规则
LANG_RULES=("golang" "java" "python" "typescript")
# ============================================

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检测操作系统
detect_os() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "linux"
    else
        echo "unknown"
    fi
}

# 第一步：输入用户名
get_username() {
    log_info "第一步：请输入您的名字（不超过15个字符）"
    read -p "请输入: " username

    # 校验：不超过15个字符
    char_count=$(echo -n "$username" | wc -m | tr -d ' ')
    if [ "$char_count" -gt 15 ]; then
        log_error "名字超过15个字符，请重新运行脚本"
        exit 1
    fi

    if [ -z "$username" ]; then
        log_error "名字不能为空"
        exit 1
    fi

    log_info "用户名设置成功: $username"
}

# 第二步：复制 CLAUDE-team-template.md 到系统目录
copy_strict_file() {
    log_info "第二步：复制 CLAUDE_team.md 到系统目录"

    # 检查源文件是否存在
    if [ ! -f "$CLAUDE_STRICT_SRC" ]; then
        log_error "源文件不存在: $CLAUDE_STRICT_SRC"
        exit 1
    fi

    local os=$(detect_os)
    local target_dir=""

    case $os in
        "macos")
            target_dir="/Library/Application Support/ClaudeCode"
            ;;
        "linux")
            target_dir="/etc/claude-code"
            ;;
        *)
            log_error "不支持的操作系统: $OSTYPE"
            exit 1
            ;;
    esac

    # 创建目录（需要 sudo 权限）
    if [ ! -d "$target_dir" ]; then
        log_info "创建目录: $target_dir"
        sudo mkdir -p "$target_dir"
        if [ $? -ne 0 ]; then
            log_error "创建目录失败"
            exit 1
        fi
    fi

    # 复制文件
    local target_file="$target_dir/CLAUDE.md"
    log_info "源文件: $CLAUDE_STRICT_SRC"
    log_info "目标位置: $target_file"

    sudo cp "$CLAUDE_STRICT_SRC" "$target_file"
    if [ $? -ne 0 ]; then
        log_error "复制 CLAUDE_team.md 失败"
        exit 1
    fi

    log_info "CLAUDE_team.md 复制成功"
}

# 第三步：复制 CLAUDE-user-template.md 到用户目录
copy_user_file() {
    log_info "第三步：复制 CLAUDE_user.md 到用户目录"

    # 检查源文件是否存在
    if [ ! -f "$CLAUDE_USER_SRC" ]; then
        log_error "源文件不存在: $CLAUDE_USER_SRC"
        exit 1
    fi

    local target_dir="$HOME/.claude"
    local target_file="$target_dir/CLAUDE.md"

    # 创建目录
    if [ ! -d "$target_dir" ]; then
        log_info "创建目录: $target_dir"
        mkdir -p "$target_dir"
        if [ $? -ne 0 ]; then
            log_error "创建目录失败"
            exit 1
        fi
    fi

    # 复制文件
    log_info "源文件: $CLAUDE_USER_SRC"
    log_info "目标位置: $target_file"

    cp "$CLAUDE_USER_SRC" "$target_file"
    if [ $? -ne 0 ]; then
        log_error "复制 CLAUDE_user.md 失败"
        exit 1
    fi

    # 替换 Boss 为用户名
    log_info "正在替换 'Boss' 为 '$username'"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS 的 sed 需要 -i 后面跟空字符串
        sed -i '' "s/Boss/$username/g" "$target_file"
    else
        # Linux 的 sed
        sed -i "s/Boss/$username/g" "$target_file"
    fi

    if [ $? -ne 0 ]; then
        log_error "替换用户名失败"
        exit 1
    fi

    log_info "CLAUDE_user.md 复制并替换成功"
}

# 第四步：安装 hooks 到用户目录并配置全局 settings.json
install_hooks() {
    log_info "第四步：安装 hooks"

    # 检查源目录是否存在
    if [ ! -d "$HOOKS_SRC_DIR" ]; then
        log_warn "hooks 源目录不存在，跳过 hooks 安装: $HOOKS_SRC_DIR"
        return 0
    fi

    local hooks_target_dir="$HOME/.claude/hooks"
    local script_target_dir="$hooks_target_dir/script"
    local global_settings="$HOME/.claude/settings.json"

    # 创建目标目录
    mkdir -p "$hooks_target_dir" "$script_target_dir"
    if [ $? -ne 0 ]; then
        log_error "创建 hooks 目录失败"
        exit 1
    fi

    # 复制 hooks 脚本（从 script 子目录，含子目录递归）
    if [ -d "$HOOKS_SRC_DIR/script" ]; then
        log_info "复制 hooks 脚本到: $script_target_dir"
        # 递归复制整个 script 目录内容（包括子目录如 metrics/）
        cp -r "$HOOKS_SRC_DIR/script"/* "$script_target_dir/" 2>/dev/null
        if [ $? -ne 0 ]; then
            log_warn "未找到 hooks 脚本文件"
        else
            # 设置所有 .sh 文件可执行权限（含子目录）
            find "$script_target_dir" -name "*.sh" -exec chmod +x {} \;
            log_info "hooks 脚本复制成功（含子目录）"
        fi
    else
        log_warn "hooks/script 源目录不存在，跳过脚本复制"
    fi

    # ---- 配置 settings.json ----
    # 备份已有 settings.json
    if [ -f "$global_settings" ]; then
        local backup_file="$global_settings.bak.$(date +%Y%m%d%H%M%S)"
        cp "$global_settings" "$backup_file"
        log_info "已备份 settings.json → $backup_file"
    fi

    # 遍历 hooks/*.json 合并到 settings.json
    local json_files=()
    for json_file in "$HOOKS_SRC_DIR"/*.json; do
        [ -f "$json_file" ] && json_files+=("$json_file")
    done

    if [ ${#json_files[@]} -eq 0 ]; then
        log_warn "未找到 hooks JSON 配置文件，跳过 settings.json 配置"
        return 0
    fi

    # 检查 jq 是否可用
    if ! command -v jq &> /dev/null; then
        log_error "未安装 jq 工具，无法自动合并 hooks 配置"
        log_error "请安装 jq 后重新运行，或手动将 hooks/*.json 内容合并到 $global_settings"
        return 1
    fi

    # 初始化 settings.json（如果不存在）
    if [ ! -f "$global_settings" ]; then
        echo '{"hooks":{}}' > "$global_settings"
        log_info "创建新的 settings.json"
    fi

    # 逐个合并 JSON 文件
    for json_file in "${json_files[@]}"; do
        local file_name=$(basename "$json_file")
        log_info "合并 hook 配置: $file_name"

        # 读取 JSON 文件中的 hooks 字段，深度合并到 settings.json
        local merged=$(jq --slurpfile hook_file "$json_file" '
            # 提取 hook 文件中的 hooks 对象
            $hook_file[0].hooks as $new_hooks |
            # 遍历新 hooks 的每个 event type（PreToolUse, PostToolUse 等）
            reduce ($new_hooks | keys[]) as $event_type (
                .;
                # 当前 settings 中该 event type 的已有条目
                (.hooks[$event_type] // []) as $existing_array |
                # 新增的条目
                $new_hooks[$event_type] as $new_entries |
                # 去重：过滤掉已存在的 matcher
                [ $new_entries[] | select(
                    .matcher as $m |
                    ($existing_array | map(.matcher) | index($m) | not)
                ) ] as $deduped_entries |
                # 合并：已有条目 + 去重后的新增条目
                .hooks[$event_type] = ($existing_array + $deduped_entries)
            )
        ' "$global_settings")

        if [ $? -ne 0 ]; then
            log_error "合并 $file_name 失败，跳过"
            continue
        fi

        echo "$merged" > "$global_settings"
    done

    log_info "全局 hooks 配置已写入: $global_settings"
    log_info "hooks 安装成功"
}

# 第五步：选择语言规则
select_lang_rules() {
    log_info "第五步：选择语言规则（可多选）"
    echo ""
    echo "可选的语言规则："
    echo "  1) golang"
    echo "  2) java"
    echo "  3) python"
    echo "  4) typescript"
    echo ""
    echo "请输入编号，多个用空格分隔（如: 1 3），直接回车跳过: "
    read -p "请选择: " selections

    selected_langs=()

    if [ -n "$selections" ]; then
        for num in $selections; do
            case $num in
                1) selected_langs+=("golang") ;;
                2) selected_langs+=("java") ;;
                3) selected_langs+=("python") ;;
                4) selected_langs+=("typescript") ;;
                *) log_warn "无效的编号: $num" ;;
            esac
        done
    fi

    if [ ${#selected_langs[@]} -eq 0 ]; then
        log_info "未选择任何语言规则，仅安装 common 规则"
    else
        log_info "已选择的语言规则: ${selected_langs[*]}"
    fi
}

# 第六步：安装 rules 到用户目录
install_rules() {
    log_info "第六步：安装 rules"

    local rules_target_dir="$HOME/.claude/rules"

    # 创建目标目录
    if [ ! -d "$rules_target_dir" ]; then
        log_info "创建目录: $rules_target_dir"
        mkdir -p "$rules_target_dir"
        if [ $? -ne 0 ]; then
            log_error "创建 rules 目录失败"
            exit 1
        fi
    fi

    # 安装 common 规则（必须）
    local common_src="$RULES_SRC_DIR/common"
    if [ -d "$common_src" ]; then
        log_info "安装 common 规则（必须）..."
        cp -r "$common_src" "$rules_target_dir/"
        if [ $? -ne 0 ]; then
            log_error "复制 common 规则失败"
            exit 1
        fi
        log_info "common 规则安装成功"
    else
        log_warn "common 规则源目录不存在: $common_src"
    fi

    # 安装用户选择的语言规则
    for lang in "${selected_langs[@]}"; do
        local lang_src="$RULES_SRC_DIR/$lang"
        if [ -d "$lang_src" ]; then
            log_info "安装 $lang 规则..."
            cp -r "$lang_src" "$rules_target_dir/"
            if [ $? -ne 0 ]; then
                log_warn "复制 $lang 规则失败"
            else
                log_info "$lang 规则安装成功"
            fi
        else
            log_warn "$lang 规则源目录不存在: $lang_src"
        fi
    done

    log_info "rules 安装完成"
}

# 第七步：安装 skills 到用户目录
install_skills() {
    log_info "第七步：安装 skills"

    if [ ! -d "$SKILLS_SRC_DIR" ]; then
        log_warn "skills 源目录不存在，跳过: $SKILLS_SRC_DIR"
        return 0
    fi

    local skills_target_dir="$HOME/.claude/skills"

    # 列出可用的 skills
    local skill_count=0
    echo ""
    echo "可用的 skills："
    for skill_dir in "$SKILLS_SRC_DIR"/*/; do
        [ -d "$skill_dir" ] || continue
        local skill_name=$(basename "$skill_dir")
        if [ -f "$skill_dir/SKILL.md" ]; then
            echo "  - $skill_name"
            ((skill_count++))
        fi
    done
    echo ""

    if [ "$skill_count" -eq 0 ]; then
        log_warn "未找到有效的 skill（缺少 SKILL.md）"
        return 0
    fi

    # 创建目标目录
    mkdir -p "$skills_target_dir"
    if [ $? -ne 0 ]; then
        log_error "创建 skills 目录失败"
        exit 1
    fi

    # 复制所有 skill（包含 SKILL.md 的目录）
    local installed=0
    for skill_dir in "$SKILLS_SRC_DIR"/*/; do
        [ -d "$skill_dir" ] || continue
        local skill_name=$(basename "$skill_dir")

        if [ ! -f "$skill_dir/SKILL.md" ]; then
            log_warn "跳过 $skill_name（缺少 SKILL.md）"
            continue
        fi

        local target="$skills_target_dir/$skill_name"

        # 如果已存在，先备份
        if [ -d "$target" ]; then
            local backup="$target.bak.$(date +%Y%m%d%H%M%S)"
            mv "$target" "$backup"
            log_info "已备份旧版本: $skill_name → $(basename "$backup")"
        fi

        # 复制整个 skill 目录（包括子目录中的模板文件）
        cp -r "$skill_dir" "$target"
        if [ $? -ne 0 ]; then
            log_error "复制 skill $skill_name 失败"
            continue
        fi

        log_info "安装 skill: $skill_name"
        ((installed++))
    done

    log_info "共安装 $installed 个 skill 到 $skills_target_dir"
}

# 第八步：可选安装 Metrics 数据上报 Cron
install_metrics_cron() {
    log_info "第八步（可选）：Metrics 数据上报 Cron"

    local metrics_script="$HOME/.claude/hooks/script/metrics/install-cron.sh"
    if [ ! -f "$metrics_script" ]; then
        log_warn "metrics cron 安装脚本不存在，跳过: $metrics_script"
        return 0
    fi

    echo ""
    echo "是否安装 Metrics 数据上报 Cron 任务？"
    echo "  - 功能：每 5 分钟自动将本地采集数据上报到服务端"
    echo "  - 需要：已配置 METRICS_SERVER 和 METRICS_API_KEY 环境变量"
    echo ""
    read -p "是否安装？(y/N): " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        bash "$metrics_script" install
    else
        log_info "跳过 Metrics Cron 安装（后续可手动执行 bash $metrics_script install）"
    fi
}

# 主函数
main() {
    echo "========================================"
    echo "    Harness 配置安装脚本"
    echo "========================================"
    echo ""

    # 检测系统
    local os=$(detect_os)
    log_info "检测到操作系统: $os"

    # 执行步骤
    get_username
    copy_strict_file
    copy_user_file
    install_hooks
    select_lang_rules
    install_rules
    install_skills
    install_metrics_cron

    echo ""
    echo "========================================"
    log_info "✅ 安装完成！"
    echo "========================================"
}

# 执行
main
