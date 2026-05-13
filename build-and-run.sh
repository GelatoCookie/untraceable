#!/bin/bash

################################################################################
# Automated Build, Clean, and Run Script for G2V2 RFID Project
# 
# Usage: ./build-and-run.sh [command]
#
# Commands:
#   clean       - Clean build artifacts
#   build       - Build the project (debug or release)
#   run         - Run on connected device/emulator
#   all         - Clean, build, and run (default)
#   test        - Run unit tests
#   release     - Build release APK
#   help        - Show this help message
#
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_WRAPPER="$PROJECT_ROOT/gradlew"

# Logging functions
log_info() {
    echo -e "${BLUE}ℹ ${1}${NC}"
}

log_success() {
    echo -e "${GREEN}✓ ${1}${NC}"
}

log_error() {
    echo -e "${RED}✗ ${1}${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠ ${1}${NC}"
}

# Check if gradlew exists
check_gradle() {
    if [ ! -f "$GRADLE_WRAPPER" ]; then
        log_error "gradlew not found in $PROJECT_ROOT"
        exit 1
    fi
}

# Clean build
clean_build() {
    log_info "Cleaning build artifacts..."
    "$GRADLE_WRAPPER" -q clean
    log_success "Clean completed"
}

# Build debug
build_debug() {
    log_info "Building debug APK..."
    "$GRADLE_WRAPPER" assembleDebug --info
    log_success "Debug build completed"
}

# Build release
build_release() {
    log_info "Building release APK..."
    "$GRADLE_WRAPPER" assembleRelease --info
    log_success "Release build completed"
    log_info "Release APK: app/build/outputs/apk/release/app-release.apk"
}

# Run tests
run_tests() {
    log_info "Running unit tests..."
    "$GRADLE_WRAPPER" test
    log_success "Tests completed"
}

# Returns success if at least one Android device/emulator is connected.
has_connected_device() {
    if ! command -v adb >/dev/null 2>&1; then
        return 1
    fi

    local connected_count
    connected_count=$(adb devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')
    [ "$connected_count" -gt 0 ]
}

# Install and run on device/emulator
run_on_device() {
    if ! has_connected_device; then
        log_error "No connected Android device/emulator found."
        log_info "Connect a device or start an emulator, then run: ./build-and-run.sh run"
        return 1
    fi

    log_info "Installing debug APK on device..."
    "$GRADLE_WRAPPER" installDebug
    log_success "Installation completed"
    
    # Get app package name from manifest or gradle
    PACKAGE_NAME="com.zebra.rfid.demo.sdksample"
    ACTIVITY_NAME="$PACKAGE_NAME.MainActivity"
    
    log_info "Launching app..."
    adb shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"
    log_success "App launched"
}

# Show help
show_help() {
    sed -n '1,/^################################/p' "$0" | sed '$ d'
}

# Main execution
main() {
    check_gradle
    
    COMMAND="${1:-all}"
    
    case "$COMMAND" in
        clean)
            clean_build
            ;;
        build)
            build_debug
            ;;
        run)
            run_on_device
            ;;
        all)
            clean_build
            build_debug
            if has_connected_device; then
                run_on_device
                log_success "Clean, build, and run completed!"
            else
                log_warning "No connected device detected. Skipping install/launch step."
                log_success "Clean and build completed!"
            fi
            ;;
        test)
            run_tests
            ;;
        release)
            clean_build
            build_release
            ;;
        help|-h|--help)
            show_help
            ;;
        *)
            log_error "Unknown command: $COMMAND"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
