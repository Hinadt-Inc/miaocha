#!/bin/bash

# =============================================================================
# 现代化测试覆盖率报告生成脚本
# 使用JaCoCo + Allure生成精美的测试覆盖率报告
# =============================================================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_header() {
    echo
    print_message $CYAN "======================================"
    print_message $CYAN "$1"
    print_message $CYAN "======================================"
    echo
}

print_success() {
    print_message $GREEN "✅ $1"
}

print_warning() {
    print_message $YELLOW "⚠️  $1"
}

print_error() {
    print_message $RED "❌ $1"
}

print_info() {
    print_message $BLUE "ℹ️  $1"
}

# 检查依赖
check_dependencies() {
    print_header "检查依赖环境"

    if ! command -v mvn &> /dev/null; then
        print_error "Maven未安装或不在PATH中"
        exit 1
    fi

    if ! command -v java &> /dev/null; then
        print_error "Java未安装或不在PATH中"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt "17" ]; then
        print_error "需要Java 17或更高版本，当前版本: $JAVA_VERSION"
        exit 1
    fi

    print_success "Maven: $(mvn -version | head -n 1)"
    print_success "Java: $(java -version 2>&1 | head -n 1)"
}

# 清理之前的构建和报告
clean_previous_builds() {
    print_header "清理之前的构建和报告"

    print_info "删除target目录..."
    mvn clean -q

    print_info "删除之前的报告目录..."
    rm -rf allure-results/
    rm -rf allure-report/
    rm -rf coverage-report/

    print_success "清理完成"
}

# 运行单元测试和集成测试
run_tests() {
    print_header "运行测试套件"

    print_info "编译项目..."
    mvn compile test-compile -q

    print_info "运行单元测试（带JaCoCo覆盖率）..."
    mvn test -q

    print_info "运行集成测试和生成覆盖率报告..."
    mvn verify -q

    print_success "所有测试执行完成"
}

# 生成聚合覆盖率报告
generate_coverage_report() {
    print_header "整理代码覆盖率报告"

    print_info "覆盖率报告已在测试阶段生成完成"

    # 检查主模块报告文件是否存在
    if [ -f "miaocha-server/target/site/jacoco/index.html" ] || [ -f "miaocha-server/target/site/jacoco-aggregate/index.html" ]; then
        print_success "JaCoCo覆盖率报告已生成"
    else
        print_info "尝试重新生成覆盖率报告..."
        mvn jacoco:report -q
    fi

    print_success "代码覆盖率报告处理完成"
}

# 使用coverage profile运行更严格的覆盖率检查
run_coverage_profile() {
    print_header "运行高标准覆盖率检查"

    print_info "使用coverage profile进行严格覆盖率检查..."
    if mvn clean test -Pcoverage -q; then
        print_success "高标准覆盖率检查通过"
    else
        print_warning "高标准覆盖率检查未完全通过，但已生成详细报告"
    fi
}

