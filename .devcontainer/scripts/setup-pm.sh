#!/usr/bin/env bash
set -euo pipefail

echo "[postCreate] Enabling Corepack and installing package managers..."

if command -v corepack >/dev/null 2>&1; then
  corepack enable || true
  echo "[postCreate] Installing Yarn 1.22.22 via Corepack..."
  corepack prepare yarn@1.22.22 --activate
  echo "[postCreate] Installing pnpm (latest) via Corepack..."
  corepack prepare pnpm@latest --activate
else
  echo "[postCreate] Corepack not found, installing Yarn via npm..."
  npm i -g yarn@1.22.22 pnpm
fi

echo -n "node:  "; node -v || true
echo -n "yarn:  "; yarn -v || true
echo -n "pnpm:  "; pnpm -v || true

echo "[postCreate] Package managers ready."
