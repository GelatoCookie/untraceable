# G2V2 Untraceable Feature — Ucode 9xe Tag

## Release

Current documented release: **v1.0.3**

## v1.0.3 Highlights

- UI-driven Untraceable flow refined for selected-tag operations
- Gen2v2 success-state handling now requires explicit success statuses
- Main-screen bottom controls cleaned up and restore action kept on-screen

## Current Behavior Snapshot

This app implements Gen2v2 Untraceable operations for tags matched by EPC pattern filtering.

Default operation behavior in current code:

- `performUntraceable()`
  - EPC visibility: hidden to 2 words (`setShowEpc(false)`, `setEpcLen(2)`)
  - TID visibility: shown (`setTid(UNTRACEABLE_TID.SHOW_TID)`)
- `restorePublicAccess()`
  - EPC visibility: restored to 6 words (`setShowEpc(true)`, `setEpcLen(6)`)
  - TID visibility: shown (`setTid(UNTRACEABLE_TID.SHOW_TID)`)

## UI Features (Current)

- Tag list supports selecting a tag to open Untraceable configuration.
- Dialog includes:
  - Password (hex)
  - Show EPC toggle and EPC length
  - TID mode (Hide All / Show TID / Show User)
  - AccessFilter controls: memory bank, tag pattern, bit count, bit offset, tag mask, mask bit count
- Tag Pattern is auto-filled from the first 2 words (first 8 hex chars) of the selected EPC.
- Memory bank spinner is functional and persisted into AccessFilter params.
- Dedicated on-screen **Restore Access** button calls `restorePublicAccess()`.

## Validation and Guardrails

- Reader disconnect callback is null-safe before host name comparison.
- Hex input validation is enforced in UI:
  - Password: hex, 1 to 8 chars
  - Tag pattern and mask: even-length hex
  - Bit counts cannot exceed provided hex length
- `hexStringToByteArray` validates empty, odd-length, and invalid hex data.
- Access read logging checks `memoryBankData != null` before length checks.

## Core Files

- `app/src/main/java/com/zebra/rfid/demo/sdksample/MainActivity.java`
- `app/src/main/java/com/zebra/rfid/demo/sdksample/RFIDHandler.java`
- `app/src/main/java/com/zebra/rfid/demo/sdksample/TagEntry.java`
- `untraceable.md` for detailed technical spec
- `BUILD.md` for script usage and build details

## Hardware / Protocol

- Reader family: Zebra RFD series (Gen2v2 capable)
- Tag class: Ucode 9xe (as tested target)
- Protocol: ISO 18000-63 Gen2v2

## Release History

- **v1.0.3** — release tag for UI cleanup, explicit Gen2v2 status handling, and script parity updates
- **v1.0.2** — UI-driven Untraceable params, doc alignment, callback/validation hardening
- **v1.0.1** — Build flow and documentation stabilization
- **v1.0.0** — Initial Untraceable + restore implementation
