#!/bin/bash

# 秒查系统发版版本管理脚本
# 用于自动化版本更新和发版准备

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 帮助信息
show_help() {
    echo "秒查系统发版版本管理脚本"
    echo ""
    echo "用法: $0 [选项] <新版本号>"
    echo ""
    echo "选项:"
    echo "  -h, --help              显示此帮助信息"
    echo "  -c, --current           显示当前版本"
    echo "  -n, --next-patch        自动计算下一个补丁版本"
    echo "  -m, --next-minor        自动计算下一个次版本"
    echo "  -M, --next-major        自动计算下一个主版本"
    echo "  -p, --prepare-only      仅准备版本更新，不执行git操作"
    echo "  -t, --tag-only          仅创建git标签，不推送"
    echo "  --dry-run               预览模式，不实际执行更改"
    echo ""
    echo "示例:"
    echo "  $0 2.1.0                     更新到版本2.1.0"
    echo "  $0 --next-patch              自动升级到下一个补丁版本"
    echo "  $0 --current                 显示当前版本"
    echo "  $0 --dry-run 2.1.0           预览更新到2.1.0的操作"
    echo ""
}

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 验证版本号格式
validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?$ ]]; then
        log_error "无效的版本号格式: $version"
        log_error "版本号应符合语义化版本规范，如: 2.1.0 或 2.1.0-SNAPSHOT"
        exit 1
    fi
}

