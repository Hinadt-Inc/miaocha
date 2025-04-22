FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 设置生产环境变量
ENV SPRING_PROFILES_ACTIVE=prod

# 创建日志目录和Logstash目录
RUN mkdir -p /app/logs /app/logs/archive /opt/logstash && \
    chmod -R 755 /opt/logstash

# 复制应用JAR包
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 80

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"] 