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

BUILD_BACKEND=true
BUILD_FRONTEND=true
BUILD_BRIDGE=true
BUILD_DOCKER=false
SKIP_TESTS=false

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --backend-only    Build only the backend"
    echo "  --frontend-only   Build only the frontend"
    echo "  --bridge-only     Build only the bridge CLI"
    echo "  --docker          Build Docker images"
    echo "  --skip-tests      Skip test execution"
    echo "  --help            Show this help message"
    echo ""
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --backend-only)
                BUILD_FRONTEND=false
                BUILD_BRIDGE=false
                shift ;;
            --frontend-only)
                BUILD_BACKEND=false
                BUILD_BRIDGE=false
                shift ;;
            --bridge-only)
                BUILD_BACKEND=false
                BUILD_FRONTEND=false
                shift ;;
            --docker)
                BUILD_DOCKER=true
                shift ;;
            --skip-tests)
                SKIP_TESTS=true
                shift ;;
            --help)
                usage
                exit 0 ;;
            *)
                error "Unknown option: $1"
                usage
                exit 1 ;;
        esac
    done
}

build_backend() {
    info "Building backend..."
    cd "$PROJECT_ROOT/backend"

    local gradle_cmd="./gradlew"
    if [ ! -f "$gradle_cmd" ]; then
        gradle_cmd="gradle"
    fi

    if [ "$SKIP_TESTS" = true ]; then
        $gradle_cmd clean build -x test
    else
        $gradle_cmd clean build
    fi

    ok "Backend build complete. JAR: $(ls -1 build/libs/*.jar 2>/dev/null | head -1)"
}

build_frontend() {
    info "Building frontend..."
    cd "$PROJECT_ROOT/frontend"

    if [ ! -d "node_modules" ]; then
        info "Installing frontend dependencies..."
        npm ci
    fi

    npm run build

    ok "Frontend build complete. Output: $PROJECT_ROOT/frontend/dist/"
}

build_bridge() {
    info "Building bridge CLI..."
    cd "$PROJECT_ROOT/bridge"

    if [ ! -d "node_modules" ]; then
        info "Installing bridge dependencies..."
        npm ci
    fi

    npm run clean
    npm run build

    ok "Bridge build complete. Output: $PROJECT_ROOT/bridge/dist/"
}

build_docker_images() {
    info "Building Docker images..."
    cd "$PROJECT_ROOT"

    local version
    version=$(git describe --tags --always 2>/dev/null || echo "dev")

    info "Building backend image (umg-backend:$version)..."
    docker build -t "umg-backend:$version" -t "umg-backend:latest" ./backend

    info "Building frontend image (umg-frontend:$version)..."
    docker build -t "umg-frontend:$version" -t "umg-frontend:latest" ./frontend

    ok "Docker images built successfully."
    echo ""
    docker images | grep umg
}

main() {
    echo ""
    echo "========================================="
    echo "  UMG Production Build"
    echo "========================================="
    echo ""

    parse_args "$@"

    local start_time
    start_time=$(date +%s)

    if [ "$BUILD_BACKEND" = true ]; then
        build_backend
        echo ""
    fi

    if [ "$BUILD_FRONTEND" = true ]; then
        build_frontend
        echo ""
    fi

    if [ "$BUILD_BRIDGE" = true ]; then
        build_bridge
        echo ""
    fi

    if [ "$BUILD_DOCKER" = true ]; then
        build_docker_images
        echo ""
    fi

    local end_time elapsed
    end_time=$(date +%s)
    elapsed=$((end_time - start_time))

    ok "Build completed in ${elapsed}s."
}

main "$@"
