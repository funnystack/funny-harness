#!/usr/bin/env bash
# Cron 定时任务安装/卸载脚本
# 安装每 5 分钟执行一次数据上报的 cron 任务
# 用法：bash install-cron.sh [install|uninstall]
#
# @author funny2048
# @since 2026-04-11

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
UPLOAD_SCRIPT="$SCRIPT_DIR/upload-metrics.sh"
CRON_MARKER="# METRICS_UPLOAD_TASK"

install_cron() {
    # 检查是否已安装
    if crontab -l 2>/dev/null | grep -q "$CRON_MARKER"; then
        echo "Cron 任务已存在，跳过安装"
        return 0
    fi

    # 添加 cron 任务：每 5 分钟执行上报
    (crontab -l 2>/dev/null; echo "*/5 * * * * bash $UPLOAD_SCRIPT >> $HOME/.claude/metrics/upload.log 2>&1 $CRON_MARKER") | crontab -

    echo "Cron 任务安装成功：每 5 分钟执行 $UPLOAD_SCRIPT"
    echo "日志文件：$HOME/.claude/metrics/upload.log"
    echo "当前 crontab："
    crontab -l | grep "$CRON_MARKER"
}

uninstall_cron() {
    if ! crontab -l 2>/dev/null | grep -q "$CRON_MARKER"; then
        echo "Cron 任务不存在，无需卸载"
        return 0
    fi

    # 移除包含标记的行
    crontab -l 2>/dev/null | grep -v "$CRON_MARKER" | crontab -

    echo "Cron 任务卸载成功"
}

ACTION="${1:-install}"

case "$ACTION" in
    install)
        install_cron
        ;;
    uninstall)
        uninstall_cron
        ;;
    *)
        echo "用法: $0 [install|uninstall]"
        exit 1
        ;;
esac
