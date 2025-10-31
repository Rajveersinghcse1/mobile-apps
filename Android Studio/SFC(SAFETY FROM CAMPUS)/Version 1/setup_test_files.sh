#!/bin/bash
# Quick Setup Script for SFC App Testing
# This script helps you quickly push test files to your Android device

echo "═══════════════════════════════════════════════════════"
echo "  SFC APP - Quick Test Files Setup"
echo "═══════════════════════════════════════════════════════"
echo ""

# Device check
echo "🔍 Checking for connected Android device..."
adb devices

echo ""
echo "📱 Device detected. Setting up test folders..."
echo ""

# Define paths
APP_PATH="/sdcard/Android/data/com.example.imageanalysis/files/SFC"
IMAGE_PATH="$APP_PATH/ReferenceImages"
DATA_PATH="$APP_PATH/ReferenceData"

# Create directories (in case app not launched yet)
echo "📁 Creating directories on device..."
adb shell mkdir -p "$IMAGE_PATH"
adb shell mkdir -p "$DATA_PATH"

echo "✅ Directories created!"
echo ""

# Check if sample_data folder exists locally
if [ -d "sample_data" ]; then
    echo "📤 Pushing sample JSON files to device..."
    
    # Push all JSON files
    for file in sample_data/*.json; do
        if [ -f "$file" ]; then
            filename=$(basename "$file")
            echo "  → Uploading $filename"
            adb push "$file" "$DATA_PATH/"
        fi
    done
    
    echo "✅ Sample data uploaded!"
else
    echo "⚠️  sample_data folder not found in current directory"
    echo "   Please run this script from the project root folder"
fi

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  📋 SETUP COMPLETE!"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Next steps:"
echo "  1. Add test images to: $IMAGE_PATH"
echo "  2. Launch the SFC app on your device"
echo "  3. Login with: Test@example.com / test123"
echo "  4. Check Settings to verify paths"
echo "  5. Test image matching feature"
echo ""
echo "To view files on device:"
echo "  adb shell ls $IMAGE_PATH"
echo "  adb shell ls $DATA_PATH"
echo ""
echo "To manually push an image:"
echo "  adb push yourimage.jpg $IMAGE_PATH/"
echo ""
echo "═══════════════════════════════════════════════════════"
