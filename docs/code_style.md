# 代码格式化指南

本项目采用现代化的自动代码格式化工具来确保代码风格的一致性。我们使用了业界最流行的工具组合：

## 工具栈

### 1. Spotless Maven Plugin

- **作用**: 自动代码格式化
- **支持**: Java, XML, Markdown, POM文件
- **标准**: Google Java Format (AOSP风格，4空格缩进)
- **版本**: 2.43.0

### 2. Checkstyle

- **作用**: 静态代码分析和风格检查
- **标准**: 自定义配置（实用模式）
- **版本**: 3.3.0

### 3. EditorConfig

- **作用**: 确保不同IDE的编码风格一致性
- **配置**: 统一的缩进、换行符、字符编码设置

## 快速开始

### 1. 安装Git Hooks（推荐）

```bash
# 安装pre-commit hook，每次提交时自动格式化代码
./scripts/install-git-hooks.sh
```

### 2. 手动格式化代码

```bash
# 格式化所有代码并运行质量检查
./scripts/format-code.sh

# 仅应用格式化，不运行检查
./scripts/format-code.sh --apply-only

# 仅运行检查，不应用格式化
./scripts/format-code.sh --check-only

# 快速模式（跳过质量检查）
./scripts/format-code.sh --quick

# 跳过测试
./scripts/format-code.sh --skip-tests
```

### 3. Maven命令

```bash
# 应用代码格式化
mvn spotless:apply

# 检查代码格式化
mvn spotless:check

# 运行代码质量检查（可选）
mvn checkstyle:check

# 生成checkstyle报告
mvn checkstyle:checkstyle

# 完整构建（包含格式化）
mvn clean install

# 快速构建（跳过代码质量检查）
mvn clean install -Pquick
```

### 4. 自定义Profile

```bash
# 使用quick profile跳过代码质量检查，适合开发阶段
mvn clean install -Pquick

# 正常构建包含所有检查，适合发布前
mvn clean install
```

## 代码风格规范

### Java代码

- **缩进**: 4个空格（不使用Tab）
- **行长度**: 最大120字符
- **导入**: 自动排序和清理未使用的导入
- **注解**: 自动格式化位置
- **风格**: Google Java Format (AOSP变体)

### XML文件

- **缩进**: 4个空格
- **行长度**: 最大120字符
- **属性**: 多属性时自动换行

### Maven POM文件

- **自动排序**: 依赖、插件、属性
- **缩进**: 4个空格
- **保持空行**: 是

### Markdown文件

- **格式化**: 使用Flexmark
- **不修剪**: 行尾空格（某些Markdown语法需要）

## IDE集成

### IntelliJ IDEA

1. 安装Google Java Format插件
2. 启用EditorConfig支持
3. 配置自动格式化：`Settings` → `Tools` → `Actions on Save`

### VS Code

1. 安装Java Extension Pack
2. 安装EditorConfig插件
3. 配置format on save

### Eclipse

1. 导入Google Java格式化配置
2. 启用EditorConfig支持

## Maven阶段集成

代码格式化和质量检查已集成到Maven生命周期中：

- **process-sources阶段**: 自动应用代码格式化
- **verify阶段**: 检查代码格式和运行Checkstyle

```bash
# 正常构建会自动运行所有检查
mvn clean install

# 跳过代码质量检查的快速构建
mvn clean install -Pquick
```

## Git工作流

### Pre-commit Hook

安装后，每次`git commit`时会自动：
1. 格式化已暂存的Java文件
2. 重新暂存格式化后的文件
3. 运行编译检查确保代码仍可编译

### 绕过Hook（不推荐）

```bash
# 跳过pre-commit hook
git commit --no-verify
```

## 常见问题

### Q: 代码格式化失败怎么办？

A:
1. 检查Java语法是否正确
2. 运行 `mvn clean compile` 确保代码可编译
3. 手动运行 `mvn spotless:apply` 查看详细错误信息

### Q: 如何在CI/CD中集成？

A:

```yaml
# GitHub Actions示例
- name: Check code style
  run: mvn spotless:check checkstyle:check
```

### Q: 如何临时禁用格式化？

A:

```java
// 在代码块前后添加注释
// @formatter:off
private String uglyCode   =    "不会被格式化";
// @formatter:on
```

### Q: 如何自定义格式化规则？

A: 修改 `pom.xml` 中的Spotless配置，参考[官方文档](https://github.com/diffplug/spotless)

## 配置文件说明

- **`.editorconfig`**: IDE编辑器配置
- **`checkstyle.xml`**: Checkstyle质量检查配置
- **`pom.xml`**: Maven插件配置
- **`scripts/`**: 辅助脚本

## 最佳实践

1. **提交前**: 始终运行 `./scripts/format-code.sh` 确保代码格式正确
2. **新代码**: 遵循既定的代码风格，工具会自动处理
3. **遗留代码**: 逐步格式化，避免一次性修改过多文件
4. **团队协作**: 确保所有团队成员都安装了Git hooks

## 性能优化

- 使用 `mvn clean install -Pquick` 进行快速构建
- 代码格式化仅在有Java文件变更时运行
- Checkstyle检查可以单独执行：`mvn checkstyle:check`

---

## 更多信息

- [Spotless官方文档](https://github.com/diffplug/spotless)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Checkstyle官方文档](https://checkstyle.org/)
- [EditorConfig官方网站](https://editorconfig.org/)

## 配置说明

### Spotless配置

- **Java格式化**: Google Java Format (AOSP风格)
- **缩进**: 4空格
- **行长度**: 120字符
- **自动导入优化**: 是
- **去除未使用导入**: 是

### Checkstyle配置

- **基于**: 自定义配置（实用模式）
- **缩进**: 不检查（由Spotless处理）
- **行长度**: 120字符
- **严重级别**: 警告（不会阻断构建）
- **专注**: 代码质量问题而非格式细节

### EditorConfig配置

- **Java文件**: 4空格缩进
- **XML文件**: 4空格缩进
- **JSON/YAML**: 2空格缩进
- **字符编码**: UTF-8
- **换行符**: LF

