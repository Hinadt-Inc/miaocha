#!/bin/bash

# Install Git pre-commit hooks for code formatting
# This script should be run once after cloning the repository

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Installing Git pre-commit hooks..."

# Create the pre-commit hook
cat > "$PROJECT_ROOT/.git/hooks/pre-commit" << 'EOF'
#!/bin/bash

echo "Running pre-commit code formatting..."

# Check if we're in a Maven project
if [[ ! -f "pom.xml" ]]; then
    echo "Error: pom.xml not found. Make sure you're in the project root directory."
    exit 1
fi

# Get list of staged Java files
STAGED_JAVA_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(java)$' || true)

if [[ -n "$STAGED_JAVA_FILES" ]]; then
    echo "Formatting Java files..."
    
    # Apply Spotless formatting
    mvn spotless:apply -q
    
    # Re-stage the formatted files
    for file in $STAGED_JAVA_FILES; do
        if [[ -f "$file" ]]; then
            git add "$file"
        fi
    done
    
    echo "Code formatting applied to staged Java files."
else
    echo "No Java files to format."
fi

# Quick compilation check
echo "Running quick compilation check..."
mvn clean compile -q

if [[ $? -ne 0 ]]; then
    echo "Error: Code compilation failed. Please fix the issues before committing."
    exit 1
fi

echo "Pre-commit checks passed!"
EOF

# Make the hook executable
chmod +x "$PROJECT_ROOT/.git/hooks/pre-commit"

echo "Git pre-commit hook installed successfully!"
echo ""
echo "The hook will now automatically:"
echo "  1. Format Java code using Spotless"
echo "  2. Re-stage formatted files"
echo "  3. Run a quick compilation check"
echo ""
echo "To bypass the hook (not recommended), use: git commit --no-verify" 