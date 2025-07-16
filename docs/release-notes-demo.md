# 🚀 RocketMQ风格Release Notes演示

## 📋 工作流程说明

### 1. 开发阶段 - 使用Conventional Commits
```bash
# feature分支上的普通提交
git commit -m "feat(ui): 添加数据源连接测试功能"
git commit -m "fix(api): 修复查询接口参数验证问题"  
git commit -m "docs: 更新API文档"
git commit -m "chore: 升级依赖版本"
```

### 2. 合并到dev分支 - 使用[ISSUE #xx]格式
```bash
# PR合并时的merge commit (这是关键！)
git merge --no-ff feature/data-source-test -m "[ISSUE #40] 完善环境搭建，本地开发文档"
git merge --no-ff feature/frontend-sync -m "[ISSUE #35] 同步前端代码"  
git merge --no-ff fix/logstash-performance -m "[ISSUE #28] 优化LogStash任务耗时信息"
git merge --no-ff feature/github-actions -m "[ISSUE #32] Github Action 支持PR设置 label 根据PR改动自动部署测试环境"
```

### 3. 发版时自动生成Release Notes
脚本会：
1. **优先检查merge commits** (包含`[ISSUE #xx]`格式)
2. **简单列出所有变更** (不分类，真实RocketMQ风格)
3. **生成与Apache RocketMQ完全相同的Release Notes**

---

## 🎯 最终生成的Release Notes效果

### GitHub Release页面效果

```markdown
## What's Changed

This version includes several improvements and bug fixes based on community feedback.

* [ISSUE #40] 完善环境搭建，本地开发文档
* [ISSUE #35] 同步前端代码  
* [ISSUE #32] Github Action 支持PR设置 label 根据PR改动自动部署测试环境
* [ISSUE #29] 合并秒查前端仓库
* [ISSUE #28] 优化LogStash任务耗时信息
* [ISSUE #25] 补充项目开发相关文档
* [ISSUE #22] 编辑数据源时按需更改信息,验证数据源连接
* Fix unstable test in BrokerOuterAPITest
* Optimize the log output of tlsHelper
* Update dependencies to latest versions

### New Contributors
* @张三 made their first contribution in #123
* @李四 made their first contribution in #124  
* @王五 made their first contribution in #125

**Full Changelog**: https://github.com/your-org/miaocha/compare/v2.0.0...v2.1.0
```

---

## 🔄 与Apache RocketMQ对比

### Apache RocketMQ 5.3.3 Release Notes (参考)
```markdown
## What's Changed

This version no longer supports "ACL 1.0" related features, please use "ACL 2.0".
Additionally, this minor version includes several general bug fixes.

* [ISSUE #8997] Ensure there is an opportunity to send a retry message when broker no response by @gaoyf in #9137
* [ISSUE #9233] Query message in tiered storage may fail for the first correct index file was not selected by @bxfjb in #9234
* [ISSUE #9246] Support init offset mode in PopConsumerService by @lizhimins in #9247
* [ISSUE #9249] When delivery fails, there is an incorrect start offset in the delivery settings by @coolmoon101 in #9252
...

### Contributors
* @fuyou001
* @dingshuangxi888
* @ymwneu
...

**Full Changelog**: rocketmq-all-5.3.2...rocketmq-all-5.3.3
```

### 我们的Release Notes (基于相同格式)
✅ **相同的专业格式**  
✅ **相同的[ISSUE #xx]条目格式**  
✅ **相同的简单列表方式**（不分类）  
✅ **相同的贡献者列表**  
✅ **相同的Full Changelog链接**

---

## 🎯 关键技术实现

### 1. Merge Commits优先策略
```bash
# 脚本优先检查merge commits
merge_commits=$(git log --merges --oneline --pretty=format:"* %s" "$last_tag..HEAD" | grep -E "\[ISSUE.*\]")

# 如果没有merge commits，则查找所有[ISSUE #xx]格式提交
all_issue_commits=$(git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" | grep -E "\[ISSUE.*\]")
```

### 2. 简单列表生成
```bash
# 直接列出所有[ISSUE #xx]格式的变更，不分类（真实RocketMQ格式）
issue_commits=$(git log --merges --oneline --pretty=format:"* %s" "$last_tag..HEAD" | grep -E "\[ISSUE.*\]")

# 如果还有其他格式的提交，也列出来
other_commits=$(git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" | grep -v -E "\[ISSUE.*\]")
```

### 3. 自动化触发
- **手动触发**: GitHub Actions workflow_dispatch
- **标签触发**: 推送`v*`标签自动触发
- **脚本触发**: 本地`./scripts/release-version.sh`

---

## 💡 使用建议

### 开发团队工作流程
1. **Feature开发**: 使用conventional commits (`feat:`, `fix:`, `docs:`)
2. **PR合并**: 确保merge commit使用`[ISSUE #xx] 描述`格式
3. **发版准备**: 运行发版脚本，自动生成专业Release Notes
4. **GitHub发版**: 使用生成的Release Notes发布

### 提交消息最佳实践
```bash
# ✅ 开发阶段 - conventional commits
git commit -m "feat(auth): 添加用户认证功能"
git commit -m "fix(api): 修复分页查询问题"

# ✅ 合并阶段 - [ISSUE #xx]格式
git merge --no-ff feature/auth -m "[ISSUE #42] 实现用户认证与权限控制系统"
git merge --no-ff fix/pagination -m "[ISSUE #43] 修复API分页查询异常问题"
```

这样就能生成与Apache RocketMQ完全相同风格的专业Release Notes！ 