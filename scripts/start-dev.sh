#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

cleanup() {
    info "Shutting down development services..."
    docker compose -f "$PROJECT_ROOT/docker-compose.dev.yml" down
    if [ -n "${BACKEND_PID:-}" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    if [ -n "${FRONTEND_PID:-}" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
    ok "All services stopped."
}

trap cleanup EXIT INT TERM

check_dependencies() {
    local missing=()

    if ! command -v docker &>/dev/null; then
        missing+=("docker")
    fi

    if ! command -v java &>/dev/null; then
        missing+=("java (JDK 21+)")
    fi

    if ! command -v node &>/dev/null; then
        missing+=("node (18+)")
    fi

    if [ ${#missing[@]} -gt 0 ]; then
        error "Missing required dependencies: ${missing[*]}"
        exit 1
    fi
}

load_env() {
    if [ -f "$PROJECT_ROOT/.env" ]; then
        info "Loading environment from .env"
        set -a
        source "$PROJECT_ROOT/.env"
        set +a
    elif [ -f "$PROJECT_ROOT/.env.example" ]; then
        warn "No .env file found. Using .env.example defaults."
        warn "Copy .env.example to .env and customize for your environment."
        set -a
        source "$PROJECT_ROOT/.env.example"
        set +a
    fi
}

start_infrastructure() {
    info "Starting PostgreSQL and Redis..."
    docker compose -f "$PROJECT_ROOT/docker-compose.dev.yml" up -d

    info "Waiting for PostgreSQL to be ready..."
    local retries=30
    while [ $retries -gt 0 ]; do
        if docker compose -f "$PROJECT_ROOT/docker-compose.dev.yml" exec -T postgres pg_isready -U umg &>/dev/null; then
            ok "PostgreSQL is ready."
            break
        fi
        retries=$((retries - 1))
        sleep 1
    done

    if [ $retries -eq 0 ]; then
        error "PostgreSQL failed to start within 30 seconds."
        exit 1
    fi

    info "Waiting for Redis to be ready..."
    retries=30
    while [ $retries -gt 0 ]; do
        if docker compose -f "$PROJECT_ROOT/docker-compose.dev.yml" exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
            ok "Redis is ready."
            break
        fi
        retries=$((retries - 1))
        sleep 1
    done

    if [ $retries -eq 0 ]; then
        error "Redis failed to start within 30 seconds."
        exit 1
    fi
}

start_backend() {
    info "Starting Spring Boot backend..."
    cd "$PROJECT_ROOT/backend"

    if [ -f "./gradlew" ]; then
        ./gradlew bootRun --args='--spring.profiles.active=local' &
    else
        gradle bootRun --args='--spring.profiles.active=local' &
    fi
    BACKEND_PID=$!

    info "Backend starting (PID: $BACKEND_PID). Waiting for health check..."
    local retries=60
    while [ $retries -gt 0 ]; do
        if curl -sf http://localhost:8080/actuator/health &>/dev/null; then
            ok "Backend is ready at http://localhost:8080"
            break
        fi
        if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
            error "Backend process exited unexpectedly."
            exit 1
        fi
        retries=$((retries - 1))
        sleep 2
    done

    if [ $retries -eq 0 ]; then
        warn "Backend health check timed out. It may still be starting up."
    fi
}

start_frontend() {
    info "Starting React frontend..."
    cd "$PROJECT_ROOT/frontend"

    if [ ! -d "node_modules" ]; then
        info "Installing frontend dependencies..."
        npm install
    fi

    npm run dev &
    FRONTEND_PID=$!
    ok "Frontend starting (PID: $FRONTEND_PID) at http://localhost:5173"
}

main() {
    echo ""
    echo "========================================="
    echo "  UMG Development Environment Launcher"
    echo "========================================="
    echo ""

    check_dependencies
    load_env
    start_infrastructure
    start_backend
    start_frontend

    echo ""
    ok "Development environment is running!"
    echo ""
    echo "  PostgreSQL : localhost:5432"
    echo "  Redis      : localhost:6379"
    echo "  Backend    : http://localhost:8080"
    echo "  Frontend   : http://localhost:5173"
    echo ""
    info "Press Ctrl+C to stop all services."
    echo ""

    wait
}

main "$@"
