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

DRY_RUN=false
SKIP_TESTS=false
PUSH=false

usage() {
    echo "Usage: $0 <version> [OPTIONS]"
    echo ""
    echo "Arguments:"
    echo "  version           Semantic version (e.g., 1.0.0, 1.2.3-rc1)"
    echo ""
    echo "Options:"
    echo "  --dry-run         Show what would be done without making changes"
    echo "  --skip-tests      Skip test execution"
    echo "  --push            Push tags and commits to remote"
    echo "  --help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 1.0.0"
    echo "  $0 1.1.0 --push"
    echo "  $0 2.0.0-rc1 --dry-run"
    echo ""
}

validate_version() {
    local version="$1"
    if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$ ]]; then
        error "Invalid version format: $version"
        error "Expected semantic version (e.g., 1.0.0 or 1.0.0-rc1)"
        exit 1
    fi
}

check_prerequisites() {
    info "Checking prerequisites..."

    if ! command -v git &>/dev/null; then
        error "git is not installed."
        exit 1
    fi

    cd "$PROJECT_ROOT"

    if [ -n "$(git status --porcelain)" ]; then
        error "Working directory is not clean. Commit or stash your changes first."
        git status --short
        exit 1
    fi

    local current_branch
    current_branch=$(git rev-parse --abbrev-ref HEAD)
    if [[ "$current_branch" != "main" && "$current_branch" != "master" ]]; then
        warn "Current branch is '$current_branch'. Releases are typically made from main/master."
        read -rp "Continue anyway? [y/N] " confirm
        if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
            info "Release cancelled."
            exit 0
        fi
    fi

    if git tag | grep -q "^v${VERSION}$"; then
        error "Tag v${VERSION} already exists."
        exit 1
    fi

    ok "Prerequisites check passed."
}

update_backend_version() {
    info "Updating backend version to $VERSION..."
    local build_gradle="$PROJECT_ROOT/backend/build.gradle"

    if [ -f "$build_gradle" ]; then
        sed -i "s/version = '.*'/version = '$VERSION'/" "$build_gradle"
        ok "Updated build.gradle"
    fi
}

update_bridge_version() {
    info "Updating bridge version to $VERSION..."
    local package_json="$PROJECT_ROOT/bridge/package.json"

    if [ -f "$package_json" ]; then
        sed -i "s/\"version\": \".*\"/\"version\": \"$VERSION\"/" "$package_json"
        ok "Updated bridge/package.json"
    fi
}

update_frontend_version() {
    info "Updating frontend version to $VERSION..."
    local package_json="$PROJECT_ROOT/frontend/package.json"

    if [ -f "$package_json" ]; then
        sed -i "s/\"version\": \".*\"/\"version\": \"$VERSION\"/" "$package_json"
        ok "Updated frontend/package.json"
    fi
}

run_build() {
    info "Running production build..."

    local build_opts=()
    if [ "$SKIP_TESTS" = true ]; then
        build_opts+=("--skip-tests")
    fi

    bash "$SCRIPT_DIR/build.sh" "${build_opts[@]}"
}

create_tag() {
    cd "$PROJECT_ROOT"

    info "Creating release commit..."
    git add -A
    git commit -m "release: v${VERSION}"

    info "Creating tag v${VERSION}..."
    git tag -a "v${VERSION}" -m "Release v${VERSION}"

    ok "Tag v${VERSION} created."
}

push_release() {
    if [ "$PUSH" = true ]; then
        info "Pushing release to remote..."
        cd "$PROJECT_ROOT"
        git push origin HEAD
        git push origin "v${VERSION}"
        ok "Release pushed to remote."
    else
        echo ""
        info "To push the release, run:"
        echo "  git push origin HEAD"
        echo "  git push origin v${VERSION}"
    fi
}

main() {
    echo ""
    echo "========================================="
    echo "  UMG Release"
    echo "========================================="
    echo ""

    if [ $# -lt 1 ] || [ "$1" = "--help" ]; then
        usage
        exit 0
    fi

    VERSION="$1"
    shift

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --dry-run)
                DRY_RUN=true
                shift ;;
            --skip-tests)
                SKIP_TESTS=true
                shift ;;
            --push)
                PUSH=true
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

    validate_version "$VERSION"

    if [ "$DRY_RUN" = true ]; then
        warn "DRY RUN mode - no changes will be made."
        echo ""
        echo "Would perform the following:"
        echo "  1. Verify clean working directory on main/master"
        echo "  2. Update version to $VERSION in backend, frontend, bridge"
        echo "  3. Run production build"
        echo "  4. Commit changes with message: release: v${VERSION}"
        echo "  5. Create annotated tag: v${VERSION}"
        if [ "$PUSH" = true ]; then
            echo "  6. Push commit and tag to remote"
        fi
        echo ""
        ok "Dry run complete."
        exit 0
    fi

    check_prerequisites

    info "Releasing version $VERSION..."
    echo ""

    update_backend_version
    update_bridge_version
    update_frontend_version

    run_build

    create_tag
    push_release

    echo ""
    ok "Release v${VERSION} complete!"
    echo ""
}

main "$@"
