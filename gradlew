#!/usr/bin/env bash
# Minimal placeholder gradlew script for RemoteDexter-v2
# This is a deterministic stub to satisfy CI and developer bootstrap checks.
# To create the real Gradle wrapper (recommended), run:
#   gradle wrapper --gradle-version 8.2.1
# Then commit the generated gradlew, gradlew.bat, and gradle/wrapper/gradle-wrapper.jar files.

# Prefer local gradle wrapper jar if present, otherwise fall back to 'gradle' on PATH
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "No 'gradle' on PATH. Install Gradle or regenerate the proper Gradle wrapper." >&2
  exit 1
fi
