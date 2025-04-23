#!/bin/bash

# 设置应用根目录
APP_HOME=$(cd "$(dirname "$0")/.." || exit; pwd)
SCRIPTS_DIR="$APP_HOME/bin"
FUNCTIONS_LIB="$SCRIPTS_DIR/lib/functions.sh"
PID_FILE="$APP_HOME/application.pid"

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
FORCE=false
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--force)
            FORCE=true
            shift
            ;;
        -h|--help)
            echo "用法: $(basename "$0") [选项]"
            echo "选项:"
            echo "  -f, --force    强制停止 (SIGKILL)"
            echo "  -h, --help     显示此帮助消息"
            exit 0
            ;;
        *)
            print_error "未知选项: $1"
            exit 1
            ;;
    esac
done

# 首先尝试从PID文件获取PID
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if [ -z "$PID" ]; then
        print_warning "PID文件为空"
        PID=""
    elif ! ps -p "$PID" >/dev/null; then
        print_warning "PID文件中的进程 ($PID) 不存在"
        PID=""
    fi
fi

# 如果PID文件无效，尝试通过进程名查找
if [ -z "$PID" ]; then
    PID=$(find_pid "log-manage-server")
    if [ -z "$PID" ]; then
        print_warning "应用未运行"
        rm -f "$PID_FILE" 2>/dev/null
        exit 0
    fi
fi

# 停止应用
print_info "正在停止应用 (PID: $PID)..."

if [ "$FORCE" = true ]; then
    # 强制停止
    print_warning "强制终止进程..."
    kill -9 "$PID"
    SIGNAL="SIGKILL"
else
    # 优雅停止
    kill "$PID"
    SIGNAL="SIGTERM"
fi

# 显示等待动画
for i in {1..10}; do
    if ! ps -p "$PID" >/dev/null; then
        break
    fi
    printf "."
    sleep 1
done
echo

# 检查进程是否已终止
if ps -p "$PID" >/dev/null; then
    print_error "无法停止应用，请尝试使用 --force 选项"
    exit 1
else
    print_success "应用已停止 (信号: $SIGNAL)"
    # 删除PID文件
    rm -f "$PID_FILE"
fi

exit 0 