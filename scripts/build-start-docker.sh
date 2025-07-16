#!/bin/bash

# 退出时如果有错误
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 获取脚本所在目录和项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 默认变量设置
IMAGE_NAME="miaocha"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
VERSION="dev-${TIMESTAMP}"
DOCKERFILE="$PROJECT_ROOT/docker/Dockerfile"

# 脚本模式选项
BUILD_ONLY=false
SKIP_TESTS=false
SKIP_MAVEN=false
AUTO_RUN=false
CUSTOM_VERSION=""
ENV_FILE=""

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

# 显示帮助信息
show_help() {
  cat << EOF
用法: $0 [选项]

选项:
  -b, --build-only        只构建镜像，不运行容器
  -r, --run              构建完成后自动运行容器
  -s, --skip-tests       跳过Maven测试
  -m, --skip-maven       跳过Maven构建（需要已有构建产物）
  -v, --version VERSION  指定镜像版本标签
  -n, --name NAME        指定镜像名称 (默认: miaocha)
  -e, --env-file FILE    指定环境变量文件 (格式: KEY=VALUE)
  -h, --help             显示此帮助信息

示例:
  $0                     # 默认模式：构建镜像后询问是否运行
  $0 -b                  # 只构建镜像
  $0 -r                  # 构建后自动运行容器
  $0 -b -s               # 只构建镜像，跳过测试
  $0 -v 1.0.0            # 指定版本标签
  $0 -b -m               # 只构建镜像，跳过Maven构建
  $0 -r -s -v latest     # 跳过测试，构建并自动运行，版本为latest
  $0 -e .env -r          # 使用环境变量文件，构建并自动运行

EOF
}

# 解析命令行参数
parse_args() {
  while [[ $# -gt 0 ]]; do
    case $1 in
      -b|--build-only)
        BUILD_ONLY=true
        shift
        ;;
      -r|--run)
        AUTO_RUN=true
        shift
        ;;
      -s|--skip-tests)
        SKIP_TESTS=true
        shift
        ;;
      -m|--skip-maven)
        SKIP_MAVEN=true
        shift
        ;;
      -v|--version)
        CUSTOM_VERSION="$2"
        shift 2
        ;;
      -n|--name)
        IMAGE_NAME="$2"
        shift 2
        ;;
      -e|--env-file)
        ENV_FILE="$2"
        shift 2
        ;;
      -h|--help)
        show_help
        exit 0
        ;;
      *)
        error "未知选项: $1"
        echo
        show_help
        exit 1
        ;;
    esac
  done
  
  # 如果指定了自定义版本，则使用它
  if [ -n "$CUSTOM_VERSION" ]; then
    VERSION="$CUSTOM_VERSION"
  fi
}

