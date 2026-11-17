#!/bin/bash

# Theme Switching Test Script for Mindspace Android App
# This script automates the installation and provides test verification steps

echo "=========================================="
echo "Mindspace Theme Switching Test"
echo "=========================================="
echo ""

# Check if Android SDK is available
if ! command -v adb &> /dev/null; then
    echo "Error: Android SDK not found. Please install Android SDK and add to PATH."
    exit 1
fi

# Check if devices are available
DEVICES=$(adb devices | tail -n +2 | grep -v "offline" | awk '{print $1}')
if [ -z "$DEVICES" ]; then
    echo "Warning: No Android devices or emulators detected."
    echo "Please connect a device or start an emulator first."
    echo ""
fi

echo "Available devices:"
echo "$DEVICES"
echo ""

# Build and install the app
echo "Building and installing the app..."
cd /home/admin/mindspace
./gradlew installDebug

if [ $? -ne 0 ]; then
    echo "Error: Build failed. Please check the build output above."
    exit 1
fi

echo ""
echo "=========================================="
echo "Installation Complete!"
echo "=========================================="
echo ""
echo "Now you need to manually test the theme switching functionality:"
echo ""

cat << "EOF"
TEST STEPS:

1. Launch the Mindspace app on your device/emulator

2. VERIFY DEFAULT THEME:
   - The app should launch with the LocalChat theme (orange color scheme)
   - Check that all UI elements have orange accents
   - Toolbar should be orange
   - Send button should have orange accent

3. TEST THEME SWITCHING:
   a) Switch to Forest Theme:
      - Tap the Settings button (⚙️) in the bottom toolbar
      - In the Settings dialog, find the Theme dropdown spinner
      - Select "Forest" from the dropdown
      - Click "Save"
      - Verify: The app should recreate with green color scheme
      - Check: Toolbar is green, send button has green accent

   b) Switch to Midnight Theme:
      - Open Settings again
      - Select "Midnight" from the theme dropdown
      - Click "Save"
      - Verify: The app should recreate with blue color scheme
      - Check: Toolbar is blue, send button has blue accent

   c) Switch to Nordic Theme:
      - Open Settings again
      - Select "Nordic" from the theme dropdown
      - Click "Save"
      - Verify: The app should recreate with blue/green color scheme
      - Check: Toolbar is blue, send button has blue accent with green secondary elements

   d) Return to LocalChat Theme:
      - Open Settings again
      - Select "LocalChat" from the theme dropdown
      - Click "Save"
      - Verify: The app should recreate with orange color scheme
      - Check: Toolbar is orange, send button has orange accent

4. TEST THEME PERSISTENCE:
   - Close the app completely
   - Reopen the app
   - Verify: The last selected theme should be applied automatically
   - Open Settings to verify the correct theme is still selected in the dropdown

5. TEST THEME SWITCHING DURING CONVERSATION:
   - Start a new chat conversation
   - Send a message to the AI
   - Switch to a different theme (e.g., from LocalChat to Forest)
   - Verify:
     * The chat messages should remain visible
     * All UI elements should update to the new theme colors
     * The conversation state should be preserved

6. TEST THEME SWITCHING WITH ATTACHMENTS:
   - Start a new chat
   - Attach an image to a message
   - Send the message with the attachment
   - Switch to a different theme
   - Verify: The attachment preview and all UI elements update correctly

TROUBLESHOOTING:

If theme doesn't change:
- Click "Save" (not Cancel) after selecting a theme
- Verify the theme spinner shows the selected theme
- Try switching to a different theme and back
- Restart the app and try again

If app crashes on theme change:
- Check logcat: adb logcat | grep mindspace
- Verify all theme colors are defined in colors.xml
- Ensure theme styles are properly defined in themes.xml

If theme reverts after app restart:
- Verify ThemeUtils.saveScheme() is being called with correct scheme key
- Check that ThemeUtils.loadScheme() returns expected value

EOF

echo ""
echo "=========================================="
echo "Test Verification Commands:"
echo "=========================================="
echo ""
echo "To check if app is installed:"
echo "  adb shell pm list packages | grep mindspace"
echo ""
echo "To view app logs:"
echo "  adb logcat | grep mindspace"
echo ""
echo "To uninstall the app:"
echo "  adb uninstall com.mindspace.app"
echo ""
