#!/bin/bash

# Little Grid 原生部署脚本
# 适用于 Ubuntu/Debian/CentOS

set -e

cd "$(dirname "$0")"

# 读取 .env 文件
load_env() {
    if [ -f .env ]; then
        log_info "加载配置文件: .env"
        # 读取 .env，忽略注释行和空行，export 环境变量
        export $(grep -v '^#' .env | grep -v '^$' | xargs)
    else
        log_warn "未找到 .env 文件，使用默认配置"
    fi
}

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检测包管理器
detect_package_manager() {
    if [ -f /etc/debian_version ]; then
        echo "apt"
    elif [ -f /etc/redhat-release ]; then
        echo "yum"
    else
        echo "unknown"
    fi
}

# 安装 Java 8
install_java() {
    if command -v java &> /dev/null; then
        JAVA_VER=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1-2)
        if [[ "$JAVA_VER" == "1.8" || "$JAVA_VER" == "8."* ]]; then
            log_info "Java 8 已安装: $(java -version 2>&1 | head -n 1)"
            return
        fi
    fi

    log_info "正在安装 Java 8..."
    PM=$(detect_package_manager)

    if [ "$PM" = "apt" ]; then
        sudo apt-get update
        sudo apt-get install -y openjdk-8-jdk
    elif [ "$PM" = "yum" ]; then
        sudo yum install -y java-1.8.0-openjdk-devel
    else
        log_error "不支持的包管理器，请手动安装 Java 8"
        exit 1
    fi
}

# 安装 Maven
install_maven() {
    if command -v mvn &> /dev/null; then
        log_info "Maven 已安装: $(mvn -version | head -n 1)"
        return
    fi

    log_info "正在安装 Maven..."
    PM=$(detect_package_manager)

    if [ "$PM" = "apt" ]; then
        sudo apt-get install -y maven
    elif [ "$PM" = "yum" ]; then
        sudo yum install -y maven
    else
        log_warn "自动安装 Maven 失败，请手动安装"
    fi
}

# 安装 MySQL
install_mysql() {
    if command -v mysql &> /dev/null; then
        log_info "MySQL 已安装"
        return
    fi

    log_info "正在安装 MySQL..."
    PM=$(detect_package_manager)

    if [ "$PM" = "apt" ]; then
        sudo apt-get update
        sudo apt-get install -y mysql-server
        sudo systemctl start mysql
        sudo systemctl enable mysql
    elif [ "$PM" = "yum" ]; then
        sudo yum install -y mysql-server
        sudo systemctl start mysqld
        sudo systemctl enable mysqld
    else
        log_error "不支持的包管理器，请手动安装 MySQL"
        exit 1
    fi

    log_warn "MySQL 安装完成，请手动设置 root 密码并创建数据库"
}

# 安装 Redis
install_redis() {
    if command -v redis-cli &> /dev/null; then
        log_info "Redis 已安装"
        return
    fi

    log_info "正在安装 Redis..."
    PM=$(detect_package_manager)

    if [ "$PM" = "apt" ]; then
        sudo apt-get install -y redis-server
        sudo systemctl start redis
        sudo systemctl enable redis
    elif [ "$PM" = "yum" ]; then
        sudo yum install -y redis
        sudo systemctl start redis
        sudo systemctl enable redis
    else
        log_error "不支持的包管理器，请手动安装 Redis"
        exit 1
    fi
}

# 创建必要的目录
create_dirs() {
    log_info "创建文件存储目录..."
    sudo mkdir -p /home/eladmin/file
    sudo mkdir -p /home/eladmin/avatar
    sudo mkdir -p /home/eladmin/logs
    sudo chown -R $USER:$USER /home/eladmin
}

# 编译项目
build_project() {
    log_info "编译项目..."
    mvn clean package -DskipTests
}

# 初始化数据库
init_database() {
    log_warn "请确保已创建数据库 'eladmin'"
    log_warn "如果未创建，请执行："
    log_warn "  mysql -u root -p"
    log_warn "  CREATE DATABASE eladmin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
}

