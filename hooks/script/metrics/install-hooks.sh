#!/usr/bin/env bash
# Hooks 安装脚本
# 自动配置 Claude Code settings.json 中的 Hooks 注册项
# 用法：bash install-hooks.sh [install|uninstall]
#
# @author funny2048
# @since 2026-04-11

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SETTINGS_FILE="$HOME/.claude/settings.json"

# 确保 settings.json 存在
mkdir -p "$(dirname "$SETTINGS_FILE")"
if [ ! -f "$SETTINGS_FILE" ]; then
    echo '{}' > "$SETTINGS_FILE"
fi

install_hooks() {
    echo "安装 Metrics Hooks..."

    python3 -c "
import json, sys

settings_file = '$SETTINGS_FILE'
script_dir = '$SCRIPT_DIR'

with open(settings_file, 'r') as f:
    settings = json.load(f)

# 初始化 hooks 结构
if 'hooks' not in settings:
    settings['hooks'] = {}

# SessionStart Hook
settings['hooks']['SessionStart'] = [{
    'type': 'command',
    'command': f'bash {script_dir}/hook-session-start.sh'
}]

# PreToolUse Hook
settings['hooks']['PreToolUse'] = [{
    'type': 'command',
    'command': f'bash {script_dir}/hook-pre-tool-use.sh'
}]

# Stop Hook
settings['hooks']['Stop'] = [{
    'type': 'command',
    'command': f'bash {script_dir}/hook-stop.sh'
}]

with open(settings_file, 'w') as f:
    json.dump(settings, f, indent=2, ensure_ascii=False)

print('Hooks 安装完成:')
print(f'  SessionStart -> {script_dir}/hook-session-start.sh')
print(f'  PreToolUse   -> {script_dir}/hook-pre-tool-use.sh')
print(f'  Stop         -> {script_dir}/hook-stop.sh')
print(f'配置文件: {settings_file}')
"
}

uninstall_hooks() {
    echo "卸载 Metrics Hooks..."

    python3 -c "
import json

settings_file = '$SETTINGS_FILE'

with open(settings_file, 'r') as f:
    settings = json.load(f)

if 'hooks' in settings:
    for key in ['SessionStart', 'PreToolUse', 'Stop']:
        settings['hooks'].pop(key, None)
    # 如果 hooks 为空则移除
    if not settings['hooks']:
        del settings['hooks']

with open(settings_file, 'w') as f:
    json.dump(settings, f, indent=2, ensure_ascii=False)

print('Hooks 卸载完成')
"
}

ACTION="${1:-install}"

case "$ACTION" in
    install)
        install_hooks
        ;;
    uninstall)
        uninstall_hooks
        ;;
    *)
        echo "用法: $0 [install|uninstall]"
        exit 1
        ;;
esac
