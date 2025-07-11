# Miaocha 贡献指南

非常感谢您对 **秒查 (Miaocha)** 项目的关注和支持！我们是一个开放、友好的社区，欢迎任何形式的贡献。本指南将帮助您顺利地参与到项目中来。

## 1. 如何参与贡献

您可以通过多种方式为 Miaocha 社区做出贡献：

*   💡 **提出功能建议**: 如果您对项目有任何新想法，欢迎通过 [GitHub Issues](https://github.com/Hinadt-Inc/miaocha/issues/new/choose) 提出。
*   🐛 **报告 Bug**: 在使用中发现任何问题，请通过 [GitHub Issues](https://github.com/Hinadt-Inc/miaocha/issues/new/choose) 告诉我们。
*   📖 **完善文档**: 发现文档中的错误或有待改进之处？您可以直接提交 Pull Request 来修复它。
*   💻 **贡献代码**: 这是最直接的贡献方式，我们欢迎您为项目修复 Bug 或实现新功能。

## 2. 寻找可贡献的任务

在 [GitHub Issues](https://github.com/Hinadt-Inc/miaocha/issues) 页面，我们为适合新人上手的任务打上了 `good first issue` 标签，您可以从这些任务开始。

**重要**: 所有代码贡献**必须**围绕一个已存在的 Issue 进行。在开始编码前，请确保您已经创建了一个 Issue 或在某个现有 Issue 下留言并获得分配。

## 3. 贡献流程

我们遵循标准的 GitHub Fork & Pull Request 工作流。

### a. Fork & Clone

1.  访问 [Miaocha GitHub 仓库](https://github.com/Hinadt-Inc/miaocha)。
2.  点击右上角的 **Fork** 按钮，将主仓库复刻到您自己的账户下。
3.  使用 `git clone` 命令将您复刻的仓库克隆到本地（注意，本项目包含子模块）：
    ```bash
    git clone --recurse-submodules https://github.com/YOUR_USERNAME/miaocha.git
    cd miaocha
    ```
4.  将主仓库添加为上游远程仓库，方便后续同步更新：
    ```bash
    git remote add upstream https://github.com/Hinadt-Inc/miaocha.git
    ```

### b. 创建分支

在进行任何修改之前，请基于 `dev` 分支创建一个新的特性分支。分支命名应清晰地描述其用途。

```bash
# 同步上游 dev 分支的最新代码
git fetch upstream
git checkout -b feature/your-cool-feature upstream/dev
```

推荐的分支命名规范：
*   `feature/`：新功能开发
*   `fix/`：Bug 修复
*   `docs/`：文档修改
*   `refactor/`：代码重构

### c. 开发与提交

1.  在新的分支上进行代码修改。请参考我们的 [本地开发指南](local_development_guide.md) 搭建环境。
2.  完成修改后，使用 `git commit` 提交您的代码。请遵循我们的 **Commit Message 规范**。

### d. 创建 Pull Request (PR)

1.  将您的本地分支推送到您复刻的远程仓库：
    ```bash
    git push -u origin feature/your-cool-feature
    ```
2.  在 GitHub 上，进入您复刻的仓库页面，此时页面会提示您创建一个新的 Pull Request。
3.  点击 "Compare & pull request" 按钮，确保基准分支是 `Hinadt-Inc/miaocha` 的 `dev` 分支。
4.  请遵循 **Pull Request 规范**，填写清晰的标题和描述。
5.  提交 PR 后，我们会尽快进行 Code Review。请关注 PR 下的评论，并及时回应和修改。

## 4. 开发规范

### a. 编码规范

*   **后端 (Java)**: 我们遵循项目内置的 `checkstyle.xml` 规范。请在提交代码前确保通过 Checkstyle 检查。
*   **前端 (React/TypeScript)**: 请遵循项目通用的 Prettier 和 ESLint 规范。

### b. Commit Message 规范

我们采用 [Angular 提交规范](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit)。一个标准的 Commit Message 格式如下：

```
<type>(<scope>): <subject>

<body>

<footer>
```

*   **Type**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore` 等。
*   **Scope**: 本次提交影响的范围，如 `log-search`, `logstash-mgt`, `ui` 等。
*   **Subject**: 简明扼要的变更描述。

**示例**:
```
feat(log-search): add support for fuzzy queries

Implements fuzzy search functionality in the main log search interface, improving user query flexibility.
```

### c. Pull Request 规范

*   **标题**: PR 的标题格式**必须**为 `[ISSUE #XXX] <您的 PR 标题>`，其中 `XXX` 是您所关联的 Issue 编号。
*   **描述**:
    *   清晰地描述此 PR 的**目的**和**主要变更**。
    *   **必须**在 PR 描述的正文中使用关键词 `close #XXX` (或 `fixes #XXX`, `resolves #XXX`) 来关联对应的 Issue。这是自动化 CI 检查的要求，否则您的 PR 将无法通过校验。
    *   如有必要，请附上截图或动图以方便 Reviewer 理解。

## 5. 社区交流

如果您在贡献过程中遇到任何问题，或想与社区成员交流，欢迎通过 [GitHub Discussions](https://github.com/Hinadt-Inc/miaocha/discussions) 与我们联系。

再次感谢您的贡献！
