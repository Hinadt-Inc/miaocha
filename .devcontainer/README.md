Dev Container for GitHub Codespaces
===================================

This repository ships a Codespaces-ready dev container that targets:

- OS: Ubuntu 22.04
- Docker: Enabled via docker-outside-of-docker feature
- Java: JDK 17 (Temurin)
- Node.js: v23.9.0
- Yarn: v1.22.22 (Classic, provisioned via Corepack)
- pnpm: latest (provisioned via Corepack)

What it does
------------

- Installs Java 17, Node 23.9.0, Yarn 1.22.22 (via Corepack), and pnpm (via Corepack).
- Enables Docker CLI inside the container and connects to the host Docker daemon.
- Post-create step pre-pulls required images:
  - `mysql:8.0`
  - `apache/doris:2.1.9-all`
  - `wataken44/ubuntu-latest-sshd:latest`

How to use (Codespaces)
-----------------------

1. Open this repository in GitHub Codespaces.
2. Codespaces will build the dev container automatically using `.devcontainer/devcontainer.json`.
3. After the container is created, the post-create script runs and pulls the required Docker images.

Check installed tools
---------------------

Inside the container terminal:

```
java -version
node -v
yarn -v
pnpm -v
docker version
```

Quick-run the images (examples)
-------------------------------

MySQL 8.0 (ephemeral):

```
docker run --rm -it \
  -e MYSQL_ROOT_PASSWORD=devpass \
  -p 3306:3306 \
  mysql:8.0
```

Apache Doris 2.1.9 (all-in-one example):

```
docker run --rm -it -p 8030:8030 -p 8040:8040 apache/doris:2.1.9-all
```

Ubuntu SSHD (example):

```
docker run --rm -it -p 2222:22 wataken44/ubuntu-latest-sshd:latest
```

Local VS Code (optional)
------------------------

If you prefer local containers instead of Codespaces, use the VS Code Dev Containers extension and open the repository in a container. The same configuration applies.
