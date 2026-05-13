# Untraceable Feature — Technical Reference

## Release

Current release: **v1.0.1**

### v1.0.1 Notes
- Core Untraceable behavior is unchanged
- Build and run workflow is updated and stable with current environment
- Documentation synchronized with release tag v1.0.1

## Overview

The **Untraceable** command is a Gen2v2 (ISO 18000-63) operation that instructs a tag to hide or reveal specific memory banks (EPC, TID, User) from standard inventory reads. This sample demonstrates:

- **`performUntraceable()`** — Hide all TID, show only 2-words of EPC on tags whose EPC word 2–3 matches `33333333`.
- **`restorePublicAccess()`** — Reverse the operation: re-expose full EPC (6 words), full TID, and User memory on the same tag population.
- **`Read TID and EPC`** — Perform access operations to read both memory banks.

---

## EPC Memory Bank Layout

Understanding the EPC memory bank structure is critical for setting the access filter correctly.

```
EPC Memory Bank (Bank 01)
┌─────────────────────────────────────────────────────────┐
│ Word 0 (bits  0–15): CRC-16                             │
│ Word 1 (bits 16–31): Protocol Control (PC)              │
│ Word 2 (bits 32–47): EPC byte 0–1   ← filter starts here│
│ Word 3 (bits 48–63): EPC byte 2–3                       │
│ Word 4 (bits 64–79): EPC byte 4–5                       │
│ ...                                                     │
└─────────────────────────────────────────────────────────┘
```

- `setBitOffset(32)` skips CRC + PC words, pointing to the start of the actual EPC data.
- The sample targets tags whose first two EPC words (32 bits) equal `0x33333333`.

---

## Access Filter — How Tag Matching Works

Both operations use an `AccessFilter` to scope the command to a specific subset of tags. Only tags whose EPC (at the given offset, masked) matches the pattern will respond to the Untraceable command.

```java
AccessFilter accessFilter = new AccessFilter();

// 4-byte all-ones mask — every bit of the pattern must match exactly
byte[] tagMask = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

accessFilter.TagPatternA.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);  // filter on EPC bank
accessFilter.TagPatternA.setTagPattern("33333333");                    // 32-bit EPC pattern to match
accessFilter.TagPatternA.setTagPatternBitCount(32);                    // pattern length = 32 bits
accessFilter.TagPatternA.setBitOffset(32);                             // skip CRC+PC (2 words = 32 bits)
accessFilter.TagPatternA.setTagMask(tagMask);                          // 4 bytes × 8 bits = 32-bit mask
accessFilter.TagPatternA.setTagMaskBitCount(tagMask.length * 8);       // = 32
accessFilter.setAccessFilterMatchPattern(FILTER_MATCH_PATTERN.A);     // use Pattern A only
```

---

## `performUntraceable()` — Show 2-words EPC, Hide TID

### Purpose

Truncates the EPC response to only 2 words and hides the TID bank entirely during inventory.

### Code

```java
synchronized void performUntraceable() {
    // ...
    Gen2v2.UntraceableParams untraceableParams = gen2V2.new UntraceableParams();
    untraceableParams.setPassword(Long.parseLong("00000001", 16));
    untraceableParams.setShowEpc(true);    // Show EPC
    untraceableParams.setHideEpc(false);
    untraceableParams.setEpcLen(2);        // Only show 2 words of EPC
    untraceableParams.setTid(UNTRACEABLE_TID.HIDE_ALL_TID); // Hide all TID
    // ...
}
```

---

## `restorePublicAccess()` — Show full EPC (6 words) and TID

### Purpose

Reverses the untraceable operation, restoring full visibility of EPC and TID.

### Code

```java
synchronized void restorePublicAccess() {
    // ...
    Gen2v2.UntraceableParams untraceableParams = gen2V2.new UntraceableParams();
    untraceableParams.setPassword(Long.parseLong("00000001", 16));
    untraceableParams.setShowEpc(true);
    untraceableParams.setHideEpc(false);
    untraceableParams.setEpcLen(6);        // Restore EPC length to 6 words
    untraceableParams.setTid(UNTRACEABLE_TID.SHOW_TID); // Show TID
    untraceableParams.setShowUser(true);
    // ...
}
```

---

## `Read TID and EPC` — Access Operation

### Purpose

Demonstrates reading specific memory banks using access operations.

### Code

```java
synchronized void performAccessTID() {
    // Read TID Bank
    TagAccess.ReadAccessParams readTID = tagAccess.new ReadAccessParams();
    readTID.setMemoryBank(MEMORY_BANK.MEMORY_BANK_TID);
    readTID.setOffset(0);
    readTID.setCount(0);
    reader.Actions.TagAccess.readEvent(readTID, null, null);

    // Read EPC Bank
    TagAccess.ReadAccessParams readEPC = tagAccess.new ReadAccessParams();
    readEPC.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
    readEPC.setOffset(0);
    readEPC.setCount(0);
    reader.Actions.TagAccess.readEvent(readEPC, null, null);
}
```
