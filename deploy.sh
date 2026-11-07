#!/bin/bash

# NoteLedger Deployment Script
# Tasks: Commit changes, push to GitHub, and trigger CI/CD pipeline

echo "🚀 Starting NoteLedger deployment process..."

# 1. Add all changed files
git add .

# 2. Create structured commit message
COMMIT_MSG="feat: complete AdMob removal, implement Admin login with member count, and integrate Google Play Billing. Added Privacy Policy and developer metadata."

# 3. Commit
echo "📝 Committing changes..."
git commit -m "$COMMIT_MSG"

# 4. Push to main branch
echo "📤 Pushing to GitHub (origin main)..."
git push origin main

echo "✅ Deployment triggered! Check GitHub Actions for Release APK and AAB build progress."
