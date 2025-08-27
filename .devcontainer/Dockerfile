FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

ENV DEBIAN_FRONTEND=noninteractive

# Install prerequisites and Temurin JDK 17 via Adoptium APT repo
RUN apt-get update -y \
    && apt-get install -y --no-install-recommends \
        wget curl gnupg ca-certificates apt-transport-https tar sudo \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg \
    && . /etc/os-release \
    && echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${VERSION_CODENAME} main" > /etc/apt/sources.list.d/adoptium.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends temurin-17-jdk \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64
ENV PATH=${JAVA_HOME}/bin:${PATH}

# Install Apache Maven 3.9.9 (binary) with sha512 verification
ENV MAVEN_VERSION=3.9.9 \
    MAVEN_HOME=/opt/maven

RUN set -eux; \
    primary="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries"; \
    mirror1="https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries"; \
    archive="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries"; \
    for base in "$primary" "$mirror1" "$archive"; do \
      url_tgz="$base/apache-maven-${MAVEN_VERSION}-bin.tar.gz"; \
      url_sha="$url_tgz.sha512"; \
      if curl -fsSL "$url_sha" -o /tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz.sha512 \
         && curl -fsSL "$url_tgz" -o /tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz; then \
           break; \
      fi; \
    done; \
    cd /tmp; sha512sum -c apache-maven-${MAVEN_VERSION}-bin.tar.gz.sha512; \
    tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt; \
    ln -sfn /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME}; \
    ln -sfn ${MAVEN_HOME}/bin/mvn /usr/local/bin/mvn; \
    rm -f /tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz*;

ENV PATH=${MAVEN_HOME}/bin:${PATH}

