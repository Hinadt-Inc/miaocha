FROM eclipse-temurin:17-jre-jammy

# 使用官方 Ubuntu 镜像源，确保 ARM64 平台支持
# 国内镜像源对 ARM64 支持不完整，使用官方源最稳定
RUN cp /etc/apt/sources.list /etc/apt/sources.list.bak

# 更新包管理器和安装必要工具 (Ubuntu/Debian 系统)
RUN apt-get update && \
    apt-get install -y bash curl procps tzdata && \
    ln -fs /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    dpkg-reconfigure -f noninteractive tzdata && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 环境变量设置
ENV SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs \
    JAVA_OPTS="-Xms1g -Xmx2g -Dfile.encoding=UTF-8"

# 创建必要的目录
RUN mkdir -p /app/logs /app/logs/archive /opt/logstash && \
    chmod -R 755 /app/logs /opt/logstash

# 复制打包后的tar.gz文件
COPY dist/*-bin.tar.gz /tmp/application.tar.gz

# 解压应用文件到工作目录
RUN tar -xzf /tmp/application.tar.gz -C /app --strip-components=1 && \
    rm /tmp/application.tar.gz && \
    chmod +x /app/bin/*.sh

# 暴露端口
EXPOSE 8080

# 启动应用 - 使用项目中提供的Docker专用启动脚本
ENTRYPOINT ["/bin/bash", "/app/bin/docker-start.sh"]
