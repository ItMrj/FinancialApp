FROM openjdk:17-jdk-slim

LABEL maintainer="FinancialApp Team"

# 设置工作目录
WORKDIR /app

# 复制 Gradle 文件
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# 安装 Gradle
RUN apt-get update && \
    apt-get install -y wget unzip && \
    wget https://services.gradle.org/distributions/gradle-8.5-bin.zip && \
    unzip gradle-8.5-bin.zip -d /opt && \
    rm gradle-8.5-bin.zip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置 Gradle 环境变量
ENV GRADLE_HOME=/opt/gradle-8.5
ENV PATH=$PATH:$GRADLE_HOME/bin

# 构建应用
RUN gradle build --no-daemon

# 运行应用
CMD ["java", "-jar", "build/libs/financial-app-1.0.0.jar"]

# 暴露端口
EXPOSE 8080
