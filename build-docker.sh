#!/bin/bash

# 退出时如果有错误
set -e

# 变量设置
IMAGE_NAME="log-manage-system"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
VERSION="dev-${TIMESTAMP}"

echo "===== 开始编译Maven项目 ====="
mvn clean package -DskipTests

JAR_FILE=$(ls target/*.jar | grep -v original)

# 检查是否成功打包
if [ ! -f $JAR_FILE ]; then
  echo "错误: 找不到Spring Boot可执行JAR包"
  echo "请确认pom.xml中的spring-boot-maven-plugin配置正确，并且应用类有@SpringBootApplication注解"
  exit 1
fi

echo "找到JAR包: $JAR_FILE"

echo "===== 构建Docker镜像: $IMAGE_NAME:$VERSION ====="
docker build -t "$IMAGE_NAME:$VERSION" .

echo "===== 构建完成 ====="
echo "镜像名称: $IMAGE_NAME:$VERSION"

# 设置环境变量
DB_URL="jdbc:mysql://10.0.19.212:3306/log_manage_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
PROFILE="prod"
DB_USER="root"
DB_PASS="root"
ENABLE_API_DOCS="true"
ENABLE_SWAGGER_UI="true"
LOGSTASH_PATH="/opt/logstash/logstash-9.0.0-linux-x86_64.tar.gz"

# 运行容器的示例命令
echo "===== 运行容器示例命令 ====="
echo "docker rm log-system --force"
echo "docker run -d -p 8080:8080 --name log-system \\"
echo "  -e \"SPRING_PROFILES_ACTIVE=${PROFILE}\" \\"
echo "  -e \"SPRING_DATASOURCE_URL=${DB_URL}\" \\"
echo "  -e \"SPRING_DATASOURCE_USERNAME=${DB_USER}\" \\"
echo "  -e \"SPRING_DATASOURCE_PASSWORD=${DB_PASS}\" \\"
echo "  -v \"${LOGSTASH_PATH}:/opt/logstash/logstash.tar.gz\" \\"
echo "  $IMAGE_NAME:$VERSION"

# 移除旧容器
docker rm log-system --force || true

# 运行新容器
docker run -d -p 8080:8080 --name log-system \
  -e "SPRING_PROFILES_ACTIVE=${PROFILE}" \
  -e "SPRING_DATASOURCE_URL=${DB_URL}" \
  -e "SPRING_DATASOURCE_USERNAME=${DB_USER}" \
  -e "SPRING_DATASOURCE_PASSWORD=${DB_PASS}" \
  -e "ENABLE_API_DOCS=${ENABLE_API_DOCS}" \
  -e "ENABLE_SWAGGER_UI=${ENABLE_SWAGGER_UI}" \
  -v "${LOGSTASH_PATH}:/opt/logstash/logstash-9.0.0-linux-x86_64.tar.gz" \
  "$IMAGE_NAME:$VERSION"