#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

function show_usage() {
    echo "用法: $0 [选项] <version>"
    echo ""
    echo "选项:"
    echo "  -h, --help                      显示帮助"
    echo "  -c, --current                   显示当前版本"
    echo "  --next-patch                    自动计算下一个patch版本"
    echo "  --next-minor                    自动计算下一个minor版本"
    echo "  --next-major                    自动计算下一个major版本"
    echo "  --dry-run                       预览模式，不执行实际更改"
    echo "  --generate-release-notes-only   仅生成Release Notes到stdout"
    echo ""
    echo "示例:"
    echo "  $0 2.1.0                        升级版本到2.1.0并提交"
    echo "  $0 --next-patch                 自动升级patch版本并提交"
    echo "  $0 --dry-run --next-patch       预览操作"
    exit 1
}

function get_repository_info() {
    # 获取远程仓库URL并解析为changelog URL
    local remote_url=$(git remote get-url origin 2>/dev/null || echo "")
    
    if [ -z "$remote_url" ]; then
        return
    fi
    
    # 标准化URL格式，移除.git后缀
    remote_url=$(echo "$remote_url" | sed 's/\.git$//')
    
    local base_url=""
    local repo_path=""
    
    # 处理不同的URL格式
    case "$remote_url" in
        # GitHub HTTPS格式: https://github.com/owner/repo
        https://github.com/*)
            base_url="https://github.com"
            repo_path="${remote_url#https://github.com/}"
            ;;
        # GitHub SSH格式: git@github.com:owner/repo  
        git@github.com:*)
            base_url="https://github.com"
            repo_path="${remote_url#git@github.com:}"
            ;;
        # GitLab HTTPS格式: https://gitlab.com/owner/repo
        https://gitlab.com/*)
            base_url="https://gitlab.com"
            repo_path="${remote_url#https://gitlab.com/}"
            ;;
        # GitLab SSH格式: git@gitlab.com:owner/repo
        git@gitlab.com:*)
            base_url="https://gitlab.com"
            repo_path="${remote_url#git@gitlab.com:}"
            ;;
        # 其他HTTPS格式，尝试直接使用
        https://*)
            echo "$remote_url"
            return
            ;;
        # 未知格式，返回空
        *)
            return
            ;;
    esac
    
    # 验证repo_path格式 (应该是 owner/repo 或 group/subgroup/repo)
    if [[ "$repo_path" =~ ^[a-zA-Z0-9._-]+/[a-zA-Z0-9._/-]+$ ]]; then
        echo "$base_url/$repo_path"
    fi
}

function get_current_version() {
    if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
        echo "错误: 未找到 pom.xml 文件" >&2
        exit 1
    fi
    
    # 使用Maven help:evaluate获取准确的项目版本
    cd "$PROJECT_ROOT"
    local version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
    
    if [ -z "$version" ] || [ "$version" = "null" ]; then
        echo "错误: 无法从 pom.xml 获取项目版本" >&2
        echo "请确保："
        echo "  1. Maven 已正确安装"
        echo "  2. pom.xml 文件有效"
        echo "  3. 项目根目录存在 <version> 标签"
        exit 1
    fi
    
    echo "$version"
}

function calculate_next_version() {
    local current_version="$1"
    local version_type="$2"
    
    # 移除SNAPSHOT后缀
    local clean_version=$(echo "$current_version" | sed 's/-SNAPSHOT//')
    
    # 分解版本号
    local major=$(echo "$clean_version" | cut -d. -f1)
    local minor=$(echo "$clean_version" | cut -d. -f2)
    local patch=$(echo "$clean_version" | cut -d. -f3)
    
    case $version_type in
        "patch")
            echo "$major.$minor.$((patch + 1))"
            ;;
        "minor")
            echo "$major.$((minor + 1)).0"
            ;;
        "major")
            echo "$((major + 1)).0.0"
            ;;
        *)
            echo "错误: 未知的版本类型: $version_type"
            exit 1
            ;;
    esac
}

