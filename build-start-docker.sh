#!/bin/bash

# 退出时如果有错误
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 变量设置
IMAGE_NAME="log-manage-system"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
VERSION="v1.0-${TIMESTAMP}"
DOCKERFILE="Dockerfile"

# 打印彩色信息
info() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warn() {
  echo -e "${YELLOW}[WARNING]${NC} $1"
}

error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# 显示脚本标题
echo "================================================="
echo "   日志管理系统 Docker 镜像构建脚本"
echo "================================================="
echo "镜像名称: $IMAGE_NAME"
echo "版本标签: $VERSION"
echo

# 检查docker是否可用
if ! command -v docker &> /dev/null; then
  error "未找到docker命令，请先安装Docker"
  exit 1
fi

info "检查Docker状态..."
if ! docker info &> /dev/null; then
  error "无法连接到Docker守护进程，请确保Docker服务已启动"
  exit 1
fi

# 检查必要的文件
info "检查项目文件..."
if [ ! -f "$DOCKERFILE" ]; then
  error "找不到Dockerfile，请确保在正确的目录中运行此脚本"
  exit 1
fi

if [ ! -f "log-manage-assembly/src/main/scripts/docker-start.sh" ]; then
  error "找不到Docker专用启动脚本，请确保docker-start.sh位于正确目录"
  exit 1
fi

info "编译Maven项目..."
if ! mvn clean package -DskipTests; then
  error "Maven构建失败，请修复构建错误后再尝试"
  exit 1
fi

info "检查构建结果..."
TAR_FILE=$(ls log-manage-assembly/target/log-manage-assembly-*-distribution.tar.gz 2>/dev/null || echo "")

if [ -z "$TAR_FILE" ] || [ ! -f "$TAR_FILE" ]; then
  error "找不到打包的tar.gz文件，请确认log-manage-assembly模块配置正确"
  exit 1
fi

success "找到压缩包: $TAR_FILE ($(du -h "$TAR_FILE" | cut -f1))"

info "构建Docker镜像: $IMAGE_NAME:$VERSION ..."
if ! docker build -t "$IMAGE_NAME:$VERSION" .; then
  error "Docker镜像构建失败"
  exit 1
fi

success "Docker镜像构建完成!"
echo "镜像名称: $IMAGE_NAME:$VERSION"

# 设置环境变量
DB_HOST="10.0.19.212"
DB_PORT="3306"
DB_NAME="log_manage_system"
DB_USER="root"
DB_PASS="root"
ENABLE_API_DOCS="true"
ENABLE_SWAGGER_UI="true"
LOGSTASH_PATH="/opt/logstash/logstash-9.0.0-linux-x86_64.tar.gz"
LOG_PATH="/app/logs"

# 容器名称
CONTAINER_NAME="log-system"

# 输出分隔线
echo
echo "================================================="
echo "   容器运行指南"
echo "================================================="

# 运行容器的示例命令
info "容器运行命令示例:"
echo "docker rm $CONTAINER_NAME --force"
echo "docker run -d -p 8080:8080 --name $CONTAINER_NAME \\"
echo "  -e \"SPRING_PROFILES_ACTIVE=prod\" \\"
echo "  -e \"SPRING_DATASOURCE_URL=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai\" \\"
echo "  -e \"SPRING_DATASOURCE_USERNAME=${DB_USER}\" \\"
echo "  -e \"SPRING_DATASOURCE_PASSWORD=${DB_PASS}\" \\"
echo "  -e \"ENABLE_API_DOCS=${ENABLE_API_DOCS}\" \\"
echo "  -e \"ENABLE_SWAGGER_UI=${ENABLE_SWAGGER_UI}\" \\"
echo "  -e \"LOG_PATH=${LOG_PATH}\" \\"
echo "  -e \"JAVA_OPTS=-Xms1g -Xmx2g\" \\"
echo "  -v \"${LOGSTASH_PATH}:/opt/logstash/logstash-9.0.0-linux-x86_64.tar.gz\" \\"
echo "  -v \"./logs:/app/logs\" \\"
echo "  $IMAGE_NAME:$VERSION"
echo

# 询问是否要运行容器
read -p "是否要运行容器? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  # 检查是否存在同名容器
  if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    info "发现同名容器，正在停止并移除..."
    docker rm $CONTAINER_NAME --force || true
  fi

  info "正在启动容器 $CONTAINER_NAME..."
  
  # 运行新容器
  if ! docker run -d -p 8080:8080 --name $CONTAINER_NAME \
    -e "SPRING_PROFILES_ACTIVE=prod" \
    -e "SPRING_DATASOURCE_URL=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai" \
    -e "SPRING_DATASOURCE_USERNAME=${DB_USER}" \
    -e "SPRING_DATASOURCE_PASSWORD=${DB_PASS}" \
    -e "ENABLE_API_DOCS=${ENABLE_API_DOCS}" \
    -e "ENABLE_SWAGGER_UI=${ENABLE_SWAGGER_UI}" \
    -e "LOG_PATH=${LOG_PATH}" \
    -e "JAVA_OPTS=-Xms1g -Xmx2g" \
    -v "${LOGSTASH_PATH}:/opt/logstash/logstash-9.0.0-linux-x86_64.tar.gz" \
    -v "./logs:/app/logs" \
    "$IMAGE_NAME:$VERSION"; then
    error "容器启动失败"
    exit 1
  fi
  
  success "容器已启动!"
  info "容器ID: $(docker ps -q -f name=$CONTAINER_NAME)"
  info "访问地址: http://localhost:8080"
  info "查看容器日志: docker logs -f $CONTAINER_NAME"
fi

echo
info "相关Docker命令:"
echo "停止容器:  docker stop $CONTAINER_NAME"
echo "启动容器:  docker start $CONTAINER_NAME"
echo "重启容器:  docker restart $CONTAINER_NAME"
echo "查看日志:  docker logs -f $CONTAINER_NAME"
echo "进入容器:  docker exec -it $CONTAINER_NAME bash"
echo "移除容器:  docker rm -f $CONTAINER_NAME"
echo "移除镜像:  docker rmi $IMAGE_NAME:$VERSION"

echo
success "脚本执行完成!"