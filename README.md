<div align="center">
<img src="docs/images/logo.png" alt="Miaocha Logo" width="100" height="100" />

# 🔍 Miaocha — Enterprise-Grade Log Management Platform

## 🚀 High-Performance Log Search Powered by Apache Doris

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)
[![Latest Release](https://img.shields.io/github/v/release/Hinadt-Inc/miaocha?style=flat-square&label=Latest%20Release&color=2ea44f)](https://github.com/Hinadt-Inc/miaocha/releases/latest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/Hinadt-Inc/miaocha/package.yml?style=flat-square&branch=main)](https://github.com/Hinadt-Inc/miaocha/actions)
[![Java Version](https://img.shields.io/badge/Java-17+-orange.svg?style=flat-square)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg?style=flat-square)](https://spring.io/projects/spring-boot)
[![React Version](https://img.shields.io/badge/React-19-blue.svg?style=flat-square&logo=react)](https://react.dev/)
[![GitHub Stars](https://img.shields.io/github/stars/Hinadt-Inc/miaocha?style=flat-square&color=yellow)](https://github.com/Hinadt-Inc/miaocha/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/Hinadt-Inc/miaocha?style=flat-square&color=blue)](https://github.com/Hinadt-Inc/miaocha/network/members)
[![Contributors](https://img.shields.io/github/contributors/Hinadt-Inc/miaocha?style=flat-square&color=green)](https://github.com/Hinadt-Inc/miaocha/graphs/contributors)

<p>
  <a href="README.md"><b>English</b></a> |
  <a href="README-cn.md">Chinese</a>
  <br/>
</p>

</div>

---

## 📖 About Miaocha

Miaocha is an open-source log analytics platform built for enterprises. It is powered by Spring Boot 3.x and React 19, with Apache Doris as the core storage and query engine. Miaocha delivers millisecond-level search over massive logs, advanced analytics, and a one-stop, UI-driven ingestion experience. With distributed deployment and built-in Logstash process orchestration, Miaocha simplifies operating large-scale log pipelines and boosts operational visibility.

### ⭐ Core Features

- ⚡ Lightning Search: Apache Doris–backed, high-throughput indexing for millisecond responses on large datasets.
- 🎯 Intelligent Analytics: Rich, multi-dimensional queries with fine-grained isolation across business lines and configurable access controls.
- 🚀 One-Stop Ingestion: UI-based Logstash management for remote deployment, one-click start/stop, hot config reload, and elastic scaling.
- ✍️ Pro SQL Editor: Full-featured SQL editor with autocomplete, syntax highlighting, query history, and export.

---

## 📸 Screenshots

### Log Search

<img src="docs/images/logsearch.png" width="900" alt="Log Search"/>

### Logstash Process Management

<img src="docs/images/logstashmanage.png" width="900" alt="Logstash Management"/>

### SQL Editor

<img src="docs/images/sqlQuery.png" width="900" alt="SQL Query"/>

---

## 🚀 Quick Start

### Run Locally

```bash
# 1. Clone
git clone https://github.com/Hinadt-Inc/miaocha
cd miaocha

# 2. Configure database
# Edit: miaocha-server/src/main/resources/application-dev.yml

# 3. Build
mvn clean package

# 4. Start
cp miaocha-assembly/target/miaocha-assembly-*.gz ./
tar -zxvf miaocha-assembly-*.gz
cd miaocha-assembly-xxxx
./bin/start.sh
```

### Docker Deployment

```bash
# Build and start
./scripts/build-start-docker.sh

# Cleanup images
./scripts/clean-docker-images.sh
```

---

## 📝 Documentation

Development Guides

- [Architecture Design](docs/dev_guide/architecture_design.md)
- [Local Development Guide](docs/dev_guide/local_development_guide.md)
- [Contribution Guide](docs/dev_guide/contribution_guide.md)

Product Guides

- [Quick Start](docs/user_guide/quick_start.md)
- [Log Ingestion Guide](docs/user_guide/log_access_guide.md)
- [Query and Analysis Guide](docs/user_guide/query_and_analysis_guide.md)
- [FAQ](docs/user_guide/faq.md)

We are continuously improving the documentation. Contributions via [Issues](https://github.com/Hinadt-Inc/miaocha/issues) or [Pull Requests](https://github.com/Hinadt-Inc/miaocha/pulls) are very welcome!

---

## 🌟 Community & Contributions

We are an open and friendly community. All kinds of contributions are welcome!

- Report Issues: https://github.com/Hinadt-Inc/miaocha/issues/new/choose
- Contribute Code: please read [Contribution Guide](docs/dev_guide/contribution_guide.md)
- Improve Docs: help refine or add missing parts

If you find Miaocha helpful, please star ⭐ the repo: https://github.com/Hinadt-Inc/miaocha

---

<sub>🎨 Built with ❤️ | 📜 Licensed under Apache 2.0</sub>
