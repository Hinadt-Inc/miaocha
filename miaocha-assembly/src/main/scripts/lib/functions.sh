#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 打印成功消息
print_success() {
    print_color "${GREEN}" "✅ $1"
}

# 打印错误消息
print_error() {
    print_color "${RED}" "❌ $1"
}

# 打印警告消息
print_warning() {
    print_color "${YELLOW}" "⚠️ $1"
}

# 打印信息消息
print_info() {
    print_color "${BLUE}" "ℹ️ $1"
}

# 检查命令是否存在
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# 检查JDK版本
check_java() {
    if ! command_exists java; then
        print_error "未找到Java，请安装JDK 17或更高版本"
        exit 1
    fi
    
    # 获取Java版本
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    java_major_version=$(echo $java_version | sed 's/^1\.//' | cut -d'.' -f1)
    
    if [ "$java_major_version" -lt 17 ]; then
        print_warning "检测到Java版本 $java_version, 建议使用JDK 17或更高版本"
    else
        print_info "检测到Java版本: $java_version"
    fi
}

# 查找进程ID
find_pid() {
    local jar_name=$1
    ps -ef | grep "java.*${jar_name}" | grep -v grep | awk '{print $2}'
}

# 显示应用状态
show_status() {
    local jar_name=$1
    local pid=$(find_pid "$jar_name")
    
    if [ -z "$pid" ]; then
        print_warning "应用未运行"
        return 1
    else
        print_success "应用正在运行，进程ID: $pid"
        return 0
    fi
}

# 创建目录（如果不存在）
ensure_dir() {
    local dir=$1
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir"
        print_info "创建目录: $dir"
    fi
}

# 显示ASCII标题
show_header() {
    # 获取版本
    APP_VERSION_FILE="${APP_HOME}/version.txt"
    if [ -f "$APP_VERSION_FILE" ]; then
        APP_VERSION=$(cat "$APP_VERSION_FILE")
    else
        APP_VERSION="1.0"
    fi
    
    echo -e "${CYAN}"
    echo "  ____  _____  ____  ______  ______  "
    echo " /    \\/     \\/    \\/      \\/      \\ "
    echo "/_______/_____/_______/\\      \\      \\"
    echo "|  ***  |  ***  |  ***  |  ***  |  ***  |"
    echo "|  ***  |  ***  |  ***  |  ***  |  ***  |"
    echo "|_______|_______|_______|_______|_______|"
    echo ""
    echo "   ____  __  __  ____  ______ "
    echo "  /    \\/  \\/  \\/    \\/      \\"
    echo " /_______/\\  /\\_______/\\      \\"
    echo " |  ***  |  |||  |  ***  |  ***  |"
    echo " |  ***  |  |||  |  ***  |  ***  |"
    echo " |_______|__|___|_______|_______|"
    echo -e "${NC}"
    echo -e "${PURPLE}=== 秒查(MiaoCha)日志搜索系统 v${APP_VERSION} ===${NC}"
    echo
}

# 检测操作系统
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "Linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macOS"
    elif [[ "$OSTYPE" == "cygwin" ]]; then
        echo "Windows/Cygwin"
    elif [[ "$OSTYPE" == "msys" ]]; then
        echo "Windows/Git Bash"
    elif [[ "$OSTYPE" == "win32" ]]; then
        echo "Windows"
    else
        echo "Unknown"
    fi
}

# 解析YAML配置文件 (简单版本)
get_yaml_value() {
    local file=$1
    local key=$2
    local value=$(grep "^[[:space:]]*$key:" "$file" | head -1 | sed 's/^[[:space:]]*'"$key"':[[:space:]]*//')
    echo "$value"
} 