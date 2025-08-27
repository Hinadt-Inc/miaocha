#!/usr/bin/env bash
set -euo pipefail

echo "[postCreate] Verifying Docker availability..."
docker version > /dev/null

images=(
  "mysql:8.0"
  "apache/doris:2.1.9-all"
  "wataken44/ubuntu-latest-sshd:latest"
)

echo "[postCreate] Pre-pulling required Docker images..."
for img in "${images[@]}"; do
  echo "Pulling ${img} ..."
  docker pull "${img}"
done

echo "[postCreate] Toolchain versions:"
echo -n "node:  "; node -v || true
echo -n "yarn:  "; yarn -v || true
echo -n "pnpm:  "; pnpm -v || true
echo "java:"
java -version || true

echo "[postCreate] Done."

