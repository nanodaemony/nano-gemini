# Docker 部署指南

使用 Docker Compose 一键部署 Little Grid 项目。

---

## 两套配置说明

| 环境 | 配置模板 | 说明 |
|------|---------|------|
| 云服务器 Docker 部署 | `.env.example` | MySQL 用 Docker 服务名 `mysql` |
| 本地电脑开发 | `.env.local.example` | MySQL 连云服务器IP `123.45.67.89:13306` |

---

## 端口映射说明

| 云服务器端口 | 容器端口 | 服务 | 说明 |
|-------------|---------|------|------|
| **8080** | 8000 | Spring Boot 应用 | 对外提供 API 服务 |
| **13306** | 3306 | MySQL | 可从本地电脑连接 |
| 127.0.0.1:6379 | 6379 | Redis | 仅本地访问，不对外暴露 |

---

## 一、云服务器部署步骤

### 1. 配置 .env 文件

```bash
# 复制云服务器配置模板
cp .env.example .env

# 编辑配置（主要改 MySQL 密码）
vim .env
```

修改这一行：
```bash
DB_PWD=你的密码    # 设置一个强密码
```

**注意**：云服务器的 `.env` 里 `DB_HOST` 可以是任意值，因为 `docker-compose.yml` 里会强制覆盖为 `mysql`（Docker 网络服务名）。

### 2. 服务器开放防火墙

需要开放这两个端口：
- **8080** - 应用访问
- **13306** - MySQL 远程连接（可选，如果你本地要连的话）

**Ubuntu/Debian (ufw):**
```bash
sudo ufw allow 8080/tcp
sudo ufw allow 13306/tcp
```

**CentOS (firewalld):**
```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=13306/tcp
sudo firewall-cmd --reload
```

**云服务器安全组：**
记得在阿里云/腾讯云/华为云的安全组规则里也添加这两个端口。

### 3. 一键部署

```bash
# 拉取代码 + 构建 + 启动
./deploy.sh
```

或者手动执行：
```bash
# 拉取代码
git pull origin master

# 停止旧容器（如果有）
docker-compose down

# 构建并启动
docker-compose up -d --build
```

---

## 常用命令

| 命令 | 说明 |
|------|------|
| `docker-compose up -d` | 启动所有服务 |
| `docker-compose down` | 停止所有服务 |
| `docker-compose restart` | 重启所有服务 |
| `docker-compose logs -f` | 查看所有日志 |
| `docker-compose logs -f app` | 只看应用日志 |
| `docker-compose logs -f mysql` | 只看 MySQL 日志 |
| `docker-compose ps` | 查看服务状态 |
| `docker-compose build` | 重新构建镜像 |

---

## 本地电脑连接云服务器 MySQL

部署成功后，你可以在本地电脑连接云数据库：

```bash
mysql -h 你的云服务器IP -P 13306 -u root -p
```

或者用数据库管理工具（Navicat / DBeaver / DataGrip 等）：
- Host: `你的云服务器IP`
- Port: `13306`
- Username: `root`
- Password: `.env` 里你设置的 `DB_PWD`

---

## 访问应用

- **API 地址**: `http://你的云服务器IP:8080`
- **API 文档**: `http://你的云服务器IP:8080/doc.html` (如果开启了 swagger)
- **Druid 监控**: `http://你的云服务器IP:8080/druid` (账号 admin / 密码 123456)

---

## 数据持久化

Docker volumes 会自动创建，数据不会丢：
- `mysql-data` - MySQL 数据
- `redis-data` - Redis 数据

即使 `docker-compose down` 再 `up`，数据也还在。

---

## 安全建议

1. **MySQL 端口**：如果只是在 Docker 内用，可以把 `13306` 的映射删掉，只留 Docker 网络访问
2. **密码强度**：`.env` 里的 `DB_PWD` 要设置强密码
3. **限制 IP**：如果云服务商安全组支持，建议把 13306 限制只允许你本地 IP 访问

---

## 故障排查

### 应用启动失败
```bash
# 查看应用日志
docker-compose logs -f app
```

### MySQL 连接失败
```bash
# 查看 MySQL 日志
docker-compose logs -f mysql

# 确认 MySQL 容器健康
docker-compose ps
```

### 重新部署
```bash
# 完整重新部署
docker-compose down
docker-compose up -d --build
```

---

## 二、本地电脑开发配置

### 1. 配置本地 .env

```bash
# 复制本地开发配置模板
cp .env.local.example .env

# 编辑配置
vim .env
```

修改这些：
```bash
DB_HOST=123.45.67.89    # 换成你的云服务器IP
DB_PORT=13306
DB_PWD=你的密码             # 和云服务器 .env 里的 DB_PWD 一致
```

### 2. 本地启动项目

用 IDEA 或命令行启动项目，会自动读取 `.env` 里的配置：
- 连接云服务器的 MySQL (`123.45.67.89:13306`
- 使用本地的 Redis (`127.0.0.1:6379`)

### 3. IDEA 里读取 .env（可选）

如果 IDEA 安装了 **EnvFile** 插件：
1. 打开启动配置
2. 勾选 `Enable EnvFile`
3. 添加项目下的 `.env` 文件

或者手动添加环境变量

---

## 配置对比总结

| 环境 | DB_HOST | DB_PORT | 说明 |
|------|---------|---------|------|
| 云服务器 Docker | `mysql` (固定) | 3306 | Docker 网络内访问 |
| 本地电脑开发 | `你的云服务器IP` | 13306 | 通过云服务器端口访问 |
| 两者共用 | `DB_NAME`, `DB_USER`, `DB_PWD` | - | 必须一致！ |
