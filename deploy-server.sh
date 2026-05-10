#!/bin/bash

# ============================================================
# Little Grid 服务器部署脚本
# 使用方式：
#   1. 本地打包好 jar
#   2. 传到服务器的项目目录
#   3. 执行 ./deploy-server.sh
# ============================================================

set -e

cd "$(dirname "$0")"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 读取 .env 文件
load_env() {
    if [ -f .env ]; then
        log_info "加载配置文件: .env"
        export $(grep -v '^#' .env | grep -v '^$' | xargs)
    else
        log_warn "未找到 .env 文件，使用默认配置"
    fi
}

# 找到最新的 jar 包
find_jar() {
    JAR_FILE=$(ls -t grid-system/target/grid-system-*.jar 2>/dev/null | head -1)
    if [ -z "$JAR_FILE" ]; then
        log_error "未找到 jar 包！"
        log_error "请先在本地执行: mvn clean package -DskipTests"
        log_error "然后把 jar 传到服务器的 grid-system/target/ 目录下"
        exit 1
    fi
    echo "$JAR_FILE"
}

# 停止旧进程
stop_app() {
    if [ -f app.pid ]; then
        PID=$(cat app.pid 2>/dev/null || true)
        if [ -n "$PID" ] && ps -p "$PID" >/dev/null 2>&1; then
            log_info "停止旧进程: $PID"
            kill -9 "$PID" 2>/dev/null || true
            sleep 2
        fi
        rm -f app.pid
    fi

    # 备用：查找所有 grid-system jar 进程
    PIDS=$(pgrep -f "grid-system.*\.jar" 2>/dev/null || true)
    if [ -n "$PIDS" ]; then
        log_info "清理残留进程: $PIDS"
        kill -9 $PIDS 2>/dev/null || true
        sleep 1
    fi
}

# 启动应用
start_app() {
    JAR_FILE=$(find_jar)
    load_env

    # 环境变量
    PROFILE=${SPRING_PROFILES_ACTIVE:-prod}
    PORT=${SERVER_PORT:-8080}

    log_info "使用 JAR: $JAR_FILE"
    log_info "使用 Profile: $PROFILE"
    log_info "服务端口: $PORT"

    # 创建必要目录
    mkdir -p /home/eladmin/file
    mkdir -p /home/eladmin/avatar
    mkdir -p logs

    # 启动应用
    nohup java -jar "$JAR_FILE" \
        --spring.profiles.active="$PROFILE" \
        --server.port="$PORT" \
        --DB_HOST=127.0.0.1 \
        --DB_PORT=13306 \
        --DB_NAME="${DB_NAME:-eladmin}" \
        --DB_USER="${DB_USER:-root}" \
        --DB_PWD="${DB_PWD:-123456}" \
        --REDIS_HOST=127.0.0.1 \
        --REDIS_PORT=6379 \
        --REDIS_PWD="${REDIS_PWD:-}" \
        > logs/app.log 2>&1 &

    APP_PID=$!
    echo "$APP_PID" > app.pid

    log_info "应用已启动，PID: $APP_PID"
    log_info "日志文件: logs/app.log"
    log_info "查看日志: tail -f logs/app.log"

    # 等待启动
    sleep 5
    if ps -p "$APP_PID" >/dev/null 2>&1; then
        log_info "应用启动成功！"
        log_info "访问地址: http://$(hostname -I | awk '{print $1}'):$PORT"
    else
        log_error "应用启动失败，请查看日志: tail -f logs/app.log"
        exit 1
    fi
}

# 查看状态
status_app() {
    if [ -f app.pid ]; then
        PID=$(cat app.pid 2>/dev/null || true)
        if [ -n "$PID" ] && ps -p "$PID" >/dev/null 2>&1; then
            log_info "应用正在运行，PID: $PID"
            exit 0
        fi
    fi
    log_info "应用未运行"
}

# 查看日志
view_logs() {
    tail -f logs/app.log
}

# 主函数
main() {
    case "${1:-restart}" in
        start)
            start_app
            ;;
        stop)
            stop_app
            log_info "应用已停止"
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
        *)
            echo "使用方式: $0 [命令]"
            echo ""
            echo "命令列表:"
            echo "  start    - 启动应用"
            echo "  stop     - 停止应用"
            echo "  restart  - 重启应用（默认）"
            echo "  status   - 查看状态"
            echo "  logs     - 查看日志"
            ;;
    esac
}

main "$@"
