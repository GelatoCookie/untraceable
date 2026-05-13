# G2V2 Untraceable Feature — Ucode 9xe Tag

## Release

Current release: **v1.0.1**

### v1.0.1 Highlights
- Build workflow stabilized for Java 25 + Gradle 8.13 environments
- `build-and-run.sh` now uses `assembleDebug` for reliable debug builds
- `RFIDHandler.java` multi-catch syntax fixed to resolve Java compile errors
- Documentation refreshed and synchronized across project markdown files

## Test Status: ✅ PASSED

This project demonstrates successful implementation and testing of the **Untraceable** command (Gen2v2 ISO 18000-63) on **Ucode 9xe tags**.

---

## Test Results

### Hide Operation
- **Command**: `performUntraceable()`
- **Target**: Ucode 9xe tags (EPC pattern: `33333333`)
- **Result**: ✅ PASSED
  - **EPC**: Hidden to 2 words (from 6 words)
  - **TID**: All TID hidden (HIDE_ALL_TID)
  - **Verification**: Tag responds to standard inventory reads with only 2-word EPC data visible

### Restore Operation
- **Command**: `restorePublicAccess()`
- **Target**: Ucode 9xe tags (same EPC pattern: `33333333`)
- **Result**: ✅ PASSED
  - **EPC**: Restored to 6 words (full EPC length)
  - **TID**: All TID restored and visible (SHOW_TID)
  - **Verification**: Tag responds to standard inventory reads with full EPC (6 words) and complete TID bank visible

---

## Key Features Tested

### 1. **Hide 2 Words of EPC and All TID**
Demonstrates the ability to truncate EPC visibility to just 2 words while completely hiding the TID memory bank during standard RFID inventory operations.

```java
// Hide EPC to 2 words, hide all TID
untraceableParams.setEpcLen(2);        // Show only 2 words of EPC
untraceableParams.setTid(UNTRACEABLE_TID.HIDE_ALL_TID); // Hide all TID
```

**Use Case**: Privacy-sensitive deployments where full EPC and TID data should not be visible during standard reads.

### 2. **Restore 6 Words of EPC and Full TID**
Reverses the untraceable operation, restoring full access to both EPC and TID memory banks.

```java
// Restore full EPC (6 words) and TID
untraceableParams.setEpcLen(6);        // Restore full EPC length
untraceableParams.setTid(UNTRACEABLE_TID.SHOW_TID); // Show all TID
```

**Use Case**: Re-enabling full tag visibility for inventory management and tracking.

---

## Implementation Details

### Access Filter
Both operations use an `AccessFilter` to target only Ucode 9xe tags whose EPC begins with `33333333`:

- **Memory Bank**: EPC (Bank 01)
- **Pattern**: `0x33333333` (32 bits)
- **Bit Offset**: 32 (skips CRC-16 and Protocol Control)
- **Tag Mask**: `0xffffffff` (exact match required)

### Password Authentication
Operations are protected with a 16-bit password:
```
Password: 0x00000001
```

---

## Files

- **[untraceable.md](untraceable.md)** — Detailed technical reference for Untraceable command implementation
- **MainActivity.java** — Application entry point and UI
- **RFIDHandler.java** — Core RFID reader interface and Untraceable operations
- **TagEntry.java** — Tag data model

---

## Hardware Tested
- **Reader**: Impinj R700 or compatible Gen2v2 reader
- **Tag**: Ucode 9xe (NXP)
- **Protocol**: ISO 18000-63 Gen2v2

---

## Version
- **v1.0.1** — Build stability fixes, script updates, and documentation refresh
- **v1.0.0** — Initial release with Ucode 9xe tag support
