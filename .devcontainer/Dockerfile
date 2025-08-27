FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

ENV DEBIAN_FRONTEND=noninteractive

# Install prerequisites and OpenJDK 17 from Ubuntu (stable mirrors)
RUN apt-get update -y \
    && apt-get install -y --no-install-recommends \
        wget curl ca-certificates tar sudo openjdk-17-jdk \
    && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME for OpenJDK 17 on Ubuntu 22.04
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=${JAVA_HOME}/bin:${PATH}
