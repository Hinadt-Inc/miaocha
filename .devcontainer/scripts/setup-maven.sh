#!/usr/bin/env bash
set -euo pipefail

echo "[postCreate] Installing Maven 3.9.9 from Apache..."

if ! command -v sudo >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y sudo
fi

sudo apt-get update -y && sudo apt-get install -y ca-certificates curl tar coreutils

MAVEN_VERSION="3.9.9"
MAVEN_TGZ="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
PRIMARY="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries"
MIRROR1="https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries"
ARCHIVE="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries"

download_with_fallback() {
  local file="$1"
  local url=""
  for base in "$PRIMARY" "$MIRROR1" "$ARCHIVE"; do
    url="${base}/${file}"
    echo "[postCreate] Trying ${url} ..."
    if curl -fL "${url}" -o "/tmp/${file}"; then
      return 0
    fi
  done
  return 1
}

verified=false
for base in "$PRIMARY" "$MIRROR1" "$ARCHIVE"; do
  echo "[postCreate] Trying base ${base} ..."
  if curl -fL "${base}/${MAVEN_TGZ}.sha512" -o "/tmp/${MAVEN_TGZ}.sha512" \
     && curl -fL "${base}/${MAVEN_TGZ}" -o "/tmp/${MAVEN_TGZ}"; then
    echo "[postCreate] Downloaded Maven and checksum from ${base}. Verifying..."
    expected=$(awk '{print $1}' "/tmp/${MAVEN_TGZ}.sha512" | tr -d '\r\n')
    actual=$(sha512sum "/tmp/${MAVEN_TGZ}" | awk '{print $1}')
    if [ -n "$expected" ] && [ "$expected" = "$actual" ]; then
      verified=true
      break
    else
      echo "[postCreate][WARN] Checksum mismatch from ${base}. Expected=$expected Actual=$actual"
    fi
  fi
done

if [ "$verified" != true ]; then
  echo "[postCreate][ERROR] Failed to download/verify Maven ${MAVEN_VERSION}. Aborting."
  exit 1
fi

sudo mkdir -p /opt
sudo tar -xzf "/tmp/${MAVEN_TGZ}" -C /opt
sudo ln -sfn "/opt/apache-maven-${MAVEN_VERSION}" /opt/maven
sudo ln -sfn /opt/maven/bin/mvn /usr/local/bin/mvn

if [ ! -f /etc/profile.d/maven_home.sh ]; then
  echo "export MAVEN_HOME=/opt/maven" | sudo tee /etc/profile.d/maven_home.sh >/dev/null
  echo 'export PATH="${MAVEN_HOME}/bin:${PATH}"' | sudo tee -a /etc/profile.d/maven_home.sh >/dev/null
  sudo chmod 644 /etc/profile.d/maven_home.sh
fi

echo "[postCreate] Maven installed. Version info:"
mvn -v || true

echo "[postCreate] Maven 3.9.9 setup complete."
