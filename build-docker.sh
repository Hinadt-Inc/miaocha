#!/bin/bash

# 退出时如果有错误
set -e

# 变量设置
IMAGE_NAME="log-manage-system"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
VERSION="dev-${TIMESTAMP}"
JAR_FILE=$(ls target/*.jar | grep -v original)

echo "===== 开始编译Maven项目 ====="
mvn clean package -DskipTests

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

# 运行容器的示例命令
echo "===== 运行容器示例命令 ====="
echo "docker rm log-system"
echo "docker run -d -p 8080:8080 --name log-system $IMAGE_NAME:$VERSION"

docker rm log-system
docker run -d -p 8080:8080 --name log-system $IMAGE_NAME:$VERSION