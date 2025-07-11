#!/bin/bash

# 设置应用根目录
APP_HOME=$(cd "$(dirname "$0")/.." || exit; pwd)
LIB_DIR="$APP_HOME/lib"
CONFIG_DIR="$APP_HOME/config"
JAR_FILE="$APP_HOME/miaocha-server.jar"
SCRIPTS_DIR="$APP_HOME/bin"
FUNCTIONS_LIB="$SCRIPTS_DIR/lib/functions.sh"
LOG_DIR="$APP_HOME/logs"
PID_FILE="$APP_HOME/application.pid"

# 导入函数库
if [ -f "$FUNCTIONS_LIB" ]; then
    source "$FUNCTIONS_LIB"
else
    echo "错误: 找不到函数库文件 $FUNCTIONS_LIB"
    exit 1
fi

# 显示欢迎标题
show_header

# 默认环境 (优先使用环境变量 SPRING_PROFILES_ACTIVE)
ACTIVE_PROFILE=${SPRING_PROFILES_ACTIVE:-dev}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env|--profile)
            ACTIVE_PROFILE="$2"
            shift 2
            ;;
        -d|--debug)
            DEBUG="true"
            shift
            ;;
        --mock-doris-data)
            MOCK_DORIS_HOST="$2"
            MOCK_DORIS_PORT="$3"
            MOCK_DORIS_USER="$4"
            MOCK_DORIS_PASSWORD="$5"
            MOCK_DORIS_COUNT="$6"
            MOCK_DORIS_STREAM_LOAD_PORT="$7"
            MOCK_MODE="true"
            shift 7
            ;;
        -h|--help)
            echo "用法: $(basename "$0") [选项]"
            echo "选项:"
            echo "  -e, --env, --profile PROFILE   设置活动环境 (默认: dev)"
            echo "  -d, --debug                    启用调试模式"
            echo "  --mock-doris-data HOST PORT USER PASSWORD COUNT [STREAM_LOAD_PORT]"
            echo "                                 Mock Doris日志数据"
            echo "                                 例如: --mock-doris-data 127.0.0.1 9030 root \"\" 10000 8040"
            echo "  -h, --help                     显示此帮助消息"
            exit 0
            ;;
        *)
            print_error "未知选项: $1"
            exit 1
            ;;
    esac
done

# 检查系统环境
print_info "检查系统环境..."
check_java

# 如果是Mock模式，执行数据生成工具
if [ "$MOCK_MODE" = "true" ]; then
    print_info "启动 Doris 日志数据 Mock 工具..."
    
    # 验证参数
    if [ -z "$MOCK_DORIS_HOST" ]; then
        print_error "Mock模式需要指定Doris主机地址"
        exit 1
    fi
    
    # 设置默认值
    MOCK_DORIS_PORT=${MOCK_DORIS_PORT:-9030}
    MOCK_DORIS_USER=${MOCK_DORIS_USER:-root}
    MOCK_DORIS_PASSWORD=${MOCK_DORIS_PASSWORD:-}
    MOCK_DORIS_COUNT=${MOCK_DORIS_COUNT:-10000}
    MOCK_DORIS_STREAM_LOAD_PORT=${MOCK_DORIS_STREAM_LOAD_PORT:-8040}
    
    print_info "Mock参数: Host=$MOCK_DORIS_HOST, Port=$MOCK_DORIS_PORT, StreamLoadPort=$MOCK_DORIS_STREAM_LOAD_PORT, User=$MOCK_DORIS_USER, Count=$MOCK_DORIS_COUNT"
    
    # 构建类路径：config目录 + 主JAR + lib目录的所有JAR
    CLASSPATH="$CONFIG_DIR:$JAR_FILE:$LIB_DIR/*"
    
    # 执行Mock工具
    java -cp "$CLASSPATH" com.hinadt.miaocha.common.tools.LogSearchDataMockTool \
        --host="$MOCK_DORIS_HOST" \
        --port="$MOCK_DORIS_PORT" \
        --user="$MOCK_DORIS_USER" \
        --password="$MOCK_DORIS_PASSWORD" \
        --count="$MOCK_DORIS_COUNT" \
        --stream-load-port="$MOCK_DORIS_STREAM_LOAD_PORT"
    
    if [ $? -eq 0 ]; then
        print_success "Doris日志数据Mock完成！"
    else
        print_error "Doris日志数据Mock失败！"
        exit 1
    fi
    
    exit 0
fi

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    print_error "未找到主JAR文件 $JAR_FILE"
    exit 1
