#!/bin/bash

# 设置应用根目录
APP_HOME=$(cd "$(dirname "$0")/.." || exit; pwd)
SCRIPTS_DIR="$APP_HOME/bin"
FUNCTIONS_LIB="$SCRIPTS_DIR/lib/functions.sh"

# 导入函数库
if [ -f "$FUNCTIONS_LIB" ]; then
    source "$FUNCTIONS_LIB"
else
    echo "错误: 找不到函数库文件 $FUNCTIONS_LIB"
    exit 1
fi

# 显示标题
show_header

# 解析命令行参数
ARGS=""
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env|--profile)
            ARGS="$ARGS -e $2"
            shift 2
            ;;
        -d|--debug)
            ARGS="$ARGS -d"
            shift
            ;;
        -f|--force)
            FORCE="-f"
            shift
            ;;
        -h|--help)
            echo "用法: $(basename "$0") [选项]"
            echo "选项:"
            echo "  -e, --env, --profile PROFILE   设置活动环境"
            echo "  -d, --debug                    启用调试模式"
            echo "  -f, --force                    强制停止 (SIGKILL)"
            echo "  -h, --help                     显示此帮助消息"
            exit 0
            ;;
        *)
            print_error "未知选项: $1"
            exit 1
            ;;
    esac
done

# 停止应用
print_info "正在重启应用..."
print_info "步骤 1/2: 停止应用"

# 执行停止脚本
"$SCRIPTS_DIR/stop.sh" $FORCE

# 启动应用
print_info "步骤 2/2: 启动应用"

# 给进程一些时间完全停止
sleep 2

# 执行启动脚本
"$SCRIPTS_DIR/start.sh" $ARGS

exit $? 