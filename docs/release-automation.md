# 🚀 秒查系统发版自动化指南

## 概述

本文档介绍了秒查系统的自动化发版流程，包括版本管理、构建、测试、打包和发布的完整流程。

## 发版方式

### 1. 🎯 手动触发发版 (推荐)

通过GitHub Actions手动触发发版流程：

1. 访问项目的GitHub Actions页面
2. 选择 "Release Automation" workflow
3. 点击 "Run workflow"
4. 配置发版参数：
   - **版本号**: 如 `2.1.0`
   - **版本类型**: manual/patch/minor/major
   - **预发布**: 是否为测试版本
   - **跳过测试**: 紧急发版时可选

### 2. 🏷️ 标签触发发版

推送版本标签自动触发发版：

```bash
# 使用脚本自动化
./scripts/release-version.sh 2.1.0

# 或手动操作
git tag -a v2.1.0 -m "Release version 2.1.0"
git push origin v2.1.0
```

## 版本管理脚本使用

### 基础用法

```bash
# 显示当前版本
./scripts/release-version.sh --current

# 更新到指定版本
./scripts/release-version.sh 2.1.0

# 自动计算版本号
./scripts/release-version.sh --next-patch   # 2.0.0 -> 2.0.1
./scripts/release-version.sh --next-minor   # 2.0.0 -> 2.1.0
./scripts/release-version.sh --next-major   # 2.0.0 -> 3.0.0
```

### 高级选项

```bash
# 预览模式（不执行实际更改）
./scripts/release-version.sh --dry-run 2.1.0

# 仅准备版本，不执行git操作
./scripts/release-version.sh --prepare-only 2.1.0

# 仅创建标签
./scripts/release-version.sh --tag-only 2.1.0
```

## 发版流程详解

### 🔍 1. 版本验证和准备
- 解析版本号格式
- 检查代码变更类型
- 确定是否需要构建资产

### 🧪 2. 构建和测试
- 更新Maven项目版本
- 运行完整测试套件
- 构建发布分发包
- 上传构建产物

### 🐳 3. Docker镜像构建
- 构建多架构镜像 (amd64/arm64)
- 推送到GitHub Container Registry
- 自动标签管理

### 📝 4. 生成Release Notes
- 自动分析提交记录
- 按类型分类变更内容
- 生成统计信息
- 包含安装和使用说明

### 🎉 5. 创建GitHub Release
- 创建正式发布页面
- 上传构建资产
- 发布Docker镜像
- 发送发布通知

## 版本号规范

采用[语义化版本](https://semver.org/lang/zh-CN/)规范：

```
主版本号.次版本号.修订号[-预发布标识]

例如：
- 2.1.0        # 正式版本
- 2.1.0-RC1    # 候选版本
- 2.1.0-beta.1 # 测试版本
```

### 版本号升级规则

| 变更类型     | 版本升级 | 示例          |
| ------------ | -------- | ------------- |
| 🐛 Bug修复    | patch    | 2.0.0 → 2.0.1 |
| 🆕 新功能     | minor    | 2.0.1 → 2.1.0 |
| 💥 破坏性变更 | major    | 2.1.0 → 3.0.0 |

## 提交信息规范

为了自动生成高质量的Release Notes，请遵循以下提交信息格式：

```
类型(范围): 描述

feat: 添加用户权限管理功能
fix: 修复数据导出时的编码问题
docs: 更新API文档
style: 代码格式优化
refactor: 重构查询引擎
test: 添加集成测试用例
chore: 更新依赖版本
```

### 提交类型说明

| 类型       | 说明     | Release Notes分类 |
| ---------- | -------- | ----------------- |
| `feat`     | 新功能   | 🆕 新功能          |
| `fix`      | Bug修复  | 🐛 Bug修复         |
| `docs`     | 文档更新 | 📚 文档更新        |
| `style`    | 代码格式 | 🔧 改进优化        |
| `refactor` | 重构     | 🔧 改进优化        |
| `perf`     | 性能优化 | 🔧 改进优化        |
| `test`     | 测试     | 🧪 测试改进        |
| `chore`    | 维护     | 🔧 改进优化        |

## 自动化特性

### 🎯 智能构建
- 检测代码变更类型
- 仅在有实质性变更时构建资产
- 文档变更时跳过构建

### 🔄 多平台支持
- 自动构建多架构Docker镜像
- 支持 linux/amd64 和 linux/arm64

### 📊 统计分析
- 自动统计版本间变更
- 生成提交数量和文件变更统计
- 提供版本对比链接

### 🔗 链接生成
- 自动生成下载链接
- 提供Docker使用命令
- 包含文档和反馈链接

## 发版检查清单

### 发版前检查
- [ ] 代码已合并到主分支
- [ ] 所有测试通过
- [ ] 版本号符合规范
- [ ] Release Notes内容准确
- [ ] Docker镜像构建成功

### 发版后验证
- [ ] GitHub Release创建成功
- [ ] 构建资产可正常下载
- [ ] Docker镜像可正常拉取
- [ ] 版本标签正确推送
- [ ] 文档链接有效

## 常见问题

### Q: 如何回滚发版？
A: 可以通过以下方式：
1. 删除错误的Release和标签
2. 使用正确版本重新发版
3. 更新Docker镜像latest标签

### Q: 如何处理发版失败？
A: 检查失败原因：
1. 查看GitHub Actions日志
2. 验证版本号格式
3. 确认构建环境配置
4. 检查权限设置

### Q: 如何自定义Release Notes？
A: 修改 `.github/workflows/release.yml` 中的生成逻辑，或在发版后手动编辑Release页面。

### Q: 能否同时发布多个版本？
A: 不建议。建议按照版本顺序依次发布，确保版本历史清晰。

## 最佳实践

### 1. 📅 定期发版
- 建议每2-4周发布一次
- 紧急修复可随时发版
- 重大功能发布前先发RC版本

### 2. 🏷️ 版本标签管理
- 使用语义化版本号
- 预发布版本加上标识符
- 保持版本历史的连续性

### 3. 📝 文档维护
- 及时更新CHANGELOG
- 保持README的准确性
- 重要变更需要迁移指南

### 4. 🧪 质量保证
- 发版前充分测试
- 使用预发布版本验证
- 监控发版后的系统状态

## 相关链接

- [语义化版本规范](https://semver.org/lang/zh-CN/)
- [GitHub Actions文档](https://docs.github.com/en/actions)
- [Docker多架构构建](https://docs.docker.com/build/building/multi-platform/)
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)

---

如有问题，请提交Issue或在讨论区交流。 