# 获取当前版本
get_current_version() {
    if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
        log_error "未找到pom.xml文件"
        exit 1
    fi
    
    local version=$(grep -m 1 '<version>' "$PROJECT_ROOT/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d ' \t\n')
    echo "$version"
}

# 计算下一个版本
calculate_next_version() {
    local current_version=$1
    local version_type=$2
    
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
            log_error "未知的版本类型: $version_type"
            exit 1
            ;;
    esac
}

# 更新Maven项目版本
update_maven_version() {
    local new_version=$1
    local dry_run=$2
    
    log_info "更新Maven项目版本到 $new_version"
    
    if [ "$dry_run" = "true" ]; then
        log_warning "[DRY RUN] 将执行: mvn versions:set -DnewVersion=$new_version -DgenerateBackupPoms=false"
        return 0
    fi
    
    # 更新版本
    mvn versions:set -DnewVersion="$new_version" -DgenerateBackupPoms=false -q
    
    if [ $? -eq 0 ]; then
        log_success "Maven版本更新成功"
    else
        log_error "Maven版本更新失败"
        exit 1
    fi
}

# 更新子模块版本
update_submodules_version() {
    local new_version=$1
    local dry_run=$2
    
    log_info "更新子模块版本"
    
    if [ "$dry_run" = "true" ]; then
        log_warning "[DRY RUN] 将更新所有子模块版本"
        return 0
    fi
    
    # 更新父版本引用
    mvn versions:update-child-modules -q
    
    log_success "子模块版本更新成功"
}

# 验证构建
validate_build() {
    local dry_run=$1
    
    log_info "验证项目构建"
    
    if [ "$dry_run" = "true" ]; then
        log_warning "[DRY RUN] 将执行: mvn clean compile test"
        return 0
    fi
    
    mvn clean compile test -q
    
    if [ $? -eq 0 ]; then
        log_success "项目构建验证成功"
    else
        log_error "项目构建验证失败"
        exit 1
    fi
}

# 生成RocketMQ风格更新日志
generate_changelog() {
    local version=$1
    local dry_run=$2
    
    log_info "生成RocketMQ风格的版本 $version 更新日志"
    
    if [ "$dry_run" = "true" ]; then
        log_warning "[DRY RUN] 将生成RocketMQ风格更新日志"
        return 0
    fi
    
    # 获取最新的tag
    local last_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    local changelog_file="CHANGELOG-$version.md"
    local release_notes_file="RELEASE-NOTES-$version.md"
    
    # 生成GitHub Release Notes格式
    generate_github_release_notes "$version" "$last_tag" "$release_notes_file"
    
    # 生成传统changelog
    echo "# 版本 $version 更新日志" > "$changelog_file"
    echo "" >> "$changelog_file"
    echo "发布日期: $(date '+%Y-%m-%d')" >> "$changelog_file"
    echo "" >> "$changelog_file"
    
    if [ -n "$last_tag" ]; then
        echo "## 🎯 本版本概述" >> "$changelog_file"
        echo "" >> "$changelog_file"
        echo "本版本包含以下主要改进和错误修复。" >> "$changelog_file"
        echo "" >> "$changelog_file"
        
        # 统计变更
        local total_commits=$(git rev-list --count "$last_tag..HEAD" 2>/dev/null || echo "0")
        local issue_count=$(git log --oneline "$last_tag..HEAD" | grep -c "\[ISSUE" || echo "0")
        echo "**统计信息**: $total_commits 次提交, $issue_count 个ISSUE修复" >> "$changelog_file"
        echo "" >> "$changelog_file"
        
        # 按类型分类变更
        echo "## 🚀 新功能 & 优化" >> "$changelog_file"
        git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" | \
            grep -E "\[ISSUE.*\].*(新增|feat|feature|完善|优化|enhancement|支持)" >> "$changelog_file" || echo "* 无新功能" >> "$changelog_file"
        echo "" >> "$changelog_file"
        
        echo "## 🐛 错误修复" >> "$changelog_file"
        git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" | \
            grep -E "\[ISSUE.*\].*(修复|fix|bug|解决)" >> "$changelog_file" || echo "* 无错误修复" >> "$changelog_file"
        echo "" >> "$changelog_file"
        
        echo "## 📚 文档 & 其他" >> "$changelog_file"
        git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" | \
            grep -E "\[ISSUE.*\].*(文档|doc|补充|更新|chore)" >> "$changelog_file" || echo "* 无文档更新" >> "$changelog_file"
        echo "" >> "$changelog_file"
        
        echo "## 📝 所有变更" >> "$changelog_file"
        echo "" >> "$changelog_file"
        git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" >> "$changelog_file"
        echo "" >> "$changelog_file"
        
        echo "## 👥 贡献者" >> "$changelog_file"
        git log --pretty=format:"%an" "$last_tag..HEAD" | sort | uniq | \
            sed 's/^/* @/' >> "$changelog_file"
        echo "" >> "$changelog_file"
        
        echo "**完整变更日志**: https://github.com/your-org/miaocha/compare/$last_tag...v$version" >> "$changelog_file"
        
    else
        echo "## 初始版本" >> "$changelog_file"
        echo "" >> "$changelog_file"
        echo "- 🎉 秒查系统初始发布" >> "$changelog_file"
        echo "- ✨ 基础功能实现完成" >> "$changelog_file"
    fi
    
    log_success "RocketMQ风格更新日志已生成: $changelog_file"
    log_success "GitHub Release Notes已生成: $release_notes_file"
}

# 生成GitHub Release Notes格式
generate_github_release_notes() {
    local version=$1
    local last_tag=$2
    local output_file=$3
    
    {
        echo "## What's Changed"
        echo ""
        echo "This version includes several improvements and bug fixes based on community feedback."
        echo ""
        
        if [ -n "$last_tag" ]; then
            # 获取所有merge commits (通常包含[ISSUE #xx]格式)
            local merge_commits=$(git log --merges --oneline --pretty=format:"* %s" "$last_tag..HEAD" | grep -E "\[ISSUE.*\]" || echo "")
            local all_issue_commits=$(git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" | grep -E "\[ISSUE.*\]" || echo "")
            
            # 优先使用merge commits，如果没有则使用所有[ISSUE #xx]格式的提交
            local issue_commits=""
            if [ -n "$merge_commits" ]; then
                issue_commits="$merge_commits"
                echo "<!-- 基于merge commits生成 -->" >> "$output_file"
            else
                issue_commits="$all_issue_commits"
                echo "<!-- 基于[ISSUE #xx]提交生成 -->" >> "$output_file"
            fi
            
            # 直接列出所有变更，不分类（真实RocketMQ格式）
            if [ -n "$issue_commits" ]; then
                echo "$issue_commits"
                echo ""
            fi
            
            # 如果还有其他格式的提交，也列出来
            local other_commits=$(git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" | grep -v -E "\[ISSUE.*\]" | head -10)
            if [ -n "$other_commits" ]; then
                echo "$other_commits"
                echo ""
            fi
            
            echo "### 📝 All Changes"
            git log --oneline --pretty=format:"* %s" "$last_tag..HEAD" | head -50
            echo ""
            
            echo "### 👥 New Contributors"
            local contributors=$(git log --pretty=format:"%an" "$last_tag..HEAD" | sort | uniq)
            if [ -n "$contributors" ]; then
                echo "$contributors" | sed 's/^/* @/'
            else
                echo "* No new contributors in this release"
            fi
            echo ""
            
            echo "**Full Changelog**: https://github.com/your-org/miaocha/compare/$last_tag...v$version"
        else
            echo "### 🎉 Initial Release"
            echo ""
            echo "* Initial release of 秒查系统 (MiaoCha System)"
            echo "* Core functionality implementation completed"
            echo ""
            echo "### 👥 Contributors"
            git log --pretty=format:"%an" | sort | uniq | sed 's/^/* @/'
        fi
        
    } > "$output_file"
}

# 创建Git提交和标签
create_git_commit_and_tag() {
    local version=$1
    local dry_run=$2
    local tag_only=$3
    
    if [ "$tag_only" = "true" ]; then
        log_info "仅创建Git标签"
        if [ "$dry_run" = "true" ]; then
            log_warning "[DRY RUN] 将创建标签: v$version"
        else
            git tag -a "v$version" -m "Release version $version"
            log_success "已创建标签: v$version"
        fi
        return 0
    fi
    
    log_info "创建Git提交和标签"
    
    if [ "$dry_run" = "true" ]; then
        log_warning "[DRY RUN] 将执行以下操作:"
        log_warning "  - git add ."
        log_warning "  - git commit -m \"Release version $version\""
        log_warning "  - git tag -a \"v$version\" -m \"Release version $version\""
        return 0
    fi
    
    # 添加所有更改
    git add .
    
    # 检查是否有更改
    if git diff --cached --quiet; then
        log_warning "没有检测到需要提交的更改"
    else
        # 创建提交
        git commit -m "Release version $version"
        log_success "已创建提交: Release version $version"
    fi
    
    # 创建标签
    git tag -a "v$version" -m "Release version $version"
    log_success "已创建标签: v$version"
}

# 推送到远程仓库
push_to_remote() {
    local version=$1
    local dry_run=$2
    
    log_info "推送到远程仓库"
    
    if [ "$dry_run" = "true" ]; then
        log_warning "[DRY RUN] 将执行:"
        log_warning "  - git push origin HEAD"
        log_warning "  - git push origin v$version"
        return 0
    fi
    
    # 推送分支
    git push origin HEAD
    
    # 推送标签
    git push origin "v$version"
    
    log_success "已推送到远程仓库"
}

# 主函数
main() {
    local new_version=""
    local current_only=false
    local next_patch=false
    local next_minor=false
    local next_major=false
    local prepare_only=false
    local tag_only=false
    local dry_run=false
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -c|--current)
                current_only=true
                shift
                ;;
            -n|--next-patch)
                next_patch=true
                shift
                ;;
            -m|--next-minor)
                next_minor=true
                shift
                ;;
            -M|--next-major)
                next_major=true
                shift
                ;;
            -p|--prepare-only)
                prepare_only=true
                shift
                ;;
            -t|--tag-only)
                tag_only=true
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            -*)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
            *)
                new_version="$1"
                shift
                ;;
        esac
    done
    
    # 切换到项目根目录
    cd "$PROJECT_ROOT"
    
    # 获取当前版本
    local current_version=$(get_current_version)
    
    if [ "$current_only" = "true" ]; then
        echo "当前版本: $current_version"
        exit 0
    fi
    
    # 计算新版本
    if [ "$next_patch" = "true" ]; then
        new_version=$(calculate_next_version "$current_version" "patch")
    elif [ "$next_minor" = "true" ]; then
        new_version=$(calculate_next_version "$current_version" "minor")
    elif [ "$next_major" = "true" ]; then
        new_version=$(calculate_next_version "$current_version" "major")
    fi
    
    # 验证是否提供了版本号
    if [ -z "$new_version" ]; then
        log_error "请提供新版本号或使用自动版本计算选项"
        show_help
        exit 1
    fi
    
    # 验证版本号格式
    validate_version "$new_version"
    
    # 显示版本信息
    log_info "版本更新信息:"
    log_info "  当前版本: $current_version"
    log_info "  新版本:   $new_version"
    
    if [ "$dry_run" = "true" ]; then
        log_warning "预览模式 - 不会执行实际更改"
    fi
    
    # 确认操作
    if [ "$dry_run" = "false" ]; then
        echo ""
        read -p "确认执行版本更新? (y/N): " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "操作已取消"
            exit 0
        fi
    fi
    
    echo ""
    log_info "开始版本更新流程..."
    
    # 执行版本更新步骤
    if [ "$tag_only" = "false" ]; then
        update_maven_version "$new_version" "$dry_run"
        update_submodules_version "$new_version" "$dry_run"
        validate_build "$dry_run"
        generate_changelog "$new_version" "$dry_run"
    fi
    
    if [ "$prepare_only" = "false" ]; then
        create_git_commit_and_tag "$new_version" "$dry_run" "$tag_only"
        
        if [ "$dry_run" = "false" ]; then
            echo ""
            read -p "是否推送到远程仓库? (y/N): " -n 1 -r
            echo ""
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                push_to_remote "$new_version" "$dry_run"
            else
                log_info "跳过推送到远程仓库"
                log_info "手动推送命令:"
                log_info "  git push origin HEAD"
                log_info "  git push origin v$new_version"
            fi
        fi
    fi
    
    echo ""
    log_success "版本更新流程完成!"
    log_info "新版本: $new_version"
    
    if [ "$prepare_only" = "true" ]; then
        log_info "仅完成了版本准备，请手动执行git操作"
    elif [ "$tag_only" = "true" ]; then
        log_info "仅创建了标签，请手动推送: git push origin v$new_version"
    fi
    
    echo ""
    log_info "下一步操作:"
    log_info "1. 在GitHub上创建Release (https://github.com/用户名/仓库名/releases/new)"
    log_info "2. 上传构建资产"
    log_info "3. 发布Release Notes"
}

# 执行主函数
main "$@" 