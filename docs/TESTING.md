# 🧪 秒查系统测试覆盖率指南

## 📋 概述

本项目采用现代化的测试覆盖率报告系统，专为秒查(MiaoCha)日志管理系统定制，集成了以下先进工具：

- **JaCoCo** - Java代码覆盖率分析，支持行覆盖率、分支覆盖率
- **Maven Surefire** - 单元测试执行器，支持并行测试
- **Maven Failsafe** - 集成测试执行器

## 🚀 快速开始

### 1. 运行完整的测试覆盖率报告

```bash
# 使用我们的智能测试脚本
./scripts/run-tests-with-coverage.sh

# 或者使用Maven命令
mvn clean test verify jacoco:report
```

### 2. 只运行单元测试

```bash
# 使用脚本
./scripts/run-tests-with-coverage.sh --test-only --skip-integration

# 或者使用Maven
mvn clean test jacoco:report
```

### 3. 生成报告并启动Web服务器

```bash
# 在8000端口启动服务器
./scripts/run-tests-with-coverage.sh --serve

# 在自定义端口启动
./scripts/run-tests-with-coverage.sh --serve 9000
```

## 📊 报告类型

### 🔍 JaCoCo代码覆盖率报告

- **位置**: `coverage-report/index.html`
- **特性**:
  - 行覆盖率和分支覆盖率统计
  - 代码高亮显示覆盖情况
  - 支持多模块聚合报告
  - 可配置覆盖率阈值检查
  - 专门排除配置类、实体类等不需要测试的代码



## ⚙️ 秒查系统特定配置

### 测试分类配置

针对秒查系统的特点，我们定义了以下测试失败分类：

- **业务逻辑缺陷** - 核心业务逻辑问题
- **日志处理问题** - 日志收集、解析、存储功能问题
- **数据源连接问题** - 数据库或外部数据源连接问题
- **SSH连接问题** - SSH远程连接或文件传输问题
- **配置和依赖问题** - Spring配置、Bean注入问题
- **数据导出问题** - Excel导出或数据格式转换问题
- **性能问题** - 查询性能或系统响应时间问题

### 覆盖率阈值配置

在 `pom.xml` 中可以调整覆盖率要求：

```xml
<properties>
    <!-- 标准覆盖率要求 -->
    <jacoco.line.coverage.minimum>0.70</jacoco.line.coverage.minimum>
    <jacoco.branch.coverage.minimum>0.65</jacoco.branch.coverage.minimum>
</properties>
```

### 严格模式

使用 `coverage` profile 进行更严格的覆盖率检查：

```bash
# 使用脚本
./scripts/run-tests-with-coverage.sh --strict

# 使用Maven
mvn clean test -Pcoverage
```

严格模式要求：
- 行覆盖率 ≥ 80%
- 分支覆盖率 ≥ 75%
- 类覆盖率 ≥ 70%

## 🏗️ 测试编写指南



### 测试分类

- **单元测试**: `*Test.java` - 测试单个类或方法
- **集成测试**: `*IntegrationTest.java` 或 `*IT.java` - 测试组件间交互

## 📈 覆盖率优化建议

### 1. 聚焦核心业务逻辑

优先为以下代码编写测试：
- LogstashProcessService - 进程管理核心逻辑
- SQL查询转换器 - 动态字段转换逻辑
- 用户认证服务 - 安全相关功能
- 数据导出服务 - 业务数据处理
- 关键字搜索引擎 - 搜索算法

### 2. 智能排除无关代码

为了让覆盖率统计更加准确地反映真正的业务逻辑代码，系统自动排除以下类型的代码：

**📋 数据模型类（无业务逻辑）**：
- 实体类 (`entity/**`, `domain/entity/**`) - 纯数据定义
- DTO类 (`dto/**`, `domain/dto/**`) - 数据传输对象，主要是getter/setter
- VO类 (`vo/**`, `domain/vo/**`) - 视图对象
- Mapper接口 (`mapper/**`, `domain/mapper/**`) - MyBatis映射器接口

**⚙️ 配置和基础设施类**：
- 启动类 (`MiaoChaApp.class`, `*Application.class`, `*App.class`)
- 配置类 (`*Config.class`, `*Configuration.class`, `config/**`)
- 安全配置 (`security/*Config*.class`, `filter/**`)

**🔧 工具和辅助类**：
- 常量类 (`constant/**`, `constants/**`)
- 枚举类 (`enums/**`, `domain/entity/enums/**`)
- 异常类 (`exception/**`, `common/exception/**`)
- 注解类 (`annotation/**`, `common/annotation/**`)
- 简单转换器 (`converter/**/*Converter.class`, `domain/converter/**`)
- 审计工具 (`common/audit/**`)

