# 原生部署指南

直接在云服务器部署 Little Grid，不使用 Docker。

## 配置 .env 文件（重要！）

部署前先配置数据库和 Redis 信息：

```bash
# 复制配置模板
cp .env.example .env

# 编辑配置文件
vim .env   # 或 nano .env
```

修改以下内容：
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=eladmin
DB_USER=root
DB_PWD=你的MySQL密码

# Redis 配置（没有密码就留空）
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PWD=

# 环境配置
SPRING_PROFILES_ACTIVE=prod  # 或 dev
SERVER_PORT=8000
```

## 环境要求

- **操作系统**: Ubuntu 20.04+ / Debian 11+ / CentOS 7+
- **Java**: JDK 8
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 5.0+

## 快速开始

### 1. 一键部署

```bash
# 给脚本添加执行权限
chmod +x deploy-native.sh

# 完整部署（自动安装依赖 + 编译 + 启动）
./deploy-native.sh all
```

### 2. 分步部署

```bash
# 1. 安装依赖（Java, Maven, MySQL, Redis）
./deploy-native.sh install

# 2. 创建数据库
mysql -u root -p
```

在 MySQL 命令行中执行：
```sql
CREATE DATABASE eladmin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;
```

```bash
# 3. 编译项目
./deploy-native.sh build

# 4. 启动应用
./deploy-native.sh start
```

## 常用命令

| 命令 | 说明 |
|------|------|
| `./deploy-native.sh start` | 启动应用 |
| `./deploy-native.sh stop` | 停止应用 |
| `./deploy-native.sh restart` | 重启应用 |
| `./deploy-native.sh status` | 查看状态 |
| `./deploy-native.sh logs` | 查看日志 |
| `./deploy-native.sh build` | 重新编译 |

## 手动部署详细步骤

如果不想用脚本，也可以手动部署：

### 1. 安装 JDK 8

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install -y openjdk-8-jdk
```

**CentOS:**
```bash
sudo yum install -y java-1.8.0-openjdk-devel
```

验证：
```bash
java -version
```

### 2. 安装 Maven

**Ubuntu/Debian:**
```bash
sudo apt-get install -y maven
```

**CentOS:**
```bash
sudo yum install -y maven
```

验证：
```bash
mvn -version
```

### 3. 安装 MySQL

**Ubuntu/Debian:**
```bash
sudo apt-get install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

**CentOS:**
```bash
sudo yum install -y mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld
```

创建数据库：
```bash
mysql -u root -p
```
```sql
CREATE DATABASE eladmin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;
```

### 4. 安装 Redis

**Ubuntu/Debian:**
```bash
sudo apt-get install -y redis-server
sudo systemctl start redis
sudo systemctl enable redis
```

**CentOS:**
```bash
sudo yum install -y redis
sudo systemctl start redis
sudo systemctl enable redis
```

验证：
```bash
redis-cli ping
```

### 5. 创建目录

```bash
sudo mkdir -p /home/eladmin/file
sudo mkdir -p /home/eladmin/avatar
sudo mkdir -p /home/eladmin/logs
sudo chown -R $USER:$USER /home/eladmin
```

### 6. 编译项目

```bash
mvn clean package -DskipTests
```

### 7. 启动应用

```bash
# 找到生成的 jar 文件
JAR_FILE=$(ls grid-system/target/grid-system-*.jar | head -n 1)

# 启动应用（后台运行）
nohup java -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    > /home/eladmin/logs/app.log 2>&1 &
```

## 配置说明

配置文件位于 `grid-system/src/main/resources/config/`：

- **application.yml** - 主配置
- **application-dev.yml** - 开发环境配置
- **application-prod.yml** - 生产环境配置

### 通过环境变量配置（推荐）

启动时可以通过环境变量覆盖配置：

```bash
# 数据库配置
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=eladmin
export DB_USER=root
export DB_PWD=你的密码

# Redis 配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PWD=你的Redis密码

# 启动应用
nohup java -jar grid-system/target/*.jar \
    --spring.profiles.active=prod \
    > /home/eladmin/logs/app.log 2>&1 &
```

或者直接作为启动参数：

```bash
nohup java -jar grid-system/target/*.jar \
    --spring.profiles.active=prod \
    --DB_HOST=localhost \
    --DB_NAME=eladmin \
    --DB_USER=root \
    --DB_PWD=你的密码 \
    > /home/eladmin/logs/app.log 2>&1 &
```

## 访问应用

- **应用地址**: http://你的服务器IP:8000
- **Swagger 文档**: http://你的服务器IP:8000/doc.html (prod 环境默认关闭)
- **Druid 监控**: http://你的服务器IP:8000/druid (账号: admin, 密码: 123456)

## 查看日志

```bash
# 实时查看日志
tail -f /home/eladmin/logs/app.log

# 查看最近 100 行
tail -n 100 /home/eladmin/logs/app.log
```

## 使用 systemd 管理服务（可选）

创建 `/etc/systemd/system/grid.service`：

```ini
[Unit]
Description=Little Grid Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=你的用户名
WorkingDirectory=/home/你的用户名/nano-gemini
ExecStart=/usr/bin/java -jar /home/你的用户名/nano-gemini/grid-system/target/grid-system-2.7.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=append:/home/eladmin/logs/app.log
StandardError=append:/home/eladmin/logs/app.log

[Install]
WantedBy=multi-user.target
```

然后：
```bash
# 重载配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start grid

# 开机自启
sudo systemctl enable grid

# 查看状态
sudo systemctl status grid

# 查看日志
sudo journalctl -u grid -f
```

## 常见问题

### 1. 端口被占用

```bash
# 查看 8000 端口占用
sudo netstat -tlnp | grep 8000

# 或者
sudo lsof -i :8000
```

### 2. 防火墙开放端口

**Ubuntu/Debian (ufw):**
```bash
sudo ufw allow 8000/tcp
```

**CentOS (firewalld):**
```bash
sudo firewall-cmd --permanent --add-port=8000/tcp
sudo firewall-cmd --reload
```

### 3. MySQL 连接失败

确认 MySQL 是否运行：
```bash
sudo systemctl status mysql
```

确认可以本地连接：
```bash
mysql -u root -p
```

### 4. Redis 连接失败

确认 Redis 是否运行：
```bash
sudo systemctl status redis
```

测试连接：
```bash
redis-cli ping
```

### 5. 内存不足

如果编译时内存不足，调整 Maven 内存配置：

```bash
export MAVEN_OPTS="-Xmx512m -Xms256m"
mvn clean package -DskipTests
```
