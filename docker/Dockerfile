FROM eclipse-temurin:17-jre-alpine

# 配置 Alpine Linux 国内镜像源 - 使用多个备选源
RUN echo "https://mirrors.tuna.tsinghua.edu.cn/alpine/v3.21/main" > /etc/apk/repositories && \
    echo "https://mirrors.tuna.tsinghua.edu.cn/alpine/v3.21/community" >> /etc/apk/repositories && \
    echo "https://mirrors.ustc.edu.cn/alpine/v3.21/main" >> /etc/apk/repositories && \
    echo "https://mirrors.ustc.edu.cn/alpine/v3.21/community" >> /etc/apk/repositories

# 添加必要的系统工具
RUN apk add --no-cache bash curl procps tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

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
COPY miaocha-assembly/target/*-distribution.tar.gz /tmp/application.tar.gz

# 解压应用文件到工作目录
RUN tar -xzf /tmp/application.tar.gz -C /app --strip-components=1 && \
    rm /tmp/application.tar.gz && \
    chmod +x /app/bin/*.sh

# 暴露端口
EXPOSE 8080

# 启动应用 - 使用项目中提供的Docker专用启动脚本
ENTRYPOINT ["/bin/bash", "/app/bin/docker-start.sh"]
