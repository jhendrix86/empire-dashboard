#!/usr/bin/env bash
set -e

echo "=== Empire Dashboard Build ==="
echo "Target: $1"

case "$1" in
  apk)
    echo "Building Android APK..."
    ./gradlew :composeApp:assembleRelease
    echo "APK: composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk"
    ;;
  exe)
    echo "Building Windows EXE..."
    ./gradlew :composeApp:packageExe
    echo "EXE: composeApp/build/compose/binaries/main/exe/"
    ;;
  deb)
    echo "Building Linux .deb..."
    ./gradlew :composeApp:packageDeb
    echo "DEB: composeApp/build/compose/binaries/main/deb/"
    ;;
  all)
    ./gradlew :composeApp:assembleRelease :composeApp:packageReleaseDistributionForCurrentOS
    ;;
  run)
    echo "Running desktop app..."
    ./gradlew :composeApp:run
    ;;
  *)
    echo "Usage: ./build.sh [apk|exe|deb|all|run]"
    exit 1
    ;;
esac
