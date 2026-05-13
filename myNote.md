# Project Notes — G2V2 Untraceable Feature

## History

### v1.0.0 (Current)
- ✅ Implemented `performUntraceable()` for Ucode 9xe tags
  - Hide 2 words of EPC (from 6 words)
  - Hide all TID (HIDE_ALL_TID)
  - Access filter targeting tags with EPC pattern `33333333`
  - Password authentication: `0x00000001`

- ✅ Implemented `restorePublicAccess()` for Ucode 9xe tags
  - Restore 6 words of EPC (full length)
  - Restore all TID (SHOW_TID)
  - Same access filter and password authentication

- ✅ Created comprehensive documentation
  - `untraceable.md`: Technical reference with code examples and memory bank layout
  - `README.md`: Test results and feature documentation

- ✅ Testing completed and passed on Ucode 9xe tags

---

## To Do

### Phase 2 - Enhanced Features
- [ ] Implement selective TID hiding (hide specific TID banks, not all)
- [ ] Add support for variable EPC lengths (1-6 words)
- [ ] Implement User memory bank hiding
- [ ] Add batch untraceable operations for multiple tag patterns
- [ ] Create UI controls for password input and EPC pattern customization

### Phase 3 - Tag Support Expansion
- [ ] Test on additional tag models (Monza, XPOL, etc.)
- [ ] Implement tag autodiscovery and capability detection
- [ ] Add fallback logic for non-Gen2v2 tags
- [ ] Create compatibility matrix documentation

### Phase 4 - Performance & Reliability
- [ ] Optimize access filter matching algorithm
- [ ] Implement retry logic for failed operations
- [ ] Add operation timeout handling
- [ ] Create performance benchmarks
- [ ] Add logging and error reporting

### Phase 5 - Documentation & Release
- [ ] Create API documentation (JavaDoc)
- [ ] Add unit tests with mock reader
- [ ] Create user guide with examples
- [ ] Add troubleshooting guide
- [ ] Create demo app with UI flows

---

## Directories

### Local Directory
```
Project Root:
/Users/chucklin/StudioProjects/69_G2V2

Key Subdirectories:
├── app/                           # Android app module
│   ├── src/main/java/            # Main Java source files
│   │   └── com/example/g2v2/
│   │       ├── MainActivity.java  # UI and main logic
│   │       ├── RFIDHandler.java   # RFID reader interface & Untraceable ops
│   │       └── TagEntry.java      # Tag data model
│   ├── src/main/res/             # Android resources
│   │   ├── layout/               # XML layouts
│   │   ├── values/               # Strings and constants
│   │   └── drawable/             # Images and drawables
│   ├── libs/                     # Local AAR libraries
│   │   ├── API3_*.aar            # RFID reader API modules
│   │   ├── rfidhostlib.aar       # Host library
│   │   └── rfidseriallib.aar     # Serial communication library
│   └── build.gradle              # App module build config
│
├── RFIDAPI3Library/              # RFID API 3 library module
│   └── build.gradle              # Library build config
│
├── README.md                      # Test results and feature doc
├── untraceable.md                # Technical reference
├── myNote.md                      # This file
├── build.gradle                   # Root project build config
├── settings.gradle                # Project structure config
└── gradle/                        # Gradle wrapper
```

### Remote Repository
```
Repository: [To be configured]
Branch: main
Tag: v1.0.0
```

**Note**: Configure remote after initial setup:
```bash
git remote add origin <repository-url>
git push -u origin main
git push origin v1.0.0
```

---

## Development Setup

### Build Commands
```bash
# Build the app
./gradlew build

# Run tests
./gradlew test

# Create release APK
./gradlew assembleRelease

# Clean build
./gradlew clean build
```

### Key Configuration Files
- `local.properties` — Local SDK paths and configuration
- `gradle.properties` — Gradle build properties
- `build.gradle` (root) — Root project configuration
- `app/build.gradle` — App module dependencies and build config

---

## Build Fixes

### Gradle/Java Compatibility Issue
- **Problem**: Gradle 8.13 test task incompatible with Java 25
- **Solution**: Updated `build-and-run.sh` to use `assembleDebug` instead of `build`
  - Skips problematic unit test task during build
  - Test task still available via `./build-and-run.sh test`
  - Allows compilation and APK generation without Java version conflicts

### Multi-Catch Syntax Error
- **Problem**: `catch (InvalidUsageException | OperationFailureException | Exception e)`
  - Java doesn't allow parent class alongside subclasses in multi-catch
- **Solution**: Removed generic `Exception` from line 345 in RFIDHandler.java
  - Kept specific exception handlers for RFID API exceptions
  - Build now compiles successfully

### Build Status
✅ **BUILD SUCCESSFUL** with `./build-and-run.sh build`

---

## Build & Run Automation Scripts (v1.0.0+)

✅ **build-and-run.sh** (macOS/Linux)
- Automated clean, build, and run workflow
- Supports commands: clean, build, run, all, test, release, help
- Colored output for easy readability
- Comprehensive error handling and exit codes
- Debug and release build support
- Device/emulator detection and automatic app launching
- Made executable: chmod +x build-and-run.sh

✅ **build-and-run.bat** (Windows)
- Windows batch equivalent of shell script
- Identical commands and functionality
- Error handling with errorlevel checks
- Device deployment via ADB
- Colored console output

✅ **BUILD.md** (Comprehensive Documentation)
- Full usage guide with examples for all platforms
- Prerequisites and environment setup instructions
- Troubleshooting section
- CI/CD integration examples (GitHub Actions, GitLab CI)
- Build artifact locations and signing configuration

**Quick Commands:**
```bash
# macOS/Linux
./build-and-run.sh all      # Clean, build, and run
./build-and-run.sh clean    # Clean only
./build-and-run.sh build    # Build only
./build-and-run.sh release  # Build release APK
./build-and-run.sh test     # Run tests

# Windows
build-and-run.bat all       # Clean, build, and run
build-and-run.bat build     # Build only
build-and-run.bat release   # Build release APK
```

---

## Code Cleanup (v1.0.0)

✅ **RFIDHandler.java**
- Removed 200+ lines of commented-out code blocks (old test implementations)
- Removed unused `performUntraceableHideEPCTest()` method
- Consolidated exception handling: Multi-catch syntax (`InvalidUsageException | OperationFailureException`)
- Modernized code: Replaced anonymous Runnable with lambda expressions
- Fixed formatting issues: Removed unnecessary null initializations
- Added Javadoc comments to public methods: `Test1()`, `Test2()`, `Defaults()`, `performInventory()`, `performUntraceable()`, `restorePublicAccess()`, `stopInventory()`
- Removed double semicolon and extra blank lines
- Final size: 575 lines (from ~800+ with dead code)

✅ **MainActivity.java**
- Aligned variable declarations consistently
- Simplified menu item handler formatting
- Final size: 211 lines

✅ **TagEntry.java**
- Added class-level Javadoc documentation
- Final size: 19 lines

**Files Improved:**
- RFIDHandler.java: ~30% reduction in lines due to dead code removal
- All files now follow consistent code style and documentation standards
- Exception handling standardized across all methods

---

## Related Files

- **untraceable.md** — Complete technical reference with code examples
- **README.md** — User-facing documentation and test results
- **app/src/main/java/com/example/g2v2/RFIDHandler.java** — Core implementation
- **app/src/main/java/com/example/g2v2/MainActivity.java** — UI integration
