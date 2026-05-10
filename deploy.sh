#!/bin/bash

set -e

cd "$(dirname "$0")"

echo ">>> 拉取最新代码..."
git pull origin master || git pull origin main

echo ">>> 停止旧容器..."
docker-compose down

echo ">>> 构建新镜像并启动..."
docker-compose up -d --build

echo ">>> 清理旧镜像..."
docker image prune -f

echo ">>> 部署完成！"
echo ">>> 查看日志: docker-compose logs -f app"
