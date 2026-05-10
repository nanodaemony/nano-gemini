# Little Grid 部署指南

部署架构：
- MySQL + Redis：Docker 部署
- Spring Boot：直接 jar 包后台运行

---

## 目录

- [一、云服务器部署](#一云服务器部署)
- [二、本地开发配置](#二本地开发配置)

---

## 一、云服务器部署

### 1. 服务器环境准备

确保已安装：
- Docker + Docker Compose
- JDK 8
- (可选 Maven，不用在服务器编译)

### 2. 上传项目代码

```bash
# 克隆或上传项目到服务器
cd /path/to/nano-gemini
```

### 3. 配置 .env

```bash
cp .env.example .env
vim .env
```

修改密码：
```bash
DB_PWD=你的强密码
```

### 4. 启动 MySQL + Redis (Docker)

```bash
# 启动 Docker 服务
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f mysql
docker-compose logs -f redis
```

### 5. 本地打包 jar

在你本地电脑执行：

```bash
cd /path/to/nano-gemini
mvn clean package -DskipTests
```

打包好的 jar 位于：
```
grid-system/target/grid-system-2.7.jar
```

### 6. 上传 jar 到服务器

把 jar 传到服务器的相同目录下（保持目录结构）：

```
# 服务器目录结构应该是：
/path/to/nano-gemini/
├── .env
├── docker-compose.yml
├── deploy-server.sh
└── grid-system/
    └── target/
        └── grid-system-2.7.jar    <-- 上传到这里
```

用 scp 或其他工具上传：
```bash
# 本地执行
scp grid-system/target/grid-system-2.7.jar user@your-server:/path/to/nano-gemini/grid-system/target/
```

### 7. 服务器执行部署脚本

```bash
# 给脚本加执行权限
chmod +x deploy-server.sh

# 部署（停止旧的 + 启动新的）
./deploy-server.sh restart
```

### 8. 验证部署

```bash
# 查看状态
./deploy-server.sh status

# 查看日志
./deploy-server.sh logs

# 或者
tail -f logs/app.log
```

访问应用：
```
http://你的云服务器IP:8080
```

---

## 二、本地开发配置

### 1. 配置本地 .env

```bash
cp .env.local.example .env
vim .env
```

修改为你的配置：
```bash
DB_HOST=123.45.67.89    # 云服务器IP
DB_PORT=13306
DB_PWD=你的密码           # 和服务器一致
```

### 2. 本地启动项目

用 IDEA 或命令行启动项目。

---

## 三、常用命令

### 服务器上的操作

| 命令 | 说明 |
|------|------|
| `./deploy-server.sh restart` | 重启应用（常用） |
| `./deploy-server.sh stop` | 停止应用 |
| `./deploy-server.sh start` | 启动应用 |
| `./deploy-server.sh status` | 查看状态 |
| `./deploy-server.sh logs` | 查看日志 |
| `docker-compose ps` | 查看 MySQL/Redis 状态 |
| `docker-compose logs -f mysql` | 查看 MySQL 日志 |
| `docker-compose logs -f redis` | 查看 Redis 日志 |

### 更新代码重新部署

```bash
# 1. 本地打包
mvn clean package -DskipTests

# 2. 上传新 jar
scp ...

# 3. 服务器重启
./deploy-server.sh restart
```

---

## 四、端口说明

| 端口 | 服务 | 说明 |
|------|------|------|
| 8080 | Spring Boot | 对外提供 API |
| 13306 | MySQL | Docker 端口映射，可从本地连接 |
| 6379 | Redis | 仅本地访问，不对外暴露 |

---

## 五、本地连接云服务器 MySQL

```bash
mysql -h 云服务器IP -P 13306 -u root -p
```

或用 Navicat / DBeaver 等工具。

---

## 六、故障排查

### Spring Boot 启动失败

```bash
# 查看日志
./deploy-server.sh logs
```

常见问题：
- MySQL 连接不上？检查 `docker-compose ps` 确认 MySQL 容器在运行
- 端口被占用？修改 `.env` 里的 `SERVER_PORT`

### MySQL 连接不上

```bash
# 确认容器运行
docker-compose ps

# 查看 MySQL 日志
docker-compose logs -f mysql
```

### 查看应用是否在运行

```bash
# 方式1
./deploy-server.sh status

# 方式2
ps aux | grep grid-system

# 方式3
lsof -i :8080
```
