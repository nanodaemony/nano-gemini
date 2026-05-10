# 部署说明

## 本地开发（Windows）

```bash
# 正常开发调试，用 Maven 或 IDEA 运行
mvn clean compile
mvn spring-boot:run -pl grid-system
```

## 推送到 GitHub

```bash
git add .
git commit -m "update"
git push
```

## 云服务器部署

### 1. 准备服务器环境

安装 Docker 和 Docker Compose：

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 重新登录后生效
```

### 2. 克隆项目

```bash
cd /opt
git clone <你的GitHub仓库地址> nano-gemini
cd nano-gemini
```

### 3. 配置环境变量

```bash
cp .env.example .env
nano .env  # 或 vim .env，修改密码等配置
```

### 4. 执行部署

```bash
chmod +x deploy.sh
./deploy.sh
```

### 5. 查看日志

```bash
docker-compose logs -f app
```

### 6. 后续更新

```bash
# 服务器上只需运行
./deploy.sh
```

## 常用命令

```bash
# 查看服务状态
docker-compose ps

# 停止服务
docker-compose down

# 手动构建
docker-compose build

# 只重启应用
docker-compose restart app
```
