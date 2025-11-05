# GitHub Actions Workflows

本目录包含项目的所有 GitHub Actions 工作流配置。

## 工作流列表

### sync-dev-to-main.yml
将 dev 分支的更改同步到 main 分支（通过 rebase 方式）。

**触发方式**: 手动触发（workflow_dispatch）

**重要配置**:
- 该工作流使用 Personal Access Token (PAT) 来推送更改
- PAT 是必需的，因为 `GITHUB_TOKEN` 不会触发其他工作流（这是 GitHub 的安全特性）
- 工作流会回退到 `GITHUB_TOKEN`（如果 PAT 未配置），但这样将无法触发后续工作流

**所需的 Secret 配置**:
- `PAT`: Personal Access Token，需要具有 `repo` 权限
  - 如果未配置此 Secret，工作流仍可运行，但不会触发 sync-to-gitlab 工作流

### sync-to-gitlab.yml
将 GitHub 仓库同步到内部 GitLab 服务器。

**触发方式**: 
- 当 dev 或 main 分支有推送时自动触发
- 依赖于 sync-dev-to-main 工作流使用 PAT 来正确触发

**所需的 Secret 配置**:
- `GITLAB_USERNAME`: GitLab 用户名
- `GITLAB_TOKEN`: GitLab 访问令牌

## 设置说明

### 如何创建和配置 PAT

1. 访问 GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 点击 "Generate new token (classic)"
3. 设置 Token 名称，例如: "Workflow Trigger Token"
4. 选择权限:
   - ✅ `repo` (完整的仓库访问权限)
5. 生成 Token 并复制

### 如何配置 Repository Secret

1. 访问仓库的 Settings → Secrets and variables → Actions
2. 点击 "New repository secret"
3. Name: `PAT`
4. Value: 粘贴上面生成的 Personal Access Token
5. 点击 "Add secret"

### 工作流链

```
sync-dev-to-main (手动触发)
    ↓
推送到 main 分支 (使用 PAT)
    ↓
触发 sync-to-gitlab (自动)
    ↓
同步到内部 GitLab
```

## 故障排除

### 问题: sync-to-gitlab 工作流没有被触发

**原因**: sync-dev-to-main 工作流可能使用了 `GITHUB_TOKEN` 而不是 PAT。

**解决方案**:
1. 确认已创建并配置了 PAT Secret
2. 检查 PAT 是否具有正确的权限（repo）
3. 检查 PAT 是否已过期

### 问题: GitLab 同步失败

**原因**: GitLab 凭据可能未配置或已过期。

**解决方案**:
1. 检查 `GITLAB_USERNAME` 和 `GITLAB_TOKEN` Secrets 是否已配置
2. 验证 GitLab Token 是否有效
3. 确认网络连接到内部 GitLab 服务器

## 参考资料

- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [触发工作流的事件](https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows)
- [GITHUB_TOKEN 的限制](https://docs.github.com/en/actions/security-guides/automatic-token-authentication#using-the-github_token-in-a-workflow)
