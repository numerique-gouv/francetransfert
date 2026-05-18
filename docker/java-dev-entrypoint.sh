#!/bin/sh
# Dev entrypoint for any Spring Boot module of the FT repo.
#
# Expected env:
#   FT_MODULE     = francetransfert-upload-api | francetransfert-download-api | francetransfert-worker
#   FT_SERVER_PORT (optional, exposed by the service)
#
# Behaviour:
#   1. Build / install francetransfert-core once (cached in the named volume)
#   2. Watch src/main and trigger `mvn compile -o` on change (entr)
#   3. Run `mvn spring-boot:run` so Spring DevTools restarts on every successful
#      recompile (~1-2s instead of a full docker rebuild)
set -eu

# Install entr once (cheap, ~200 KB). The maven:eclipse-temurin image is
# debian-slim-based, so apt-get is available.
if ! command -v entr >/dev/null 2>&1; then
  echo "[java-dev] installing entr (one-time)"
  apt-get update -qq && apt-get install -y -qq entr
fi

cd /build

echo "[java-dev] installing francetransfert-core (cached in ~/.m2)"
(cd francetransfert-core && mvn -B -DskipTests -q install)

echo "[java-dev] starting Maven Spring Boot for $FT_MODULE"
cd "$FT_MODULE"

# Auto-recompile loop: entr re-fires mvn compile -o whenever any .java/.html
# under src/ changes. The -o flag forces offline mode (no network round trips).
(
  while sleep 1; do
    # shellcheck disable=SC2046
    find src/main -type f \( -name '*.java' -o -name '*.properties' -o -name '*.xml' \) \
      | entr -d -p sh -c 'mvn -B -o -q compile && echo "[java-dev] recompiled — Spring DevTools will restart"' \
      || true
  done
) &

exec mvn -B -P dev -DskipTests spring-boot:run \
  -Dspring-boot.run.fork=false \
  -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.poll-interval=1s -Dspring.devtools.restart.quiet-period=400ms"
