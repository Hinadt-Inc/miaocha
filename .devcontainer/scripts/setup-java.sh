#!/usr/bin/env bash
set -euo pipefail

echo "[postCreate] Installing Temurin JDK 17 via Adoptium APT repo..."

if ! command -v sudo >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y sudo
fi

sudo apt-get update -y
sudo apt-get install -y wget curl gnupg ca-certificates apt-transport-https tar

sudo mkdir -p /etc/apt/keyrings
if [ ! -f /etc/apt/keyrings/adoptium.gpg ]; then
  curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
fi

source /etc/os-release
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${VERSION_CODENAME} main" | sudo tee /etc/apt/sources.list.d/adoptium.list >/dev/null

sudo apt-get update -y
sudo apt-get install -y temurin-17-jdk

echo "[postCreate] Java installed. Version info:"
java -version || true
javac -version || true

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

# Install Maven (Apache binary, pinned version)
MAVEN_VERSION="3.9.9"
MAVEN_BASE_URL="https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries"
MAVEN_TGZ="apache-maven-${MAVEN_VERSION}-bin.tar.gz"

echo "[postCreate] Installing Maven ${MAVEN_VERSION} from Apache..."
curl -fsSL "${MAVEN_BASE_URL}/${MAVEN_TGZ}" -o "/tmp/${MAVEN_TGZ}"
curl -fsSL "${MAVEN_BASE_URL}/${MAVEN_TGZ}.sha512" -o "/tmp/${MAVEN_TGZ}.sha512"
cd /tmp && sha512sum -c "/tmp/${MAVEN_TGZ}.sha512"

sudo mkdir -p /opt
sudo tar -xzf "/tmp/${MAVEN_TGZ}" -C /opt
sudo ln -sfn "/opt/apache-maven-${MAVEN_VERSION}" /opt/maven
sudo ln -sfn /opt/maven/bin/mvn /usr/local/bin/mvn

# Persist MAVEN_HOME
if [ -d "/opt/maven" ]; then
  echo "export MAVEN_HOME=/opt/maven" | sudo tee /etc/profile.d/maven_home.sh >/dev/null
  echo 'export PATH="${MAVEN_HOME}/bin:${PATH}"' | sudo tee -a /etc/profile.d/maven_home.sh >/dev/null
  sudo chmod 644 /etc/profile.d/maven_home.sh
fi

echo "[postCreate] Maven installed. Version info:"
mvn -v || true
