#!/bin/bash

# Manual code formatting and quality check script
# Usage: ./scripts/format-code.sh [OPTIONS]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default options
SKIP_TESTS=false
QUICK_MODE=false
APPLY_ONLY=false
CHECK_ONLY=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --quick|-q)
            QUICK_MODE=true
            shift
            ;;
        --apply-only)
            APPLY_ONLY=true
            shift
            ;;
        --check-only)
            CHECK_ONLY=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --apply-only    Only apply code formatting, skip quality checks"
            echo "  --check-only    Only run checks, don't apply formatting"
            echo "  --quick, -q     Quick mode, skip quality checks"
            echo "  --skip-tests    Skip running tests"
            echo "  --help, -h      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                    # Full formatting and quality checks"
            echo "  $0 --apply-only      # Only apply formatting"
            echo "  $0 --check-only      # Only run checks"
            echo "  $0 --quick           # Quick formatting only"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

cd "$PROJECT_ROOT"

echo "=== Code Formatting and Quality Checks ==="
echo

if [[ "$CHECK_ONLY" == "true" ]]; then
    echo "Running code style checks only..."
    echo "1. Checking code formatting..."
    mvn spotless:check
    
    if [[ "$QUICK_MODE" == "false" ]]; then
        echo "2. Running Checkstyle..."
        mvn checkstyle:check
        
        if [[ "$SKIP_TESTS" == "false" ]]; then
            echo "3. Running tests..."
            mvn test
        fi
    fi
    
elif [[ "$APPLY_ONLY" == "true" ]]; then
    echo "Applying code formatting only..."
    echo "1. Applying Spotless formatting..."
    mvn spotless:apply
    
else
    echo "Applying formatting and running quality checks..."
    echo "1. Applying Spotless formatting..."
    mvn spotless:apply
    
    echo "2. Checking code formatting..."
    mvn spotless:check
    
    if [[ "$QUICK_MODE" == "false" ]]; then
        echo "3. Running Checkstyle..."
        mvn checkstyle:check
        
        if [[ "$SKIP_TESTS" == "false" ]]; then
            echo "4. Running tests..."
            mvn test
        fi
        
        echo "5. Running compilation check..."
        mvn clean compile
    fi
fi

echo
echo "=== Code formatting completed successfully! ==="
echo
echo "Tip: You can run 'mvn clean install -Pquick' for fast builds during development" 