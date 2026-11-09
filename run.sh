#!/usr/bin/env bash
# Build and run the New2Canada server on http://localhost:8080.
#
# Works on macOS, Linux and Windows (Git Bash / WSL). Needs JDK 17+ and Maven.
#
# Usage:
#   ./run.sh          build if needed, then run
#   ./run.sh build    force a clean rebuild (runs the tests too)
#   ./run.sh test     run the unit tests only
#   ./run.sh clean    remove build output
#
# Environment (all optional):
#   JAVA_HOME           JDK 17+ to use (otherwise `java` on PATH is used)
#   NEW2CANADA_CONTACT  contact shown to websites in our User-Agent (email or URL)
#   NEW2CANADA_CRAWL    set to "off" to use cached data only (no live scraping)
set -euo pipefail
cd "$(dirname "$0")"

fail() { echo "error: $*" >&2; exit 1; }

# ---- find a JDK 17+ ----------------------------------------------------------
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="$(command -v java || true)"
fi
[[ -n "$JAVA" ]] || fail "Java not found. Install JDK 17+ or set JAVA_HOME."

major="$("$JAVA" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
[[ "${major:-0}" -ge 17 ]] || fail "JDK 17+ required (found ${major:-unknown}). Set JAVA_HOME to a newer JDK."

command -v mvn >/dev/null 2>&1 || fail "Maven not found. Install Maven 3.8+ (https://maven.apache.org)."

# ---- actions -----------------------------------------------------------------
case "${1:-}" in
  clean) mvn -q clean ;;
  test)  mvn -B test ;;
  build) mvn -B clean package ;;
  *)
    if [[ ! -f target/new2canada.jar ]]; then
      echo "First run: building with Maven (this can take a minute)…"
      mvn -q -B package -DskipTests
    fi
    echo "Starting New2Canada on http://localhost:8080  (Ctrl+C to stop)"
    exec "$JAVA" -jar target/new2canada.jar
    ;;
esac
