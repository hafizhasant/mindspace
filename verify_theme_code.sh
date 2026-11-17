#!/bin/bash
# Theme Switching Verification Script
# This script checks the theme switching code for potential issues

echo "=== Theme Switching Code Verification ==="
echo ""

# Check if ThemeUtils.java exists
if [ -f "app/src/main/java/com/mindspace/app/ThemeUtils.java" ]; then
    echo "✓ ThemeUtils.java exists"
else
    echo "✗ ThemeUtils.java not found"
    exit 1
fi

# Check if themes.xml exists
if [ -f "app/src/main/res/values/themes.xml" ]; then
    echo "✓ themes.xml exists"
else
    echo "✗ themes.xml not found"
    exit 1
fi

# Check if colors.xml exists
if [ -f "app/src/main/res/values/colors.xml" ]; then
    echo "✓ colors.xml exists"
else
    echo "✗ colors.xml not found"
    exit 1
fi

echo ""
echo "=== Checking Theme Definitions ==="

# Check for theme definitions in themes.xml
if grep -q "Theme.LocalChat" app/src/main/res/values/themes.xml; then
    echo "✓ LocalChat theme defined"
else
    echo "✗ LocalChat theme not found"
fi

if grep -q "Theme.Forest" app/src/main/res/values/themes.xml; then
    echo "✓ Forest theme defined"
else
    echo "✗ Forest theme not found"
fi

if grep -q "Theme.Midnight" app/src/main/res/values/themes.xml; then
    echo "✓ Midnight theme defined"
else
    echo "✗ Midnight theme not found"
fi

if grep -q "Theme.Nordic" app/src/main/res/values/themes.xml; then
    echo "✓ Nordic theme defined"
else
    echo "✗ Nordic theme not found"
fi

echo ""
echo "=== Checking Color Definitions ==="

# Check for color definitions
if grep -q "forest_primary" app/src/main/res/values/colors.xml; then
    echo "✓ Forest primary color defined"
else
    echo "✗ Forest primary color not found"
fi

if grep -q "midnight_primary" app/src/main/res/values/colors.xml; then
    echo "✓ Midnight primary color defined"
else
    echo "✗ Midnight primary color not found"
fi

if grep -q "nordic_primary" app/src/main/res/values/colors.xml; then
    echo "✓ Nordic primary color defined"
else
    echo "✗ Nordic primary color not found"
fi

echo ""
echo "=== Checking MainActivity Theme Logic ==="

# Check if MainActivity loads theme on create
if grep -q "loadScheme" app/src/main/java/com/mindspace/app/MainActivity.java; then
    echo "✓ MainActivity loads theme on create"
else
    echo "✗ MainActivity doesn't load theme"
fi

# Check if MainActivity applies theme before setContentView
if grep -q "setTheme(themeId);" app/src/main/java/com/mindspace/app/MainActivity.java; then
    echo "✓ MainActivity applies theme before setContentView"
else
    echo "✗ MainActivity doesn't apply theme properly"
fi

# Check if MainActivity calls recreate()
if grep -q "recreate();" app/src/main/java/com/mindspace/app/MainActivity.java; then
    echo "✓ MainActivity calls recreate() for theme changes"
else
    echo "✗ MainActivity doesn't call recreate()"
fi

echo ""
echo "=== Checking Settings Dialog ==="

# Check if settings dialog has theme spinner
if grep -q "android:id=\"@+id/scheme\"" app/src/main/res/layout/dialog_settings.xml; then
    echo "✓ Settings dialog has theme spinner"
else
    echo "✗ Settings dialog missing theme spinner"
fi

echo ""
echo "=== Summary ==="
echo "All checks completed. Review the output above for any issues."
echo ""
echo "Next steps for manual testing:"
echo "1. Build and install the app: ./gradlew installDebug"
echo "2. Launch the app and test theme switching in Settings"
echo "3. Verify themes persist across app restarts"