function update_version() {
    local new_version="$1"
    local dry_run="$2"
    
    if [ "$dry_run" = "true" ]; then
        echo "[DRY RUN] 将更新版本到 $new_version"
        return
    fi
    
    if [ -f "$PROJECT_ROOT/pom.xml" ]; then
        # 使用Maven versions插件更新版本，避免sed跨平台兼容性问题
        cd "$PROJECT_ROOT"
        if ! mvn versions:set -DnewVersion="$new_version" -DgenerateBackupPoms=false -q; then
            echo "错误: 无法更新 pom.xml 版本" >&2
            exit 1
        fi
        echo "更新 pom.xml 版本到 $new_version"
    fi
    
    if [ -f "$PROJECT_ROOT/package.json" ]; then
        # 跨平台兼容的sed写法
        if sed --version 2>/dev/null | grep -q "GNU"; then
            # GNU sed (Linux)
            sed -i "s/\"version\": \"[^\"]*\"/\"version\": \"$new_version\"/" "$PROJECT_ROOT/package.json"
        else
            # BSD sed (macOS)
            sed -i "" "s/\"version\": \"[^\"]*\"/\"version\": \"$new_version\"/" "$PROJECT_ROOT/package.json"
        fi
        echo "更新 package.json 版本到 $new_version"
    fi
}

