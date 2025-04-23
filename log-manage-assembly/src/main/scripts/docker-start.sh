#!/bin/bash

# 显示欢迎信息
echo "================================================="
echo "   日志管理系统 - Docker容器版"
echo "================================================="

# 设置应用根目录
APP_HOME="/app"
CONFIG_DIR="$APP_HOME/config"
JAR_FILE="$APP_HOME/log-manage-server.jar"
LOG_DIR="$APP_HOME/logs"

# 确保目录存在
mkdir -p "$LOG_DIR" "$LOG_DIR/archive"

# 检查环境变量
echo "[INFO] 使用环境配置: $SPRING_PROFILES_ACTIVE"
echo "[INFO] 日志目录: $LOG_PATH"
echo "[INFO] JVM参数: $JAVA_OPTS"

# 检查数据库连接
if [[ -n "$SPRING_DATASOURCE_URL" ]]; then
  echo "[INFO] 数据库连接: $SPRING_DATASOURCE_URL"
else
  echo "[WARN] 未设置数据库连接，将使用配置文件中的默认值"
fi

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] 未找到主JAR文件 $JAR_FILE"
    exit 1
fi

# 使用exec确保Java进程接收容器的信号
echo "[INFO] 正在启动应用..."
exec java $JAVA_OPTS \
  -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
  -Dspring.config.location=file:$CONFIG_DIR/ \
  -Dlogging.config=$CONFIG_DIR/logback-spring.xml \
  -jar $JAR_FILE 