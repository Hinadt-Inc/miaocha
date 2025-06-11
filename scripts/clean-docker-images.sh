#!/bin/bash

# 退出时如果有错误
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# 容器名称和镜像名称
CONTAINER_NAME="log-system"
IMAGE_NAME="log-manage-system"

echo "================================================="
echo "   Docker镜像清理脚本"
echo "================================================="

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

# 查找正在运行的容器使用的镜像
RUNNING_IMAGE=$(docker ps --filter "name=$CONTAINER_NAME" --format "{{.Image}}" 2>/dev/null)

if [ -z "$RUNNING_IMAGE" ]; then
  warn "没有找到正在运行的 $CONTAINER_NAME 容器"
  
  # 查找是否有停止的容器
  STOPPED_CONTAINER=$(docker ps -a --filter "name=$CONTAINER_NAME" --format "{{.ID}}" 2>/dev/null)
  
  if [ -n "$STOPPED_CONTAINER" ]; then
    RUNNING_IMAGE=$(docker ps -a --filter "name=$CONTAINER_NAME" --format "{{.Image}}" 2>/dev/null)
    info "找到已停止的容器 $CONTAINER_NAME，使用的镜像: $RUNNING_IMAGE"
  else
    warn "未找到任何 $CONTAINER_NAME 容器，将删除所有 $IMAGE_NAME 镜像"
    
    # 确认是否继续
    read -p "确定要删除所有 $IMAGE_NAME 镜像吗? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      info "操作已取消"
      exit 0
    fi
    
    # 删除所有相关镜像
    IMAGES_TO_DELETE=$(docker images "$IMAGE_NAME" --format "{{.ID}}" 2>/dev/null)
    if [ -n "$IMAGES_TO_DELETE" ]; then
      info "正在删除所有 $IMAGE_NAME 镜像..."
      echo "$IMAGES_TO_DELETE" | xargs -r docker rmi -f
      success "所有 $IMAGE_NAME 镜像已删除"
    else
      info "没有找到任何 $IMAGE_NAME 镜像"
    fi
    
    exit 0
  fi
else
  info "找到正在运行的容器 $CONTAINER_NAME，使用的镜像: $RUNNING_IMAGE"
fi

# 列出所有相关镜像
ALL_IMAGES=$(docker images "$IMAGE_NAME" --format "{{.Repository}}:{{.Tag}}" 2>/dev/null)
IMAGE_COUNT=$(echo "$ALL_IMAGES" | wc -l)

if [ -z "$ALL_IMAGES" ]; then
  info "没有找到任何 $IMAGE_NAME 镜像"
  exit 0
fi

info "找到 $IMAGE_COUNT 个 $IMAGE_NAME 镜像:"
echo "$ALL_IMAGES" | nl

# 过滤出要删除的镜像
IMAGES_TO_DELETE=$(echo "$ALL_IMAGES" | grep -v "^$RUNNING_IMAGE$" 2>/dev/null || true)

if [ -z "$IMAGES_TO_DELETE" ]; then
  info "没有需要删除的镜像，当前只有正在使用的镜像 $RUNNING_IMAGE"
  exit 0
fi

# 显示要删除的镜像
echo
info "以下镜像将被删除:"
echo "$IMAGES_TO_DELETE" | nl

# 确认删除
echo
read -p "确定要删除这些镜像吗? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  info "操作已取消"
  exit 0
fi

# 执行删除
echo
info "正在删除未使用的镜像..."
ERROR_COUNT=0
for IMG in $IMAGES_TO_DELETE; do
  echo -n "删除 $IMG... "
  if docker rmi "$IMG" &>/dev/null; then
    echo -e "${GREEN}成功${NC}"
  else
    echo -e "${RED}失败${NC}"
    ERROR_COUNT=$((ERROR_COUNT + 1))
  fi
done

# 显示结果
echo
if [ $ERROR_COUNT -eq 0 ]; then
  success "清理完成！所有未使用的 $IMAGE_NAME 镜像已删除"
else
  warn "清理完成，但有 $ERROR_COUNT 个镜像删除失败"
  info "可能是因为这些镜像被其他容器或标签引用"
  info "可以尝试使用 'docker rmi -f <镜像ID>' 强制删除"
fi

# 显示保留的镜像
REMAINING_IMAGES=$(docker images "$IMAGE_NAME" --format "{{.Repository}}:{{.Tag}}")
echo
info "保留的镜像:"
echo "$REMAINING_IMAGES" | nl 