# 生成Allure报告
generate_allure_report() {
    print_header "生成Allure测试报告"

    # 检查主模块的Allure结果
    if [ -d "miaocha-server/target/allure-results" ] && [ "$(ls -A miaocha-server/target/allure-results)" ]; then
        print_info "找到Allure测试结果，生成报告..."

        # 复制所有Allure结果到根目录
        mkdir -p target/allure-results
        cp -r miaocha-server/target/allure-results/* target/allure-results/ 2>/dev/null || true

        # 生成Allure报告
        mvn allure:report -q

        if [ -d "target/site/allure-maven-plugin" ]; then
            # 复制到更友好的位置
            mkdir -p allure-report
            cp -r target/site/allure-maven-plugin/* allure-report/
            print_success "Allure报告生成完成: allure-report/index.html"
        else
            print_warning "Allure报告生成可能存在问题"
        fi
    else
        print_warning "没有找到Allure测试结果，跳过Allure报告生成"
        print_info "检查路径: miaocha-server/target/allure-results"
    fi
}

# 整理覆盖率报告
organize_coverage_reports() {
    print_header "整理覆盖率报告"

    mkdir -p coverage-report

    # 主报告：优先使用聚合报告，然后是主模块报告
    main_report_copied=false

    # 首先尝试复制聚合报告作为主报告
    if [ -d "miaocha-server/target/site/jacoco-aggregate" ]; then
        cp -r miaocha-server/target/site/jacoco-aggregate/* coverage-report/
        print_success "聚合覆盖率报告已复制到: coverage-report/index.html"
        main_report_copied=true
    elif [ -d "miaocha-server/target/site/jacoco" ]; then
        # 如果没有聚合报告，使用主模块报告
        cp -r miaocha-server/target/site/jacoco/* coverage-report/
        print_success "主模块覆盖率报告已复制到: coverage-report/index.html"
        main_report_copied=true
    fi

    # 复制聚合报告到专门的子目录
    if [ -d "miaocha-server/target/site/jacoco-aggregate" ]; then
        mkdir -p coverage-report/aggregate
        cp -r miaocha-server/target/site/jacoco-aggregate/* coverage-report/aggregate/
        print_success "聚合覆盖率报告: coverage-report/aggregate/index.html"
    fi

    # 复制各个子模块报告
    for module in miaocha-server miaocha-assembly; do
        if [ -d "$module/target/site/jacoco" ]; then
            mkdir -p "coverage-report/$module"
            cp -r "$module/target/site/jacoco"/* "coverage-report/$module/"
            print_success "模块 $module 覆盖率报告: coverage-report/$module/index.html"
        fi
    done

    if [ "$main_report_copied" = false ]; then
        print_warning "警告：未找到主覆盖率报告，请检查测试是否正确执行"
    fi
}

# 生成报告摘要
generate_summary() {
    print_header "测试和覆盖率报告摘要"

    echo "📊 报告位置："
    echo "   🔍 代码覆盖率报告: coverage-report/index.html"

    if [ -d "coverage-report/aggregate" ]; then
        echo "   📈 聚合覆盖率报告: coverage-report/aggregate/index.html"
    fi

    if [ -d "allure-report" ]; then
        echo "   📋 Allure测试报告: allure-report/index.html"
    fi

    echo
    echo "🚀 快速查看报告："

    if command -v xdg-open &> /dev/null; then
        echo "   xdg-open coverage-report/index.html"
        if [ -d "allure-report" ]; then
            echo "   xdg-open allure-report/index.html"
        fi
    elif command -v open &> /dev/null; then
        echo "   open coverage-report/index.html"
        if [ -d "allure-report" ]; then
            echo "   open allure-report/index.html"
        fi
    else
        echo "   直接用浏览器打开上述HTML文件"
    fi

    echo
    print_success "所有报告生成完成！"
}

# 启动本地HTTP服务器查看报告
start_report_server() {
    if [ "$1" = "--serve" ]; then
        print_header "启动报告服务器"

        PORT=${2:-8000}

        if command -v python3 &> /dev/null; then
            print_info "在端口 $PORT 启动HTTP服务器..."
            print_info "覆盖率报告: http://localhost:$PORT/coverage-report/"
            if [ -d "allure-report" ]; then
                print_info "Allure报告: http://localhost:$PORT/allure-report/"
            fi
            print_info "按 Ctrl+C 停止服务器"
            python3 -m http.server $PORT
        elif command -v python &> /dev/null; then
            print_info "在端口 $PORT 启动HTTP服务器..."
            print_info "覆盖率报告: http://localhost:$PORT/coverage-report/"
            if [ -d "allure-report" ]; then
                print_info "Allure报告: http://localhost:$PORT/allure-report/"
            fi
            print_info "按 Ctrl+C 停止服务器"
            python -m SimpleHTTPServer $PORT
        else
            print_warning "未找到Python，无法启动HTTP服务器"
            print_info "请手动用浏览器打开HTML文件"
        fi
    fi
}

# 只启动JaCoCo覆盖率服务器
start_jacoco_server() {
    print_header "🌐 启动JaCoCo专用覆盖率报告服务器（无Allure）"

    PORT=${1:-8000}

    if command -v python3 &> /dev/null; then
        print_info "🚀 在端口 $PORT 启动JaCoCo专用HTTP服务器..."
        print_success "🌐 JaCoCo覆盖率报告: http://localhost:$PORT/"
        if [ -d "coverage-report/aggregate" ]; then
            print_info "📈 聚合报告: http://localhost:$PORT/aggregate/"
        fi
        for module in miaocha-server miaocha-assembly; do
            if [ -d "coverage-report/$module" ]; then
                print_info "📊 模块 $module: http://localhost:$PORT/$module/"
            fi
        done
        print_info "按 Ctrl+C 停止服务器"
        echo
        cd coverage-report
        python3 -m http.server $PORT
    elif command -v python &> /dev/null; then
        print_info "🚀 在端口 $PORT 启动JaCoCo专用HTTP服务器..."
        print_success "🌐 JaCoCo覆盖率报告: http://localhost:$PORT/"
        if [ -d "coverage-report/aggregate" ]; then
            print_info "📈 聚合报告: http://localhost:$PORT/aggregate/"
        fi
        for module in miaocha-server miaocha-assembly; do
            if [ -d "coverage-report/$module" ]; then
                print_info "📊 模块 $module: http://localhost:$PORT/$module/"
            fi
        done
        print_info "按 Ctrl+C 停止服务器"
        echo
        cd coverage-report
        python -m SimpleHTTPServer $PORT
    else
        print_warning "未找到Python，无法启动HTTP服务器"
        print_info "请手动用浏览器打开 coverage-report/index.html"
    fi
}

# 显示使用帮助
show_help() {
    echo "现代化测试覆盖率报告生成脚本"
    echo
    echo "用法: $0 [选项]"
    echo
    echo "选项:"
    echo "  --help              显示此帮助信息"
    echo "  --clean-only        仅清理之前的构建"
    echo "  --test-only         仅运行测试"
    echo "  --coverage-only     仅生成覆盖率报告"
    echo "  --jacoco-only       仅生成JaCoCo覆盖率报告（不生成Allure）"
    echo "  --jacoco-serve [端口] 只生成JaCoCo报告并启动HTTP服务器 (默认端口: 8000)"
    echo "  --strict            使用严格的覆盖率标准"
    echo "  --serve [端口]      生成报告后启动HTTP服务器 (默认端口: 8000)"
    echo "  --skip-integration  跳过集成测试"
    echo
    echo "示例:"
    echo "  $0                      # 运行完整的测试和报告生成"
    echo "  $0 --strict             # 使用严格覆盖率标准"
    echo "  $0 --serve 9000         # 生成报告并在9000端口启动服务器"
    echo "  $0 --jacoco-only        # 只生成JaCoCo覆盖率报告"
    echo "  $0 --jacoco-serve 8080  # 只生成JaCoCo报告并在8080端口启动服务器"
}

# 主执行逻辑
main() {
    local clean_only=false
    local test_only=false
    local coverage_only=false
    local jacoco_only=false
    local jacoco_serve_mode=false
    local strict_mode=false
    local serve_mode=false
    local serve_port=8000
    local skip_integration=false

    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --help|-h)
                show_help
                exit 0
                ;;
            --clean-only)
                clean_only=true
                shift
                ;;
            --test-only)
                test_only=true
                shift
                ;;
            --coverage-only)
                coverage_only=true
                shift
                ;;
            --jacoco-only)
                jacoco_only=true
                shift
                ;;
            --jacoco-serve)
                jacoco_only=true
                jacoco_serve_mode=true
                if [[ $2 =~ ^[0-9]+$ ]]; then
                    serve_port=$2
                    shift
                fi
                shift
                ;;
            --strict)
                strict_mode=true
                shift
                ;;
            --serve)
                serve_mode=true
                if [[ $2 =~ ^[0-9]+$ ]]; then
                    serve_port=$2
                    shift
                fi
                shift
                ;;
            --skip-integration)
                skip_integration=true
                shift
                ;;
            *)
                print_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 检查依赖
    check_dependencies

    # 执行相应的操作
    if [ "$clean_only" = true ]; then
        clean_previous_builds
        return
    fi

    if [ "$coverage_only" = false ]; then
        clean_previous_builds
    fi

    if [ "$test_only" = true ] || [ "$coverage_only" = false ]; then
        if [ "$jacoco_only" = true ]; then
            # JaCoCo专用模式：只运行测试和生成覆盖率，不生成Allure
            print_header "🎯 JaCoCo专用模式 - 仅生成覆盖率报告（跳过Allure）"
            print_info "⚡ 编译项目..."
            mvn compile test-compile -q
            print_info "🧪 运行单元测试（带JaCoCo覆盖率）..."
            mvn test jacoco:report -q
            if [ "$skip_integration" = false ]; then
                print_info "🔗 运行集成测试..."
                mvn verify -q
            fi
            print_success "✅ JaCoCo专用测试执行完成 - 未生成Allure报告"
        elif [ "$skip_integration" = true ]; then
            print_info "跳过集成测试"
            mvn test jacoco:report -q
        else
            run_tests
        fi
    fi

    if [ "$coverage_only" = true ] || [ "$test_only" = false ]; then
        generate_coverage_report

        if [ "$strict_mode" = true ]; then
            run_coverage_profile
        fi

        if [ "$jacoco_only" = false ]; then
            generate_allure_report
        fi
        
        organize_coverage_reports
        
        if [ "$jacoco_only" = false ]; then
            generate_summary
        else
            # JaCoCo专用摘要
            print_header "🎯 JaCoCo专用覆盖率报告摘要（无Allure）"
            echo "📊 JaCoCo覆盖率报告位置："
            echo "   🔍 主报告: coverage-report/index.html"
            if [ -d "coverage-report/aggregate" ]; then
                echo "   📈 聚合报告: coverage-report/aggregate/index.html"
            fi
            for module in miaocha-server miaocha-assembly; do
                if [ -d "coverage-report/$module" ]; then
                    echo "   📊 模块 $module: coverage-report/$module/index.html"
                fi
            done
            echo
            print_success "✅ JaCoCo专用覆盖率报告生成完成！（已跳过Allure测试报告）"
        fi
    fi

    if [ "$serve_mode" = true ]; then
        start_report_server --serve $serve_port
    elif [ "$jacoco_serve_mode" = true ]; then
        start_jacoco_server $serve_port
    fi
}

# 执行主函数
main "$@"
