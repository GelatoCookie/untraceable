# Build & Run Automation

Automated scripts to clean, build, and run the G2V2 RFID application on macOS/Linux or Windows.

## Quick Start

### macOS / Linux
```bash
./build-and-run.sh all
```

### Windows
```cmd
build-and-run.bat all
```

---

## Available Commands

| Command | Description |
|---------|-------------|
| `clean` | Remove all build artifacts and intermediate files |
| `build` | Build debug APK |
| `run` | Install and launch app on connected device/emulator |
| `all` | Clean → Build → Run (default) |
| `test` | Run unit tests |
| `release` | Build release APK (signed) |
| `help` | Show help message |

---

## Usage Examples

### macOS / Linux

```bash
# Default: clean, build, and run
./build-and-run.sh

# Clean only
./build-and-run.sh clean

# Build debug APK only
./build-and-run.sh build

# Install and run on device
./build-and-run.sh run

# Run unit tests
./build-and-run.sh test

# Build release APK
./build-and-run.sh release

# Show help
./build-and-run.sh help
```

### Windows

```cmd
REM Default: clean, build, and run
build-and-run.bat

REM Clean only
build-and-run.bat clean

REM Build debug APK only
build-and-run.bat build

REM Install and run on device
build-and-run.bat run

REM Run unit tests
build-and-run.bat test

REM Build release APK
build-and-run.bat release

REM Show help
build-and-run.bat help
```

---

## Prerequisites

### Required
- Android SDK (API 21 or higher)
- Gradle (included via gradlew)
- Java Development Kit (JDK 8 or higher)

### For Running on Device/Emulator
- Android Device or Emulator connected via USB/ADB
- ADB (Android Debug Bridge) configured
- USB debugging enabled on physical devices

### Environment Setup

#### macOS / Linux
```bash
# Ensure ANDROID_HOME is set
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
export ANDROID_HOME=$HOME/Android/Sdk          # Linux

# Add platform-tools to PATH
export PATH=$ANDROID_HOME/platform-tools:$PATH
```

#### Windows
```cmd
REM Set ANDROID_HOME (usually installed by Android Studio)
setx ANDROID_HOME "C:\Users\%USERNAME%\AppData\Local\Android\Sdk"

REM Verify ADB is accessible
adb version
```

---

## Script Output

The scripts provide colored output for easy reading:

- 🔵 **INFO** (Blue) - Informational messages
- ✓ **SUCCESS** (Green) - Operation completed successfully
- ✗ **ERROR** (Red) - Operation failed
- ⚠ **WARNING** (Yellow) - Potential issues

### Example Output
```
ℹ Cleaning build artifacts...
✓ Clean completed
ℹ Building debug APK...
✓ Debug build completed
ℹ Installing debug APK on device...
✓ Installation completed
ℹ Launching app...
✓ App launched
✓ Clean, build, and run completed!
```

---

## Build Artifacts Location

After building, you'll find:

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Build logs**: `app/build/outputs/logs/`
- **Test results**: `app/build/reports/tests/`

---

## Troubleshooting

### "gradlew not found"
- Ensure scripts are in the project root directory
- macOS/Linux: Run `chmod +x build-and-run.sh` to make it executable

### Device Not Found
```bash
# Check connected devices
adb devices

# If no devices, enable USB debugging:
# 1. Connect physical device via USB
# 2. Settings > Developer Options > USB Debugging (toggle ON)
# 3. Authorize the computer when prompted
```

### Build Fails with "Permission denied"
macOS/Linux only:
```bash
chmod +x build-and-run.sh
chmod +x gradlew
```

### Gradle Build Issues
```bash
# Clear Gradle cache
./gradlew clean

# Rebuild with verbose output
./gradlew build --info
```

### App Won't Launch
```bash
# Verify package name and activity
adb shell pm list packages | grep rfid

# Check logs
adb logcat | grep "G2V2\|rfid"
```

---

## Configuration

### Modify Build Target
Edit `app/build.gradle`:
```gradle
android {
    compileSdkVersion 31
    defaultConfig {
        applicationId "com.zebra.rfid.demo.sdksample"
        minSdkVersion 21
        targetSdkVersion 31
    }
}
```

### Signing Configuration (Release Builds)
Configure in `app/build.gradle`:
```gradle
signingConfigs {
    release {
        storeFile file("keystore.jks")
        storePassword "password"
        keyAlias "key"
        keyPassword "password"
    }
}
```

---

## Continuous Integration

Use these scripts in CI/CD pipelines:

### GitHub Actions Example
```yaml
- name: Build and Test
  run: ./build-and-run.sh test

- name: Build Release APK
  run: ./build-and-run.sh release
```

### GitLab CI Example
```yaml
build:
  script:
    - ./build-and-run.sh build
  artifacts:
    paths:
      - app/build/outputs/
```

---

## Support

For issues or questions:
1. Check the README.md for feature documentation
2. Review untraceable.md for technical details
3. Examine build logs in `app/build/outputs/logs/`
4. Run with `--info` flag for verbose output

---

## License

See LICENSE file in project root.
