#!/bin/bash
# ==============================================================================
# SyntricDB Version Bump & Git Release Tagging Helper
# Usage: ./deploy/bump_version.sh 1.1.0
# ==============================================================================
set -e

NEW_VERSION="$1"

if [ -z "$NEW_VERSION" ]; then
    echo "❌ Error: Version number required."
    echo "Usage: ./deploy/bump_version.sh <version> (e.g., ./deploy/bump_version.sh 1.1.0)"
    exit 1
fi

TAG_NAME="v$NEW_VERSION"

echo "=========================================================================="
echo "🔖 Bumping SyntricDB Version to $NEW_VERSION (Tag: $TAG_NAME)"
echo "=========================================================================="

# 1. Update Maven pom.xml version
echo "📝 Updating pom.xml project version..."
mvn versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false

# 2. Update deploy/package_releases.sh VERSION variable
if [ -f "deploy/package_releases.sh" ]; then
    echo "📝 Updating deploy/package_releases.sh version variable..."
    sed -i '' "s/VERSION=\".*\"/VERSION=\"$NEW_VERSION\"/" deploy/package_releases.sh 2>/dev/null || sed -i "s/VERSION=\".*\"/VERSION=\"$NEW_VERSION\"/" deploy/package_releases.sh
fi

echo "✅ Version updated in files."

# 3. Stage changes
git add pom.xml deploy/package_releases.sh

# 4. Commit version update
git commit -m "chore(release): bump version to $NEW_VERSION" || echo "No changes to commit."

# 5. Create annotated Git Tag
echo "🏷️ Creating Git Tag $TAG_NAME..."
git tag -a "$TAG_NAME" -m "SyntricDB Release $TAG_NAME"

echo ""
echo "=========================================================================="
echo "🎉 Version bumped to $NEW_VERSION and tagged as $TAG_NAME!"
echo "=========================================================================="
echo "🚀 To trigger the automated GitHub Release & Docker container build, run:"
echo "   git push origin main --tags"
echo "=========================================================================="
