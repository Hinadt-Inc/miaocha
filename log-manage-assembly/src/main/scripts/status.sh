#!/bin/bash

# 设置应用根目录
APP_HOME=$(cd "$(dirname "$0")/.." || exit; pwd)
SCRIPTS_DIR="$APP_HOME/bin"
FUNCTIONS_LIB="$SCRIPTS_DIR/lib/functions.sh"
CONFIG_DIR="$APP_HOME/config"
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
        exit 1
    fi
fi

# 显示应用状态
print_success "应用正在运行 (PID: $PID)"

# 尝试获取应用当前的配置文件
JAVA_COMMAND=$(ps -p "$PID" -o command= 2>/dev/null)
ACTIVE_PROFILE=$(echo "$JAVA_COMMAND" | grep -o "spring.profiles.active=[^ ]*" | cut -d= -f2)
if [ -z "$ACTIVE_PROFILE" ]; then
    ACTIVE_PROFILE="default"
fi

# 尝试获取应用的内存使用情况
if command_exists jps && command_exists jstat; then
    print_info "内存使用情况:"
    VMID=$(jps | grep "$PID" | awk '{print $1}')
    if [ -n "$VMID" ]; then
        jstat -gc "$VMID" | head -n 1
        jstat -gc "$VMID" | tail -n 1
    fi
fi

# 尝试获取应用的端口号
PORT=$(get_yaml_value "$CONFIG_DIR/application-$ACTIVE_PROFILE.yml" "server.port")
if [ -z "$PORT" ]; then
    PORT=$(get_yaml_value "$CONFIG_DIR/application.yml" "server.port")
fi

if [ -n "$PORT" ]; then
    print_info "应用端口: $PORT"
    # 检查端口是否已被监听
    if command_exists netstat; then
        if netstat -tuln | grep -q ":$PORT "; then
            print_success "端口 $PORT 已经开放并正在监听"
        else
            print_warning "端口 $PORT 未监听，应用可能尚未完全启动"
        fi
    fi
    
    # 尝试检查健康检查接口
    if command_exists curl; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/actuator/health 2>/dev/null)
        if [ "$HTTP_CODE" = "200" ]; then
            print_success "健康检查: 正常"
        else
            print_warning "健康检查: 异常 (HTTP状态码: $HTTP_CODE)"
        fi
    fi
fi

# 显示运行时间
START_TIME=$(ps -p "$PID" -o lstart= 2>/dev/null)
if [ -n "$START_TIME" ]; then
    print_info "启动时间: $START_TIME"
    
    # 计算运行时间
    if command_exists ps && command_exists awk; then
        UPTIME=$(ps -p "$PID" -o etimes= | awk '{printf "%d天%02d时%02d分%02d秒", $1/86400, ($1%86400)/3600, ($1%3600)/60, $1%60}')
        print_info "运行时间: $UPTIME"
    fi
fi

# 显示环境信息
print_info "活动环境: $ACTIVE_PROFILE"

exit 0 