**🌐 控制器层（可选）**：
- Endpoint控制器 (`endpoint/**`) - 主要处理HTTP请求绑定

**💡 这样做的好处**：
- 覆盖率统计更专注于业务逻辑代码
- 避免因DTO、配置类等稀释真实覆盖率
- 开发者可以更清晰地了解核心业务代码的测试情况

### 3. 提升分支覆盖率

重点关注：
- Logstash状态判断分支
- SQL转换条件分支
- 用户权限验证分支
- 数据源连接异常处理

## 🔧 脚本命令参考

我们的测试脚本支持多种使用模式：

```bash
# 显示帮助
./scripts/run-tests-with-coverage.sh --help

# 只清理构建文件
./scripts/run-tests-with-coverage.sh --clean-only

# 只运行测试，不生成报告
./scripts/run-tests-with-coverage.sh --test-only

# 只生成报告，不运行测试
./scripts/run-tests-with-coverage.sh --coverage-only

# 跳过集成测试
./scripts/run-tests-with-coverage.sh --skip-integration

# 严格模式 + 启动服务器
./scripts/run-tests-with-coverage.sh --strict --serve
```

## 📁 报告目录结构

测试运行后会生成以下目录结构：

```
├── coverage-report/           # JaCoCo覆盖率报告
│   ├── index.html            # 主报告页面
│   ├── aggregate/            # 聚合报告
│   ├── miaocha-server/       # 服务器模块报告
│   └── miaocha-assembly/     # 组装模块报告
└── target/
    ├── site/jacoco/         # JaCoCo原始报告
    └── surefire-reports/    # Surefire测试报告
```

## 🎯 质量门控

项目设置了以下质量标准：

| 指标       | 最低要求 | 严格模式 | 说明                     |
| ---------- | -------- | -------- | ------------------------ |
| 行覆盖率   | 70%      | 80%      | 代码行被测试覆盖的比例   |
| 分支覆盖率 | 65%      | 75%      | 条件分支被测试覆盖的比例 |
| 类覆盖率   | -        | 70%      | 类被测试覆盖的比例       |

## 🐛 常见问题

### Q: 为什么覆盖率报告是空的？

A: 确保测试执行成功，检查是否有编译错误或测试失败。使用 `mvn clean test` 重新运行。



### Q: 如何排除某些类的覆盖率检查？

A: 在JaCoCo插件配置中添加排除规则：

```xml
<configuration>
    <excludes>
        <exclude>**/YourClass.class</exclude>
    </excludes>
</configuration>
```

### Q: 测试运行很慢？

A: 可以使用并行测试或跳过集成测试：

```bash
# 跳过集成测试
./scripts/run-tests-with-coverage.sh --skip-integration

# Maven并行测试
mvn test -T 1C
```



## 🔄 CI/CD集成

### GitHub Actions示例

```yaml
name: 秒查系统测试覆盖率

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: 设置 JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: 缓存 Maven 依赖
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        
    - name: 运行测试覆盖率
      run: |
        chmod +x scripts/run-tests-with-coverage.sh
        ./scripts/run-tests-with-coverage.sh
        
    - name: 上传覆盖率报告
      uses: actions/upload-artifact@v3
      with:
        name: coverage-reports
        path: coverage-report/
```

### Jenkins Pipeline示例

```groovy
pipeline {
    agent any
    
    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }
    
    stages {
        stage('检出代码') {
            steps {
                checkout scm
            }
        }
        
        stage('测试覆盖率') {
            steps {
                sh 'chmod +x scripts/run-tests-with-coverage.sh'
                sh './scripts/run-tests-with-coverage.sh'
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'coverage-report',
                        reportFiles: 'index.html',
                        reportName: '代码覆盖率报告'
                    ])
                    

                }
            }
        }
    }
}
```

## 📈 测试指标监控

### 关键指标

1. **测试通过率** - 应保持在95%以上
2. **覆盖率趋势** - 应逐步提升
3. **测试执行时间** - 应控制在合理范围内
4. **失败分布** - 关注失败模式

### 报告解读

在JaCoCo覆盖率报告中重点关注：
- **总体统计** - 覆盖率百分比、代码行数
- **模块分解** - 各模块的覆盖率情况
- **详细分析** - 具体到文件和方法的覆盖率

## 📚 更多资源

- [JaCoCo官方文档](https://www.jacoco.org/jacoco/trunk/doc/)

- [JUnit 5用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Maven Surefire插件](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [秒查系统开发规范](docs/development-guide.md)

---

🎉 **享受现代化的测试体验！** 如有问题，请查看日志或联系开发团队。

**秒查团队** 致力于提供最佳的日志管理体验 🚀
