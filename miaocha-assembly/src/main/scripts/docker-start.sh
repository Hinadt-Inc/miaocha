#!/bin/bash

# 显示欢迎信息
echo "================================================="
echo "   秒查 - Docker容器版"
echo "================================================="

# 设置应用根目录
APP_HOME="/app"
CONFIG_DIR="$APP_HOME/config"
JAR_FILE="$APP_HOME/miaocha-server.jar"
LOG_DIR="$APP_HOME/logs"

# 查找实际的JAR文件（优先使用exec版本）
if [ -f "$APP_HOME"/miaocha-server-*-exec.jar ]; then
    JAR_FILE=$(ls "$APP_HOME"/miaocha-server-*-exec.jar | head -1)
elif [ -f "$APP_HOME"/miaocha-server.jar ]; then
    JAR_FILE="$APP_HOME/miaocha-server.jar"
else
    echo "[ERROR] 未找到可执行的JAR文件"
    exit 1
fi

echo "[INFO] 使用JAR文件: $JAR_FILE"

# 确保目录存在
mkdir -p "$LOG_DIR" "$LOG_DIR/archive"

# 显示环境配置信息
ACTIVE_PROFILE=${SPRING_PROFILES_ACTIVE:-dev}
echo "[INFO] 使用环境配置: $ACTIVE_PROFILE"
echo "[INFO] 日志目录: ${LOG_PATH:-$LOG_DIR}"
echo "[INFO] JVM参数: ${JAVA_OPTS:--Xms1g -Xmx2g -Dfile.encoding=UTF-8}"

# 检查数据库连接
if [[ -n "$SPRING_DATASOURCE_URL" ]]; then
  echo "[INFO] 数据库连接: $SPRING_DATASOURCE_URL"
else
  echo "[WARN] 未设置数据库连接，将使用配置文件中的默认值"
fi

# 使用exec确保Java进程接收容器的信号
echo "[INFO] 正在启动应用..."
exec java ${JAVA_OPTS:--Xms1g -Xmx2g -Dfile.encoding=UTF-8} \
  -Dspring.profiles.active=$ACTIVE_PROFILE \
  -Dspring.config.location=file:$CONFIG_DIR/ \
  -Dlogging.config=$CONFIG_DIR/logback-spring.xml \
  -jar $JAR_FILE
