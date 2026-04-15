#!/bin/bash

# 危险操作检测钩子 (PreToolUse)
# 在执行 Bash 命令前检测并拦截危险操作

# 读取 stdin 的 JSON 输入
input=$(cat)
tool_name=$(echo "$input" | jq -r '.tool_name // empty')
command=$(echo "$input" | jq -r '.tool_input.command // empty')

# 如果不是 Bash 工具，直接放行
if [ "$tool_name" != "Bash" ]; then
    echo '{}'
    exit 0
fi

# 如果没有命令，直接放行
if [ -z "$command" ]; then
    echo '{}'
    exit 0
fi

# 危险命令模式列表
# 格式: "模式|描述"
dangerous_patterns=(
    "rm -rf|删除目录及其内容"
    "rm -fr|删除目录及其内容"
    "dd if=|磁盘写入操作"
    "mkfs|格式化磁盘"
    "shutdown|关机命令"
    "reboot|重启命令"
    "init 0|关机命令"
    "init 6|重启命令"
    ":(){ :|:& };:|Fork 炸弹"
    "> /dev/sd|直接写入磁盘设备"
    "chmod -R 777|递归修改权限为 777"
    "chown -R.*root|递归修改所有者为 root"
    "curl.*|.*sh|从网络下载并执行脚本"
    "wget.*|.*sh|从网络下载并执行脚本"
    "git push.*--force|强制推送"
    "git push.*-f|强制推送"
    "DROP DATABASE|删除数据库"
    "DROP TABLE|删除表"
    "TRUNCATE|清空表数据"
    "DELETE FROM|删除数据"
)

# 检查命令是否匹配危险模式
for pattern_desc in "${dangerous_patterns[@]}"; do
    pattern="${pattern_desc%%|*}"
    desc="${pattern_desc##*|}"

    if echo "$command" | grep -qiE "$pattern"; then
        echo "⚠️  检测到危险操作: [$desc]"
        echo "   命令: $command"
        # 返回 JSON 阻止操作
        cat <<EOF
{
  "block": true,
  "message": "⚠️ 危险操作已被拦截: $desc\n命令: $command\n如需执行，请手动在终端运行"
}
EOF
        exit 2
    fi
done
# 这里关键是 exit 2。退出码 2 的意思是：拒绝这次操作，并把错误信息反馈给 Claude，让它想出更安全的替代方案。退出码 0 是放行，其他码是记录警告但不阻断。
# 安全，允许执行
echo '{}'
