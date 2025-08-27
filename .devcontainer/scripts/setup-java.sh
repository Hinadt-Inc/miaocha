#!/usr/bin/env bash
set -euo pipefail

echo "[postCreate] Installing Temurin JDK 17 via Adoptium APT repo..."

if ! command -v sudo >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y sudo
fi

sudo apt-get update -y
sudo apt-get install -y wget curl gnupg ca-certificates apt-transport-https

sudo mkdir -p /etc/apt/keyrings
if [ ! -f /etc/apt/keyrings/adoptium.gpg ]; then
  curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
fi

source /etc/os-release
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${VERSION_CODENAME} main" | sudo tee /etc/apt/sources.list.d/adoptium.list >/dev/null

sudo apt-get update -y
sudo apt-get install -y temurin-17-jdk maven

echo "[postCreate] Java and Maven installed. Version info:"
java -version || true
javac -version || true
mvn -v || true

# Derive JAVA_HOME and persist for all users
JAVA_BIN_PATH="$(readlink -f "$(command -v java)")"
JAVA_HOME_DIR="$(dirname "$(dirname "${JAVA_BIN_PATH}")")"
if [ -n "${JAVA_HOME_DIR}" ] && [ -d "${JAVA_HOME_DIR}" ]; then
  echo "[postCreate] Setting JAVA_HOME=${JAVA_HOME_DIR}"
  echo "export JAVA_HOME=${JAVA_HOME_DIR}" | sudo tee /etc/profile.d/java_home.sh >/dev/null
  echo 'export PATH="${JAVA_HOME}/bin:${PATH}"' | sudo tee -a /etc/profile.d/java_home.sh >/dev/null
  sudo chmod 644 /etc/profile.d/java_home.sh
fi

echo "[postCreate] Temurin JDK 17 setup complete."
