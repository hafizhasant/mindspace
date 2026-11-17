# Theme Switching Testing Guide

## How to Test Theme Switching

### Prerequisites
1. Android device or emulator with Mindspace app installed
2. USB debugging enabled (if testing on physical device)

### Test Steps

#### 1. Install the App
```bash
cd /home/admin/mindspace
./gradlew installDebug
```

#### 2. Launch the App
- Open the Mindspace app on your device/emulator
- Verify it launches with the default LocalChat theme

#### 3. Test Theme Switching
1. Tap the **Settings** button (⚙️) in the bottom toolbar
2. In the Settings dialog, you should see a **Theme** dropdown spinner
3. Test each theme option:
   - **LocalChat** (default) - Orange/red color scheme
   - **Forest** - Green color scheme
   - **Midnight** - Blue color scheme
   - **Nordic** - Blue/green color scheme

4. For each theme:
   - Select the theme from the dropdown
   - Click **Save**
   - Verify the app recreates with the new theme colors
   - Check that all UI elements (buttons, text, backgrounds) update correctly
   - Navigate between screens to ensure theme is consistent

#### 4. Verify Theme Persistence
1. Close and reopen the app
2. Verify the selected theme is applied on launch
3. Check that the correct theme is still selected in Settings

#### 5. Test Theme Switching During Conversation
1. Start a chat conversation
2. Switch themes mid-conversation
3. Verify messages, input field, and all UI elements update correctly

### Expected Behavior

- Theme changes should be **instantaneous** with no app restart required
- All UI elements should update to match the selected theme colors
- The app should maintain its state (current chat, messages) after theme change
- The selected theme should persist across app launches

### Known Themes

| Theme Name | Primary Color | Secondary Color |
|------------|---------------|-----------------|
| LocalChat | #D97757 (Orange) | #D97757 (Orange) |
| Forest | #4A7C59 (Green) | #6B9E72 (Lighter Green) |
| Midnight | #3D5A80 (Blue) | #97C2CC (Cyan) |
| Nordic | #2C5F8D (Blue) | #A0D468 (Green) |

### Troubleshooting

**Issue: Theme doesn't change**
- Check that you clicked **Save**, not Cancel
- Verify the theme spinner shows the selected theme
- Try switching to a different theme and back

**Issue: App crashes on theme change**
- Check logcat for errors: `adb logcat | grep mindspace`
- Verify that all theme colors are defined in `colors.xml`

**Issue: Theme reverts after app restart**
- Check that the theme is being saved to SharedPreferences correctly
- Verify the `loadScheme()` method returns the correct value
