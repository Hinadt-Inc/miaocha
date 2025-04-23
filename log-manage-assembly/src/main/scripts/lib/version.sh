#!/bin/bash

# 设置应用根目录
ROOT_DIR=$(cd "$(dirname "$0")/../.." || exit; pwd)
POM_FILE="$ROOT_DIR/pom.xml"
VERSION_FILE="$ROOT_DIR/version.txt"

# 从POM文件中提取版本号
if [ -f "$POM_FILE" ]; then
    VERSION=$(grep -oP '(?<=<version>)[^<]+' "$POM_FILE" | head -1)
    if [ -n "$VERSION" ]; then
        echo "$VERSION" > "$VERSION_FILE"
        echo "版本文件已创建: $VERSION_FILE (版本: $VERSION)"
        exit 0
    fi
fi

# 如果无法从POM文件中获取，则使用默认版本
echo "1.0" > "$VERSION_FILE"
echo "无法从POM文件中提取版本号，使用默认版本 1.0"
exit 1 