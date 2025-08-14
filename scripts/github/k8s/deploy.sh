#!/bin/bash

set -e

# 检查必要的环境变量
if [ -z "$PR_NUMBER" ]; then
    echo "❌ 错误: PR_NUMBER 环境变量未设置"
    exit 1
fi

if [ -z "$DOCKER_IMAGE" ]; then
    echo "❌ 错误: DOCKER_IMAGE 环境变量未设置"
    exit 1
fi

echo "🚀 开始部署 miaocha PR-$PR_NUMBER 环境"
echo "📦 Docker 镜像: $DOCKER_IMAGE"
echo "🔌 端口将由 K8S 自动分配"

# 生成部署时间戳
DEPLOYMENT_TIMESTAMP=$(date +%Y%m%d%H%M%S)
echo "🕐 部署时间戳: $DEPLOYMENT_TIMESTAMP"

# 临时目录
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# 复制并替换变量
for file in namespace.yml mysql-deployment.yml doris-deployment.yml miaocha-deployment.yml mysql-sync-job.yml; do
    if [ -f "scripts/github/k8s/$file" ]; then
        echo "🔧 处理 $file"
        sed -e "s/\${PR_NUMBER}/$PR_NUMBER/g" \
            -e "s|\${DOCKER_IMAGE}|$DOCKER_IMAGE|g" \
            -e "s/\${DEPLOYMENT_TIMESTAMP}/$DEPLOYMENT_TIMESTAMP/g" \
            -e "s/\${DEEPSEEK_API_KEY}/$DEEPSEEK_API_KEY/g" \
            "scripts/github/k8s/$file" > "$TEMP_DIR/$file"
    else
        echo "❌ 文件 scripts/github/k8s/$file 不存在"
        exit 1
    fi
done

# 应用 Kubernetes 清单
NAMESPACE="miaocha-pr-$PR_NUMBER"

echo "📋 创建/更新 namespace..."
kubectl apply -f "$TEMP_DIR/namespace.yml"

# 检查是否是首次部署（通过检查关键服务是否存在）
FIRST_DEPLOYMENT=true
if kubectl get deployment mysql -n "$NAMESPACE" >/dev/null 2>&1 && \
   kubectl get deployment doris -n "$NAMESPACE" >/dev/null 2>&1 && \
   kubectl get deployment miaocha -n "$NAMESPACE" >/dev/null 2>&1; then
    FIRST_DEPLOYMENT=false
    echo "🔄 检测到现有部署，将进行增量更新"
else
    echo "🆕 首次部署，将部署所有服务"
fi

if [ "$FIRST_DEPLOYMENT" = true ]; then
    echo "🗄️  部署 MySQL..."
    kubectl apply -f "$TEMP_DIR/mysql-deployment.yml"

    echo "📊 部署 Doris..."
    kubectl apply -f "$TEMP_DIR/doris-deployment.yml"

    echo "🚀 部署 Miaocha 应用..."
    kubectl apply -f "$TEMP_DIR/miaocha-deployment.yml"
else
    echo "🔄 更新 Miaocha 应用..."
    # 应用配置会自动触发滚动更新（由于时间戳变化）
    kubectl apply -f "$TEMP_DIR/miaocha-deployment.yml"

    echo "✅ MySQL 和 Doris 保持不变，仅更新应用"
fi

# 等待部署完成的函数
wait_for_deployment() {
    local app_name=$1
    local namespace=$2
    local timeout=${3:-300}

    echo "⏳ 等待 $app_name 部署完成..."

    # 检查 deployment 是否存在
    if ! kubectl get deployment $app_name -n $namespace >/dev/null 2>&1; then
        echo "❌ Deployment $app_name 不存在"
        return 1
    fi

    # 等待 deployment 就绪
    kubectl wait --for=condition=available deployment/$app_name -n $namespace --timeout=${timeout}s

    if [ $? -eq 0 ]; then
        echo "✅ $app_name 部署成功"
        return 0
    else
        echo "❌ $app_name 部署超时"
        return 1
    fi
}

# 等待部署完成
if [ "$FIRST_DEPLOYMENT" = true ]; then
    echo "⏳ 等待基础服务部署完成..."
    wait_for_deployment "mysql" "$NAMESPACE" 300
    wait_for_deployment "doris" "$NAMESPACE" 300

    # 首次部署时，启动数据同步任务
    echo "🔄 启动数据同步任务..."
    kubectl apply -f "$TEMP_DIR/mysql-sync-job.yml"

    # 等待数据同步完成
    echo "⏳ 等待数据同步完成..."
    kubectl wait --for=condition=complete job/mysql-sync-job -n $NAMESPACE --timeout=600s

    if [ $? -eq 0 ]; then
        echo "✅ 数据同步完成"
    else
        echo "⚠️ 数据同步可能失败，请检查日志"
        # 显示 Job 日志
        kubectl logs -l app=mysql-sync -n $NAMESPACE --tail=20
    fi

    # 继续等待应用部署
    echo "⏳ 等待应用部署完成..."
    wait_for_deployment "miaocha" "$NAMESPACE" 300
else
    echo "⏳ 等待应用更新完成..."
    # 只等待 Miaocha 应用更新
    wait_for_deployment "miaocha" "$NAMESPACE" 300

    # 检查滚动更新状态
    echo "🔄 检查滚动更新状态..."
    kubectl rollout status deployment/miaocha -n $NAMESPACE --timeout=300s
fi

echo "✅ 部署完成!"
echo ""

# 获取实际分配的端口信息
echo "🔍 获取服务端口信息..."
NAMESPACE="miaocha-pr-$PR_NUMBER"

# 获取节点 IP
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')
if [ -z "$NODE_IP" ]; then
    NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
fi

# 获取服务端口
MIAOCHA_PORT=$(kubectl get svc miaocha -n $NAMESPACE -o jsonpath='{.spec.ports[0].nodePort}')
MYSQL_PORT=$(kubectl get svc mysql -n $NAMESPACE -o jsonpath='{.spec.ports[0].nodePort}')
DORIS_HTTP_PORT=$(kubectl get svc doris -n $NAMESPACE -o jsonpath='{.spec.ports[?(@.name=="http")].nodePort}')
DORIS_STREAM_PORT=$(kubectl get svc doris -n $NAMESPACE -o jsonpath='{.spec.ports[?(@.name=="stream-load")].nodePort}')
DORIS_QUERY_PORT=$(kubectl get svc doris -n $NAMESPACE -o jsonpath='{.spec.ports[?(@.name=="query")].nodePort}')

echo ""
echo "🔗 访问地址:"
echo "  Miaocha 应用: http://$NODE_IP:$MIAOCHA_PORT"
echo "  Doris HTTP: http://$NODE_IP:$DORIS_HTTP_PORT"
echo "  Doris Stream Load: http://$NODE_IP:$DORIS_STREAM_PORT"
echo "  Doris Query: $NODE_IP:$DORIS_QUERY_PORT"
echo "  MySQL: $NODE_IP:$MYSQL_PORT"
echo ""
echo "🧹 清理命令:"
echo "  kubectl delete namespace miaocha-pr-$PR_NUMBER"