# 启动应用
start_app() {
    load_env
    JAR_FILE=$(ls grid-system/target/grid-system-*.jar | head -n 1)

    if [ -z "$JAR_FILE" ]; then
        log_error "未找到 jar 文件，请先编译项目"
        exit 1
    fi

    log_info "启动应用: $JAR_FILE"

    # 检查是否有正在运行的进程
    PIDS=$(pgrep -f "grid-system" || true)
    if [ -n "$PIDS" ]; then
        log_warn "发现正在运行的进程，正在停止..."
        kill -9 $PIDS || true
        sleep 2
    fi

    # 确定 Spring Profile，默认 prod
    PROFILE=${SPRING_PROFILES_ACTIVE:-prod}
    # 确定端口，默认 8000
    PORT=${SERVER_PORT:-8000}

    log_info "使用配置文件: $PROFILE"
    log_info "服务端口: $PORT"

    # 启动应用（通过环境变量传递配置）
    nohup java -jar "$JAR_FILE" \
        --spring.profiles.active="$PROFILE" \
        --server.port="$PORT" \
        > /home/eladmin/logs/app.log 2>&1 &

    APP_PID=$!
    echo "$APP_PID" > app.pid

    log_info "应用已启动，PID: $APP_PID"
    log_info "日志文件: /home/eladmin/logs/app.log"
    log_info "查看日志: tail -f /home/eladmin/logs/app.log"
    log_info "停止应用: ./deploy-native.sh stop"

    # 等待启动
    sleep 5
    if ps -p $APP_PID > /dev/null; then
        log_info "应用启动成功！"
        log_info "访问地址: http://$(hostname -I | awk '{print $1}'):8000"
    else
        log_error "应用启动失败，请查看日志"
    fi
}

# 停止应用
stop_app() {
    if [ -f app.pid ]; then
        PID=$(cat app.pid 2>/dev/null || true)
        if [ -n "$PID" ]; then
            kill -9 $PID 2>/dev/null || true
        fi
        rm -f app.pid
    fi

    # 备用：查找所有 grid-system 进程
    PIDS=$(pgrep -f "grid-system" || true)
    if [ -n "$PIDS" ]; then
        kill -9 $PIDS 2>/dev/null || true
    fi

    log_info "应用已停止"
}

# 查看状态
status_app() {
    if [ -f app.pid ]; then
        PID=$(cat app.pid 2>/dev/null || true)
        if [ -n "$PID" ] && ps -p $PID > /dev/null; then
            log_info "应用正在运行，PID: $PID"
            exit 0
        fi
    fi
    log_info "应用未运行"
}

# 查看日志
view_logs() {
    tail -f /home/eladmin/logs/app.log
}

# 主函数
main() {
    case "${1:-all}" in
        install)
            log_info "开始安装依赖..."
            install_java
            install_maven
            install_mysql
            install_redis
            create_dirs
            log_info "依赖安装完成！"
            ;;
        build)
            build_project
            ;;
        init-db)
            init_database
            ;;
        start)
            start_app
            ;;
        stop)
            stop_app
            ;;
        restart)
            stop_app
            sleep 1
            start_app
            ;;
        status)
            status_app
            ;;
        logs)
            view_logs
            ;;
        all)
            log_info "开始完整部署..."
            install_java
            install_maven
            install_mysql
            install_redis
            create_dirs
            init_database
            build_project
            start_app
            log_info "部署完成！"
            ;;
        *)
            echo "用法: $0 [command]"
            echo ""
            echo "命令列表:"
            echo "  install    - 安装依赖 (Java, Maven, MySQL, Redis)"
            echo "  build      - 编译项目"
            echo "  init-db    - 数据库初始化提示"
            echo "  start      - 启动应用"
            echo "  stop       - 停止应用"
            echo "  restart    - 重启应用"
            echo "  status     - 查看应用状态"
            echo "  logs       - 查看应用日志"
            echo "  all        - 完整部署 (默认)"
            ;;
    esac
}

main "$@"
