# 🧪 秒查系统测试覆盖率指南

## 📋 概述

本项目采用现代化的测试覆盖率报告系统，专为秒查(MiaoCha)日志管理系统定制，集成了以下先进工具：

- **JaCoCo** - Java代码覆盖率分析，支持行覆盖率、分支覆盖率
- **Allure** - 精美的测试报告生成器，支持丰富的可视化和中文界面
- **Maven Surefire** - 单元测试执行器，支持并行测试
- **Maven Failsafe** - 集成测试执行器

## 🚀 快速开始

### 1. 运行完整的测试覆盖率报告

```bash
# 使用我们的智能测试脚本
./scripts/run-tests-with-coverage.sh

# 或者使用Maven命令
mvn clean test verify jacoco:report allure:report
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

### 📋 Allure测试报告

- **位置**: `allure-report/index.html`
- **特性**:
  - 精美的Web界面，支持中文
  - 按Epic、Feature、Story分组的测试用例
  - 测试历史趋势和统计
  - 失败用例智能分类（针对秒查系统特点）
  - 丰富的附件和参数显示
  - 测试步骤详细记录

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

### 使用Allure注解丰富测试报告

秒查系统的测试应当遵循以下注解规范：

```java
@Epic("秒查日志管理系统")
@Feature("用户管理")
@Story("用户登录")
@Severity(SeverityLevel.CRITICAL)
@DisplayName("用户登录功能测试")
@Description("验证用户能够使用正确的用户名和密码成功登录秒查系统")
@Owner("开发团队")
@Issue("MIAOCHA-123")
class UserLoginTest {

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("成功登录测试")
    @Description("使用有效凭据登录系统")
    void shouldLoginSuccessfully() {
        Allure.step("准备测试数据", () -> {
            Allure.parameter("用户名", "admin");
            Allure.parameter("密码", "***");
        });
        
        Allure.step("执行登录", () -> {
            // 测试逻辑
        });
        
        Allure.step("验证结果", () -> {
            Allure.attachment("登录响应", "application/json", response);
        });
    }
}
```

### Epic/Feature/Story 分类指南

针对秒查系统，建议使用以下分类：

**Epic级别**:
- 秒查日志管理系统

**Feature级别**:
- 用户管理
- Logstash进程管理
- 数据源管理
- SQL查询引擎
- 日志检索
- 数据导出
- 系统监控

**Story级别**:
- 用户认证
- 进程扩容缩容
- 动态字段转换
- 关键字搜索
- Excel导出
- 性能监控

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

### 2. 自动排除的代码

系统自动排除以下代码（无需测试）：
- 配置类 (`*Config.class`, `*Configuration.class`)
- 启动类 (`*Application.class`, `*App.class`)
- 实体类 (`entity/*`, `dto/*`, `vo/*`)
- 常量和枚举 (`constant/*`, `enums/*`)
- 异常类 (`exception/*`)

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
├── allure-report/            # Allure测试报告
│   ├── index.html           # 报告主页
│   ├── data/                # 报告数据
│   └── plugins/             # 插件资源
└── target/
    ├── site/jacoco/         # JaCoCo原始报告
    ├── allure-results/      # Allure测试结果
    └── surefire-reports/    # Surefire测试报告
```

## 🎯 质量门控

项目设置了以下质量标准：

|  指标   | 最低要求 | 严格模式 |      说明      |
|-------|------|------|--------------|
| 行覆盖率  | 70%  | 80%  | 代码行被测试覆盖的比例  |
| 分支覆盖率 | 65%  | 75%  | 条件分支被测试覆盖的比例 |
| 类覆盖率  | -    | 70%  | 类被测试覆盖的比例    |

## 🐛 常见问题

### Q: 为什么覆盖率报告是空的？

A: 确保测试执行成功，检查是否有编译错误或测试失败。使用 `mvn clean test` 重新运行。

### Q: Allure报告无法显示？

A: 检查 `target/allure-results` 目录是否存在测试结果文件。确保测试类中使用了Allure注解。

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

### Q: Allure报告显示乱码？

A: 确保在 `allure.properties` 中设置了正确的语言：

```properties
allure.report.language=zh
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
        path: |
          coverage-report/
          allure-report/
          
    - name: 发布 Allure 报告
      uses: simple-elf/allure-report-action@master
      if: always()
      with:
        allure_results: target/allure-results
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
                    
                    allure([
                        includeProperties: false,
                        jdk: '',
                        properties: [],
                        reportBuildPolicy: 'ALWAYS',
                        results: [[path: 'target/allure-results']]
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

在Allure报告中重点关注：
- **总体统计** - 测试总数、通过率、失败率
- **分类视图** - 按Feature和Story的组织情况
- **趋势图** - 历史执行情况
- **缺陷分类** - 失败原因分析

## 📚 更多资源

- [JaCoCo官方文档](https://www.jacoco.org/jacoco/trunk/doc/)
- [Allure官方文档](https://docs.qameta.io/allure/)
- [JUnit 5用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Maven Surefire插件](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [秒查系统开发规范](docs/development-guide.md)

---

🎉 **享受现代化的测试体验！** 如有问题，请查看日志或联系开发团队。

**秒查团队** 致力于提供最佳的日志管理体验 🚀