fi

# 确保日志目录存在
ensure_dir "$LOG_DIR"

# 检查应用是否已经运行
if show_status "miaocha-server"; then
    print_warning "应用已经在运行中，如需重启请先停止"
    exit 0
fi

# 构建JVM参数
JAVA_OPTS="-Xms1g -Xmx2g"

# 调试模式
if [ "$DEBUG" = "true" ]; then
    JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
    print_info "已启用调试模式，端口: 5005"
fi

# 附加系统属性
JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=$ACTIVE_PROFILE"
JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=file:$CONFIG_DIR/"
JAVA_OPTS="$JAVA_OPTS -Dlogging.config=$CONFIG_DIR/logback-spring.xml"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"

print_info "启动环境: $ACTIVE_PROFILE"
print_info "配置目录: $CONFIG_DIR"
print_info "JVM参数: $JAVA_OPTS"

# 启动应用
print_info "正在启动应用..."
# 构建完整的类路径：config目录 + 主JAR + lib目录的所有JAR
CLASSPATH="$CONFIG_DIR:$JAR_FILE:$LIB_DIR/*"
# 使用主类启动，因为这不是fat jar
nohup java $JAVA_OPTS -cp "$CLASSPATH" com.hinadt.miaocha.MiaoChaApp > "$LOG_DIR/startup.log" 2>&1 & APP_PID=$!

# 保存PID
echo $APP_PID > "$PID_FILE"

# 应用启动检查参数
MAX_WAIT_SECONDS=45
print_info "等待应用启动 (最多 ${MAX_WAIT_SECONDS} 秒)..."

# 启动时间
start_time=$(date +%s)
end_time=$((start_time + MAX_WAIT_SECONDS))
current_time=$start_time

# 动画字符
animation_chars=("-" "\\" "|" "/")
animation_idx=0

# 启动检查循环
while [ $current_time -lt $end_time ]; do
    # 更新动画
    echo -ne "\r[ ${animation_chars[$animation_idx]} ] 检查启动状态... "
    animation_idx=$(( (animation_idx + 1) % 4 ))

    # 检查进程是否仍在运行
    if ! ps -p $APP_PID > /dev/null; then
        echo -e "\r[ ❌ ] 进程已终止!"
        print_error "启动失败，进程已退出。请检查日志: $LOG_DIR/startup.log"
        cat "$LOG_DIR/startup.log" | tail -n 20
        exit 1
    fi

    # 检查日志中的成功标志 (支持多种应用名称)
    if grep -q -E "Started (LogManageSystemApplication|MiaoChaApp)" "$LOG_DIR/startup.log" 2>/dev/null; then
        echo -e "\r[ ✅ ] 应用启动成功!      "
        break
    fi

    # 等待一小段时间再检查
    sleep 1
    current_time=$(date +%s)
done

# 超时检查
if ! grep -q -E "Started (LogManageSystemApplication|MiaoChaApp)" "$LOG_DIR/startup.log" 2>/dev/null; then
    echo -e "\r[ ⚠️ ] 等待超时，但进程仍在运行      "
    print_warning "应用似乎仍在启动中，但已超过等待时间 (${MAX_WAIT_SECONDS}秒)"
    print_warning "请检查日志确认启动状态: $LOG_DIR/startup.log"
    print_warning "最近的日志内容:"
    cat "$LOG_DIR/startup.log" | tail -n 10
else
    print_success "应用启动成功! PID: $APP_PID"
    # 获取启动时间
    if grep -q -E "Started (LogManageSystemApplication|MiaoChaApp) in" "$LOG_DIR/startup.log"; then
        startup_time=$(grep -E "Started (LogManageSystemApplication|MiaoChaApp) in" "$LOG_DIR/startup.log" | sed -E 's/.*in ([0-9.]+) seconds.*/\1/')
        print_info "启动用时: ${startup_time} 秒"
    fi
    echo "查看完整日志: tail -f $LOG_DIR/startup.log"

    # 尝试获取应用的端口号
    PORT=$(get_yaml_value "$CONFIG_DIR/application-$ACTIVE_PROFILE.yml" "server.port")
    if [ -z "$PORT" ]; then
        PORT=$(get_yaml_value "$CONFIG_DIR/application.yml" "server.port")
    fi

    if [ -n "$PORT" ]; then
        print_info "应用访问地址: http://localhost:$PORT/"
        print_info "API文档: http://localhost:$PORT/swagger-ui"
    fi
fi

exit 0