function generate_release_notes() {
    local version="$1"
    local dry_run="$2"
    local only_generate="$3"
    local last_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    
    if [ "$only_generate" != "true" ]; then
        echo "生成 Release Notes..."
    fi
    
    local release_notes="## What's Changed

This version includes several improvements and bug fixes based on community feedback.

"
    
    local commit_range=""
    if [ -z "$last_tag" ]; then
        # 没有找到上一个标签，基于所有符合格式的提交生成Release Notes
        if [ "$only_generate" != "true" ]; then
            echo "没有找到上一个标签，基于符合 [ISSUE #xx] 格式的提交生成Release Notes"
        fi
        commit_range="HEAD"
    else
        # 查找上一个标签（在 last_tag 之前的标签）
        local prev_tag=$(git tag --sort=-version:refname | grep -A1 "^${last_tag}$" | tail -1)
        if [ -n "$prev_tag" ] && [ "$prev_tag" != "$last_tag" ]; then
            # 有上一个标签，比较标签之间的差异
            commit_range="$prev_tag..$last_tag"
            if [ "$only_generate" != "true" ]; then
                echo "比较标签范围: $prev_tag..$last_tag"
            fi
        else
            # 这是第一个标签，使用所有历史提交到该标签
            commit_range="$last_tag"
            if [ "$only_generate" != "true" ]; then
                echo "这是第一个标签，使用到 $last_tag 的所有提交"
            fi
        fi
    fi
    
    # 获取[ISSUE #xx]格式的提交
    local issue_commits=""
    local contributors_info=""
    
    if [ -z "$last_tag" ]; then
        # 没有标签时，获取所有符合格式的提交
        contributors_info=$(git log --oneline --pretty=format:"%h|%s|%an|%ae" | grep -E "\[ISSUE #[0-9]+\]" | head -30 || echo "")
    elif [[ "$commit_range" == *".."* ]]; then
        # 标签范围比较，获取范围内的commits
        contributors_info=$(git log --oneline --pretty=format:"%h|%s|%an|%ae" "$commit_range" | grep -E "\[ISSUE #[0-9]+\]" || echo "")
    else
        # 单个标签，获取到该标签的所有提交
        contributors_info=$(git log --oneline --pretty=format:"%h|%s|%an|%ae" "$commit_range" | grep -E "\[ISSUE #[0-9]+\]" | head -30 || echo "")
    fi
    
    if [ -n "$contributors_info" ]; then
        # 处理每个ISSUE提交，添加贡献者信息
        while IFS='|' read -r hash subject author email; do
            # 跳过内部用户（hinadt.com邮箱）
            if [[ "$email" =~ @hinadt\.com$ ]]; then
                issue_commits="$issue_commits* $subject
"
                continue
            fi
            
            # 从GitHub邮箱提取用户名
            local github_user=""
            if [[ "$email" =~ ^[0-9]+\+([^@]+)@users\.noreply\.github\.com$ ]]; then
                github_user="${BASH_REMATCH[1]}"
            elif [[ "$email" =~ ^([^@]+)@users\.noreply\.github\.com$ ]]; then
                github_user="${BASH_REMATCH[1]}"
            else
                # 如果不是GitHub邮箱，使用作者名
                github_user="$author"
            fi
            
            issue_commits="$issue_commits* $subject by @$github_user
"
        done <<< "$contributors_info"
        
        release_notes="$release_notes$issue_commits
"
    fi
    
    # 获取其他提交
    local other_commits=""
    local other_commits_count=0
    
    if [ -z "$last_tag" ]; then
        # 第一个标签，获取所有非ISSUE格式的提交
        other_commits_count=$(git log --oneline --pretty=format:"%s" | grep -v -E "\[ISSUE.*\]|\[RELEASE PREPARE\]" | grep -v "^$" | wc -l)
        other_commits=$(git log --oneline --pretty=format:"- %s" | grep -v -E "\[ISSUE.*\]|\[RELEASE PREPARE\]" | head -10 || echo "")
    elif [[ "$commit_range" == *".."* ]]; then
        # 标签范围比较，先获取总数，再获取前10条
        other_commits_count=$(git log --oneline --pretty=format:"%s" "$commit_range" | grep -v -E "\[ISSUE.*\]|\[RELEASE PREPARE\]" | grep -v "^$" | wc -l)
        other_commits=$(git log --oneline --pretty=format:"- %s" "$commit_range" | grep -v -E "\[ISSUE.*\]|\[RELEASE PREPARE\]" | head -10 || echo "")
    else
        # 单个标签，先获取总数，再获取前10条
        other_commits_count=$(git log --oneline --pretty=format:"%s" "$commit_range" | grep -v -E "\[ISSUE.*\]|\[RELEASE PREPARE\]" | grep -v "^$" | wc -l)
        other_commits=$(git log --oneline --pretty=format:"- %s" "$commit_range" | grep -v -E "\[ISSUE.*\]|\[RELEASE PREPARE\]" | head -10 || echo "")
    fi
        
    if [ -n "$other_commits" ]; then
            release_notes="$release_notes## Other Changes

$other_commits"
            
            # 如果有超过10条其他提交，添加提示和链接
            if [ "$other_commits_count" -gt 10 ]; then
                local remaining_count=$((other_commits_count - 10))
                local repo_info=$(get_repository_info)
                if [ -n "$repo_info" ]; then
                    if [[ "$commit_range" == *".."* ]]; then
                        # 标签范围比较
                        local prev_tag=$(echo "$commit_range" | cut -d'.' -f1)
                        release_notes="$release_notes
... and $remaining_count more commits. [View all changes]($repo_info/compare/$prev_tag...v$version)"
                    else
                        # 单个标签
                        release_notes="$release_notes
... and $remaining_count more commits. [View all changes]($repo_info/commits/v$version)"
                    fi
                else
                    release_notes="$release_notes
... and $remaining_count more commits."
                fi
            fi
            
            release_notes="$release_notes

"
        fi
    
    # 生成贡献者统计
    if [ -n "$contributors_info" ]; then
        local unique_contributors=""
        local new_contributors=""
        
        # 收集所有贡献者
        while IFS='|' read -r hash subject author email; do
            # 跳过内部用户（hinadt.com邮箱）
            if [[ "$email" =~ @hinadt\.com$ ]]; then
                continue
            fi
            
            local github_user=""
            if [[ "$email" =~ ^[0-9]+\+([^@]+)@users\.noreply\.github\.com$ ]]; then
                github_user="${BASH_REMATCH[1]}"
            elif [[ "$email" =~ ^([^@]+)@users\.noreply\.github\.com$ ]]; then
                github_user="${BASH_REMATCH[1]}"
            else
                github_user="$author"
            fi
            
            # 检查是否为新贡献者（在这个范围内首次出现）
            if [[ ! "$unique_contributors" =~ "$github_user" ]]; then
                if [ -z "$unique_contributors" ]; then
                    unique_contributors="$github_user"
                else
                    unique_contributors="$unique_contributors,$github_user"
                fi
                
                # 获取此贡献者的第一个ISSUE提交
                local first_issue=$(echo "$contributors_info" | grep "$email" | tail -1 | cut -d'|' -f2)
                if [[ "$first_issue" =~ \(#([0-9]+)\) ]]; then
                    local pr_number="${BASH_REMATCH[1]}"
                    new_contributors="$new_contributors* @$github_user made their first contribution in #$pr_number
"
                fi
            fi
        done <<< "$contributors_info"
        
        if [ -n "$new_contributors" ]; then
            release_notes="$release_notes
## New Contributors
$new_contributors"
        fi
    fi
    
    # 获取仓库信息，支持多种URL格式
    local repo_info=$(get_repository_info)
    if [ -n "$repo_info" ] && [ -n "$last_tag" ]; then
        if [[ "$commit_range" == *".."* ]]; then
            # 标签范围比较，生成比较链接
            local prev_tag=$(echo "$commit_range" | cut -d'.' -f1)
            release_notes="$release_notes
**Full Changelog**: $repo_info/compare/$prev_tag...v$version"
        else
            # 第一个标签，生成到该标签的提交链接
            release_notes="$release_notes
**Full Changelog**: $repo_info/commits/v$version"
        fi
    elif [ -n "$repo_info" ] && [ -z "$last_tag" ]; then
        release_notes="$release_notes
**Project Repository**: $repo_info"
    fi
    
    if [ "$only_generate" = "true" ]; then
        # 仅输出到stdout，供GitHub Actions使用
        echo "$release_notes"
    elif [ "$dry_run" = "true" ]; then
        echo ""
        echo "========== 预览 Release Notes =========="
        echo "$release_notes"
        echo "========================================"
        echo ""
    else
        echo "$release_notes" > "$PROJECT_ROOT/RELEASE_NOTES.md"
        echo "Release Notes 已生成到 RELEASE_NOTES.md"
    fi
}

function main() {
    local version=""
    local dry_run="false"
    local current_only="false"
    local next_patch="false"
    local next_minor="false"
    local next_major="false"
    local only_generate="false"
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                ;;
            -c|--current)
                current_only="true"
                shift
                ;;
            --next-patch)
                next_patch="true"
                shift
                ;;
            --next-minor)
                next_minor="true"
                shift
                ;;
            --next-major)
                next_major="true"
                shift
                ;;
            --dry-run)
                dry_run="true"
                shift
                ;;
            --generate-release-notes-only)
                only_generate="true"
                shift
                ;;
            -*)
                echo "错误: 未知选项 $1"
                show_usage
                ;;
            *)
                version="$1"
                shift
                ;;
        esac
    done
    
    local current_version=$(get_current_version)
    
    if [ "$current_only" = "true" ]; then
        echo "当前版本: $current_version"
        exit 0
    fi
    
    # 计算新版本
    if [ "$next_patch" = "true" ]; then
        version=$(calculate_next_version "$current_version" "patch")
        echo "自动计算下一个patch版本: $current_version -> $version"
    elif [ "$next_minor" = "true" ]; then
        version=$(calculate_next_version "$current_version" "minor")
        echo "自动计算下一个minor版本: $current_version -> $version"
    elif [ "$next_major" = "true" ]; then
        version=$(calculate_next_version "$current_version" "major")
        echo "自动计算下一个major版本: $current_version -> $version"
    fi
    
    if [ -z "$version" ]; then
        if [ "$only_generate" != "true" ]; then
            echo "当前版本: $current_version"
            echo ""
            show_usage
        else
            exit 1
        fi
    fi
    
    if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?$ ]]; then
        echo "错误: 版本号格式不正确，应为 x.y.z 或 x.y.z-suffix (如: 2.0.0-rc.1, 2.0.0-hotfix, 2.0.0-test-auto)"
        exit 1
    fi
    
    # 仅生成Release Notes
    if [ "$only_generate" = "true" ]; then
        generate_release_notes "$version" "false" "true"
        exit 0
    fi
    
    if [ "$dry_run" = "true" ]; then
        echo "=== 预览模式 v$version ==="
        echo ""
        echo "[DRY RUN] 当前分支: $(git branch --show-current)"
        update_version "$version" "$dry_run"
        generate_release_notes "$version" "$dry_run" "false"
        echo "[DRY RUN] 提交：[RELEASE PREPARE] prepare release miaocha-$version"
        echo ""
        echo "预览完成。使用不带 --dry-run 的命令执行实际操作。"
        exit 0
    fi
    
    local current_branch=$(git branch --show-current)
    echo "在分支 $current_branch 上升级版本: $version"
    
    # 更新版本号
    update_version "$version" "$dry_run"
    
    # 生成Release Notes
    generate_release_notes "$version" "$dry_run" "false"
    
    # 提交版本更改
    git add .
    git commit -m "[RELEASE PREPARE] prepare release miaocha-$version"
    
    echo ""
    echo "版本升级完成: v$version"
    echo "下一步手动操作:"
    echo "  git tag v$version"
    echo "  git push origin v$version"
}

main "$@" 