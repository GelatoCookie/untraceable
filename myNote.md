# Project Notes — G2V2 Untraceable Feature

## Current Snapshot

- Documented release: **v1.0.3**
- Current project root:
  - `/Users/chucklin/StudioProjects/66_untraceable/untraceable`
- Current package:
  - `com.zebra.rfid.demo.sdksample`

## Implemented Behavior (Current Code)

- `performUntraceable()` default:
  - Hide EPC to 2 words (`setShowEpc(false)`, `setEpcLen(2)`)
  - Show TID (`setTid(UNTRACEABLE_TID.SHOW_TID)`)
- `restorePublicAccess()`:
  - Restore EPC to 6 words (`setShowEpc(true)`, `setEpcLen(6)`)
  - Show TID (`setTid(UNTRACEABLE_TID.SHOW_TID)`)
- UI dialog supports runtime configuration of:
  - password, EPC visibility/length, TID mode
  - memory bank, tag pattern/bit count, bit offset, tag mask/bit count
- Tag Pattern is auto-filled from first 2 EPC words of selected tag.
- Dedicated UI button exists for restore public access.

## Hardening Already Applied

- Null-safe reader disappearance handling
- Hex input validation on UI side
- Safe hex parsing with explicit error checks
- Null guard for access-read memory bank data logging
- Menu handlers return immediately when handled

## Markdown Files Aligned

- `README.md`
- `BUILD.md`
- `untraceable.md`
- `myNote.md`

## Script Notes

- `build-and-run.sh build` uses `assembleDebug`
- `build-and-run.bat build` uses `assembleDebug --info`

## v1.0.3 Release Notes

- Explicit Gen2v2 success-state handling in `eventReadNotify()`
- RFID status text now surfaces Untraceable success, failure, or unavailable status
- Bottom-of-screen controls reorganized into a cleaner panel layout
- Windows build script aligned with shell-script debug build behavior
