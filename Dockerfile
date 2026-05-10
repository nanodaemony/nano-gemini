# syntax=docker/dockerfile:1.4
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 设置 Maven 使用阿里云镜像
COPY settings.xml /usr/share/maven/ref/settings.xml

# 复制所有 pom.xml 文件
COPY pom.xml .
COPY grid-common/pom.xml grid-common/
COPY grid-logging/pom.xml grid-logging/
COPY grid-system/pom.xml grid-system/
COPY grid-tools/pom.xml grid-tools/
COPY grid-generator/pom.xml grid-generator/
COPY grid-app/pom.xml grid-app/

# 下载依赖 - 使用缓存挂载持久化 Maven 本地仓库
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# 复制源码
COPY grid-common/src grid-common/src
COPY grid-logging/src grid-logging/src
COPY grid-system/src grid-system/src
COPY grid-tools/src grid-tools/src
COPY grid-generator/src grid-generator/src
COPY grid-app/src grid-app/src

# 构建项目 - 使用缓存挂载
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

# 运行时镜像
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 复制 JAR 文件 - 使用 grid-system 作为主应用（exec classifier）
COPY --from=builder /app/grid-system/target/grid-system-2.7-exec.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

EXPOSE 8000

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