# 加载环境变量文件
load_env_file() {
  if [ -n "$ENV_FILE" ]; then
    if [ ! -f "$ENV_FILE" ]; then
      error "环境变量文件不存在: $ENV_FILE"
      exit 1
    fi
    
    info "正在加载环境变量文件: $ENV_FILE"
    # 读取环境变量文件并设置变量
    while IFS='=' read -r key value; do
      # 跳过空行和注释行
      if [[ -z "$key" || "$key" =~ ^[[:space:]]*# ]]; then
        continue
      fi
      
      # 移除前后空白字符
      key=$(echo "$key" | xargs)
      value=$(echo "$value" | xargs)
      
      # 移除value中的引号
      value=$(echo "$value" | sed 's/^["\x27]//;s/["\x27]$//')
      
      # 设置环境变量
      export "$key"="$value"
      info "设置环境变量: $key=$value"
    done < "$ENV_FILE"
  fi
}

# 解析命令行参数
parse_args "$@"

# 加载环境变量文件（如果指定）
load_env_file

# 显示脚本标题
echo "================================================="
echo "   日志管理系统 Docker 镜像构建脚本"
echo "================================================="
echo "项目根目录: $PROJECT_ROOT"
echo "镜像名称: $IMAGE_NAME"
echo "版本标签: $VERSION"
echo "构建模式: $([ "$BUILD_ONLY" = true ] && echo "只构建镜像" || echo "构建并可选运行")"
echo "Maven构建: $([ "$SKIP_MAVEN" = true ] && echo "跳过" || echo "执行")"
echo "Maven测试: $([ "$SKIP_TESTS" = true ] && echo "跳过" || echo "执行")"
echo "自动运行: $([ "$AUTO_RUN" = true ] && echo "是" || echo "否")"
echo "环境变量文件: $([ -n "$ENV_FILE" ] && echo "$ENV_FILE" || echo "无")"
echo "数据库主机: $DB_HOST"
echo "API文档启用: $ENABLE_API_DOCS"
echo "Swagger UI启用: $ENABLE_SWAGGER_UI"
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
  error "找不到Dockerfile，期望位置: $DOCKERFILE"
  exit 1
fi

if [ ! -f "$PROJECT_ROOT/miaocha-assembly/src/main/scripts/docker-start.sh" ]; then
  error "找不到Docker专用启动脚本，请确保docker-start.sh位于正确目录"
  exit 1
fi

# 切换到项目根目录进行Maven构建
cd "$PROJECT_ROOT"

# Maven构建步骤
if [ "$SKIP_MAVEN" = true ]; then
  warn "跳过Maven构建，使用现有构建产物"
else
  info "编译Maven项目..."
  
  # 构建Maven命令
  MAVEN_CMD="mvn clean package"
  if [ "$SKIP_TESTS" = true ]; then
    MAVEN_CMD="$MAVEN_CMD -DskipTests"
    info "跳过测试，执行命令: $MAVEN_CMD"
  else
    info "执行完整构建（包含测试）: $MAVEN_CMD"
  fi
  
  if ! $MAVEN_CMD; then
    error "Maven构建失败，请修复构建错误后再尝试"
    exit 1
  fi
  
  success "Maven构建完成"
fi

info "检查构建结果..."
# 查找最新的bin.tar.gz文件（按修改时间排序，选择最新的）
TAR_FILE=$(ls -t dist/*-bin.tar.gz 2>/dev/null | head -1 || echo "")

if [ -z "$TAR_FILE" ] || [ ! -f "$TAR_FILE" ]; then
  error "找不到打包的tar.gz文件"
  if [ "$SKIP_MAVEN" = true ]; then
    error "由于跳过了Maven构建，请先执行完整构建或移除 -m 选项"
  else
    error "请确认Maven构建正常完成，构建产物应在dist目录中"
  fi
  echo "当前dist目录内容:"
  ls -la dist/ 2>/dev/null || echo "dist目录不存在"
  exit 1
fi

success "找到压缩包: $TAR_FILE ($(du -h "$TAR_FILE" | cut -f1))"

info "构建Docker镜像: $IMAGE_NAME:$VERSION ..."
info "构建上下文: $PROJECT_ROOT"
info "Dockerfile路径: $DOCKERFILE"

# 使用绝对路径的Dockerfile进行构建
if ! docker build -f "$DOCKERFILE" -t "$IMAGE_NAME:$VERSION" "$PROJECT_ROOT"; then
  error "Docker镜像构建失败"
  exit 1
fi

success "Docker镜像构建完成!"
echo "镜像名称: $IMAGE_NAME:$VERSION"

# 设置环境变量（与配置文件对齐）
DB_HOST="${DB_HOST:-mysql}"                    # 对应 ${DB_HOST:mysql}
DB_PORT="${DB_PORT:-3306}"                     # 对应 ${DB_PORT:3306}
DB_NAME="${DB_NAME:-log_manage_system}"        # 对应 ${DB_NAME:log_manage_system}
DB_USER="${DB_USER:-root}"                     # 对应 ${DB_USER:root}
DB_PASSWORD="${DB_PASSWORD:-password}"         # 对应 ${DB_PASSWORD:password}
ENABLE_API_DOCS="${ENABLE_API_DOCS:-false}"    # 对应 ${ENABLE_API_DOCS:false}
ENABLE_SWAGGER_UI="${ENABLE_SWAGGER_UI:-false}"  # 对应 ${ENABLE_SWAGGER_UI:false}
JWT_SECRET="${JWT_SECRET:-8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92}"  # 对应 ${JWT_SECRET:...}
LOGSTASH_PACKAGE_PATH="${LOGSTASH_PACKAGE_PATH:-/opt/logstash/logstash-9.0.0-linux-x86_64.tar.gz}"  # 对应 ${LOGSTASH_PACKAGE_PATH}
LOGSTASH_DEPLOY_DIR="${LOGSTASH_DEPLOY_DIR:-/opt/logstash}"  # 对应 ${LOGSTASH_DEPLOY_DIR:/opt/logstash}
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

# 容器名称
CONTAINER_NAME="miaocha"

# 容器运行函数
run_container() {
  # 检查是否存在同名容器
  if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    info "发现同名容器，正在停止并移除..."
    docker rm $CONTAINER_NAME --force || true
  fi

  info "正在启动容器 $CONTAINER_NAME..."

  # 运行新容器
  if ! docker run -d -p 8080:8080 --name $CONTAINER_NAME \
    -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}" \
    -e "DB_HOST=${DB_HOST}" \
    -e "DB_PORT=${DB_PORT}" \
    -e "DB_NAME=${DB_NAME}" \
    -e "DB_USER=${DB_USER}" \
    -e "DB_PASSWORD=${DB_PASSWORD}" \
    -e "ENABLE_API_DOCS=${ENABLE_API_DOCS}" \
    -e "ENABLE_SWAGGER_UI=${ENABLE_SWAGGER_UI}" \
    -e "JWT_SECRET=${JWT_SECRET}" \
    -e "LOGSTASH_PACKAGE_PATH=${LOGSTASH_PACKAGE_PATH}" \
    -e "LOGSTASH_DEPLOY_DIR=${LOGSTASH_DEPLOY_DIR}" \
    -e "JAVA_OPTS=-Xms1g -Xmx2g" \
    -v "${LOGSTASH_PACKAGE_PATH}:${LOGSTASH_PACKAGE_PATH}" \
    -v "$PROJECT_ROOT/logs:/app/logs" \
    "$IMAGE_NAME:$VERSION"; then
    error "容器启动失败"
    exit 1
  fi

  success "容器已启动!"
  info "容器ID: $(docker ps -q -f name=$CONTAINER_NAME)"
  info "访问地址: http://localhost:8080"
  info "查看容器日志: docker logs -f $CONTAINER_NAME"
}

# 根据选项决定是否运行容器
if [ "$BUILD_ONLY" = true ]; then
  success "镜像构建完成，跳过容器运行"
elif [ "$AUTO_RUN" = true ]; then
  info "自动运行容器..."
  run_container
else
  # 输出分隔线
  echo
  echo "================================================="
  echo "   容器运行指南"
  echo "================================================="

  # 运行容器的示例命令
  info "容器运行命令示例:"
  echo "docker rm $CONTAINER_NAME --force"
  echo "docker run -d -p 8080:8080 --name $CONTAINER_NAME \\"
  echo "  -e \"SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}\" \\"
  echo "  -e \"DB_HOST=${DB_HOST}\" \\"
  echo "  -e \"DB_PORT=${DB_PORT}\" \\"
  echo "  -e \"DB_NAME=${DB_NAME}\" \\"
  echo "  -e \"DB_USER=${DB_USER}\" \\"
  echo "  -e \"DB_PASSWORD=${DB_PASSWORD}\" \\"
  echo "  -e \"ENABLE_API_DOCS=${ENABLE_API_DOCS}\" \\"
  echo "  -e \"ENABLE_SWAGGER_UI=${ENABLE_SWAGGER_UI}\" \\"
  echo "  -e \"JWT_SECRET=${JWT_SECRET}\" \\"
  echo "  -e \"LOGSTASH_PACKAGE_PATH=${LOGSTASH_PACKAGE_PATH}\" \\"
  echo "  -e \"LOGSTASH_DEPLOY_DIR=${LOGSTASH_DEPLOY_DIR}\" \\"
  echo "  -e \"JAVA_OPTS=-Xms1g -Xmx2g\" \\"
  echo "  -v \"${LOGSTASH_PACKAGE_PATH}:${LOGSTASH_PACKAGE_PATH}\" \\"
  echo "  -v \"$PROJECT_ROOT/logs:/app/logs\" \\"
  echo "  $IMAGE_NAME:$VERSION"
  echo

  # 询问是否要运行容器
  read -p "是否要运行容器? (y/n) " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    run_container
  fi
fi

echo
info "相关Docker命令:"
echo "查看镜像:  docker images | grep $IMAGE_NAME"
echo "移除镜像:  docker rmi $IMAGE_NAME:$VERSION"

# 如果没有选择只构建镜像，则显示容器相关命令
if [ "$BUILD_ONLY" != true ]; then
  echo "停止容器:  docker stop $CONTAINER_NAME"
  echo "启动容器:  docker start $CONTAINER_NAME"
  echo "重启容器:  docker restart $CONTAINER_NAME"
  echo "查看日志:  docker logs -f $CONTAINER_NAME"
  echo "进入容器:  docker exec -it $CONTAINER_NAME bash"
  echo "移除容器:  docker rm -f $CONTAINER_NAME"
fi

echo
success "脚本执行完成!"

# 显示使用建议
if [ "$BUILD_ONLY" = true ]; then
  echo
  info "提示: 使用 '$0 -r -v $VERSION' 可以直接运行此镜像"
elif [ "$AUTO_RUN" = true ]; then
  echo
  info "提示: 容器正在后台运行，使用 'docker logs -f $CONTAINER_NAME' 查看日志"
fi
