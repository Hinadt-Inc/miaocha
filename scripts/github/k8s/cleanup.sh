#!/bin/bash

set -e

# 检查必要的环境变量
if [ -z "$PR_NUMBER" ]; then
    echo "❌ 错误: PR_NUMBER 环境变量未设置"
    exit 1
fi

NAMESPACE="miaocha-pr-$PR_NUMBER"

echo "🧹 开始清理 PR-$PR_NUMBER 环境"

# 检查 namespace 是否存在
if kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
    echo "🔍 找到 namespace: $NAMESPACE"
    
    # 显示将要删除的资源
    echo "📋 即将删除以下资源:"
    kubectl get all -n "$NAMESPACE" --no-headers 2>/dev/null | head -20
    
    # 删除 namespace (这会删除其中的所有资源)
    echo "🗑️  删除 namespace 及其所有资源..."
    kubectl delete namespace "$NAMESPACE" --timeout=300s
    
    echo "✅ 清理完成!"
else
    echo "⚠️  namespace $NAMESPACE 不存在，无需清理"
fi

echo "🎉 PR-$PR_NUMBER 环境已清理完毕" 