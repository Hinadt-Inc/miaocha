## 本地开发指南

本指南将引导您完成"秒查"项目在本地开发环境的完整搭建流程。与常规前后端分离开发模式不同，本项目的**前端应用是作为静态资源，被集成到后端服务中统一打包和提供服务的**。

### 1. 环境准备 (Prerequisites)

在开始之前，请确保您的开发环境中已安装以下软件，并满足版本要求：

* **Java**: `17` 建议使用 JDK17 ,其余版本尚未测试
* **Maven**: `3.8` 或更高版本
* **Docker** 和 **Docker Compose**: 用于运行项目依赖的基础设施

### 2. 获取源码 (Get the Source Code)

使用标准的 Git 命令克隆项目：

```bash
git clone https://github.com/Hinadt-Inc/miaocha.git
cd miaocha
```

### 3. 启动基础设施 (Start Infrastructure)

项目依赖 MySQL 和 Apache Doris 作为核心数据存储。在启动基础设施服务之前，需要先配置系统参数以确保 Doris 能够正常运行。

#### 3.1 配置系统参数 (必须步骤)

Doris 对系统参数有特定要求，启动前必须进行以下配置：

```bash
# 关闭swap分区
sudo swapoff -a

# 设置系统参数
sudo sysctl -w vm.max_map_count=2000000
sudo sysctl -w vm.swappiness=0
sudo sysctl -w fs.file-max=655360

# 验证设置
echo "=== System Configuration ==="
echo "Swap status:"
swapon --show
echo "vm.max_map_count: $(cat /proc/sys/vm/max_map_count)"
echo "vm.swappiness: $(cat /proc/sys/vm/swappiness)"
echo "fs.file-max: $(cat /proc/sys/fs/file-max)"
```

**参数说明：**

- `vm.max_map_count=2000000`: 增加内存映射区域数量上限，Doris 需要大量内存映射
- `vm.swappiness=0`: 禁用交换分区使用，提高性能
- `fs.file-max=655360`: 增加系统文件描述符上限

**重要提示：**

- 这些设置在系统重启后会失效，如需永久生效，请将参数添加到 `/etc/sysctl.conf` 文件中
- 如果不进行这些配置，Doris 容器可能无法正常启动或运行异常

#### 3.2 启动容器服务

系统参数配置完成后，使用 Docker Compose 启动所有必需的基础设施服务：

```bash
# 进入项目根目录，执行以下命令
docker-compose -f docker/compose/Infrastructure.yml up -d
```

启动成功后，您可以通过 `docker ps` 命令检查容器是否成功运行，如下图所示：
<img src="../images/infrastucture.png" width="900"  alt="基础设施容器启动状态"/>

#### 3.3 故障排除

如果 Doris 容器启动失败，请检查：

1. **系统参数是否正确设置**：重新执行步骤 3.1 中的配置命令
2. **容器日志**：使用 `docker logs <container_name>` 查看具体错误信息
3. **资源限制**：确保系统有足够的内存和磁盘空间

### 4. 本地集成开发 (Backend & Frontend Integrated)

在本地开发环境中，为了能通过后端服务直接访问前端页面，您需要先通过 Maven 构建 `miaocha-ui` 模块。该模块的 Maven 构建流程已**完全集成了前端编译**，会自动将生成的静态资源打包成一个 JAR 文件。**您无需手动执行任何 `npm` 命令**。

1. **构建 UI 模块 (关键步骤)**:
   这是在 IDE 中启动 `miaocha-server` 之前必须完成的准备工作。
   ```bash
   # 在项目根目录执行
   # 此命令会构建 ui 模块，并将其产物（一个包含静态资源的 JAR）安装到本地 Maven 仓库
   mvn clean install -pl miaocha-ui
   ```

2. **启动后端服务 (IntelliJ IDEA 为例)**:
    * 当 `miaocha-ui` 模块成功构建后，`miaocha-server` 就可以通过其在 `pom.xml` 中声明的依赖来引用前端资源。
    * **关键配置**: 在 IDE 中，为了确保启动时能加载到最新的前端代码，您需要编辑 `MiaoChaApp` 的 **运行/调试配置 (Run/Debug Configuration)**。
    * 如下图所示，请在 `miaocha-server` 的启动配置中，手动添加 `miaocha-ui` 模块的编译产物 (JAR 包) 作为类路径依赖。

      <img src="../images/miaocha-ui-dev.png" width="900" alt="在 IDEA 中添加 UI 模块作为类路径依赖"/>

    * 配置完成后，直接运行 `MiaoChaApp` 启动类。项目启动成功后，在浏览器中访问后端的地址 (如 `http://localhost:8080`)，即可看到由后端服务提供的完整前端界面。

### 5. 生产打包 (Production Build)

当您需要将项目打包成可执行的 JAR 文件时：

* **标准打包 (包含前端)**:
  ```bash
  # 在项目根目录执行
  mvn clean package
  ```
  这将执行完整的构建流程，包括前端编译，并生成一个包含所有静态资源的“胖”JAR。

* **跳过前端打包 (纯后端)**:
  如果您只需要一个纯后端的服务包，可以使用 `-Pskip-ui` Profile。
  ```bash
  mvn clean package -Pskip-ui
  ```

### 6. (可选) 模拟日志数据 (Mock Log Data)

为了在开发阶段有充足的测试数据，我们提供了一个数据模拟工具。在执行完 `mvn clean package` 后，`miaocha-assembly` 模块会生成最终的部署包。

解压 `miaocha-assembly/target/miaocha-XXXXXXX-bin.tar.gz` 文件，您会看到如下结构：
<img src="../images/mock-doris-data.png" width="900" alt="数据模拟工具目录"/>

请参照其 `bin/start.sh` 脚本中的说明来生成模拟的 Doris 日志数据。
