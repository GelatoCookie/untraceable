# G2V2 Untraceable Feature — Technical Specification

## Release

Current documented release: **v1.0.3**

### v1.0.3 Highlights
- Untraceable UI supports per-tag parameterized AccessFilter and operation settings
- Default behavior aligned to Hide EPC + Show TID
- Callback and input validation hardened to reduce runtime faults
- Gen2v2 Untraceable callback now uses explicit success statuses instead of treating `null` status as success
- Main-screen controls keep restore and operation feedback available in the bottom panel

## Scope

This specification defines behavior and validation for:

- `performUntraceable()` on EPC-matched tags
- `restorePublicAccess()` for visibility rollback
- `performUntraceableWithParams(TagEntry)` for UI-driven custom execution
- `performAccessTID()` for TID/EPC read verification
- `eventReadNotify()` handling of access and Gen2v2 telemetry

Out of scope:

- Reader discovery and pairing flows
- UI presentation details outside callback handoff
- Production key management process

---

## Preconditions

- Reader is connected and configured
- Trigger mode is set to RFID mode
- Tag read events are enabled
- Target tags match EPC filter criteria (default pattern `33333333` or UI-specified)
- Access password and RF settings are valid for the environment

---

## EPC Data Contract

### EPC Word Positioning

- Word 0: CRC-16 (bits 0-15)
- Word 1: PC (bits 16-31)
- Word 2 onward: EPC payload (starts at bit offset 32)

Operational implication:

- `setBitOffset(32)` is required to filter on EPC payload bytes
- Pattern `33333333` is evaluated as 32 bits at EPC words 2-3

### Access Filter Reference

```java
AccessFilter accessFilter = new AccessFilter();
byte[] tagMask = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

accessFilter.TagPatternA.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
accessFilter.TagPatternA.setTagPattern("33333333");
accessFilter.TagPatternA.setTagPatternBitCount(32);
accessFilter.TagPatternA.setBitOffset(32);
accessFilter.TagPatternA.setTagMask(tagMask);
accessFilter.TagPatternA.setTagMaskBitCount(tagMask.length * 8);
accessFilter.setAccessFilterMatchPattern(FILTER_MATCH_PATTERN.A);
```

---

## Operation Specifications

### performUntraceable()

Purpose:

- Restrict visible EPC length to 2 words
- Keep TID visible for matched tags (current default)

Inputs:

- Access password `0x00000001` (sample only)
- AccessFilter Pattern A

Expected output:

- Gen2v2 Untraceable response events for matching tags

Failure envelope:

- `InvalidUsageException`
- `OperationFailureException`

Reference snippet:

```java
Gen2v2.UntraceableParams p = gen2V2.new UntraceableParams();
p.setPassword(Long.parseLong("00000001", 16));
p.setShowEpc(false);
p.setHideEpc(false);
p.setEpcLen(2);
p.setTid(UNTRACEABLE_TID.SHOW_TID);
reader.Actions.gen2v2Access.untraceable(p, accessFilter, null);
```

### performUntraceableWithParams(TagEntry)

Purpose:

- Execute Untraceable with runtime parameters from UI selection

Inputs (from `TagEntry`):

- `password` (hex)
- `showEpc`, `epcLen`
- `tidOption`
- `accessFilterMemoryBank`, `tagPattern`, `tagPatternBitCount`, `bitOffset`, `tagMask`, `tagMaskBitCount`

Validation/hardening behavior:

- Hex conversion enforces non-empty, even-length, valid hex characters
- Illegal parameter formatting is trapped by `IllegalArgumentException`

### restorePublicAccess()

Purpose:

- Restore broader tag visibility after Untraceable restriction

Inputs:

- Same access credential domain as `performUntraceable()`
- Same AccessFilter Pattern A

Expected output:

- Gen2v2 response events indicating restoration operation execution

Failure envelope:

- `InvalidUsageException`
- `OperationFailureException`

Reference snippet:

```java
Gen2v2.UntraceableParams p = gen2V2.new UntraceableParams();
p.setPassword(Long.parseLong("00000001", 16));
p.setShowEpc(true);
p.setHideEpc(false);
p.setEpcLen(6);
p.setTid(UNTRACEABLE_TID.SHOW_TID);
p.setShowUser(true);
reader.Actions.gen2v2Access.untraceable(p, accessFilter, null);
```

### performAccessTID()

Purpose:

- Read TID and EPC memory banks as access operations

Expected output:

- Access read responses surfaced through `eventReadNotify()`

Failure envelope:

- `InvalidUsageException`
- `OperationFailureException`

Reference snippet:

```java
TagAccess.ReadAccessParams readTID = tagAccess.new ReadAccessParams();
readTID.setMemoryBank(MEMORY_BANK.MEMORY_BANK_TID);
readTID.setOffset(0);
readTID.setCount(0);
reader.Actions.TagAccess.readEvent(readTID, null, null);

TagAccess.ReadAccessParams readEPC = tagAccess.new ReadAccessParams();
readEPC.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
readEPC.setOffset(0);
readEPC.setCount(0);
reader.Actions.TagAccess.readEvent(readEPC, null, null);
```

---

## eventReadNotify() Specification

### Design Requirement

Untraceable telemetry must be interpreted strictly from Gen2v2 opcode and status fields. A missing Gen2v2 status is unknown telemetry, not implicit success.

### Consolidated Rules

- Gate Untraceable handling with `tag.getG2v2OpCode() == G2V2_OPERATION_UNTRACEABLE`
- Treat only these statuses as success:
    - `ACCESS_SUCCESS`
    - `ACCESS_SUCCESS_STORED_WITHOUT_LENGTH`
    - `ACCESS_SUCCESS_STORED_WITH_LENGTH`
    - `ACCESS_SUCCESS_SEND_WITHOUT_LENGTH`
    - `ACCESS_SUCCESS_SEND_WITH_LENGTH`
- Do not treat `null` `getG2v2OpStatus()` as success
- Keep access-read logs and Untraceable logs in separate branches
- Log EPC, G2v2 op code, status, and response on the same line
- Surface Untraceable success/failure state to the existing RFID status text view
- Preserve asynchronous handoff to `AsyncDataUpdate`
- Guard memory-bank access read with `tag.getMemoryBankData() != null`

### Callback Core Reference

```java
public void eventReadNotify(RfidReadEvents e) {
    TagData[] myTags = reader.Actions.getReadTags(100);
    if (myTags == null) {
        return;
    }

    for (TagData tag : myTags) {
        Log.d(TAG, "Tag ID=" + tag.getTagID() + " opCode=" + tag.getOpCode());

        if (tag.getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ
                && tag.getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS
                && tag.getMemoryBankData() != null
                && tag.getMemoryBankData().length() > 0) {
            Log.d(TAG, "MemBank=" + tag.getMemoryBank() + " data=" + tag.getMemoryBankData());
        }

        if (tag.getG2v2OpCode() == GEN2V2_OPERATION_CODE.G2V2_OPERATION_UNTRACEABLE) {
            GEN2V2_OPERATION_STATUS g2v2Status = tag.getG2v2OpStatus();
            String g2v2Response = tag.getG2v2Response();

            Log.d(TAG, "Untraceable response: EPC=" + tag.getTagID()
                    + " op=" + tag.getG2v2OpCode()
                    + " status=" + g2v2Status
                    + " response=" + g2v2Response);

            if (isSuccessfulGen2v2Status(g2v2Status)) {
                updateReaderStatus("Untraceable success: " + tag.getTagID());
                stopInventory();
            } else if (g2v2Status != null) {
                updateReaderStatus("Untraceable failed: " + g2v2Status);
            } else {
                updateReaderStatus("Untraceable status unavailable");
            }
        }
    }

    new AsyncDataUpdate().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myTags);
}
```

---

## Validation

### Functional Checks

- After `performUntraceable()`, at least one `Untraceable response` log has non-null response
- After `performUntraceable()`, success is accepted only when `getG2v2OpStatus()` is one of the documented success constants
- If `getG2v2OpStatus()` is non-success, the RFID status text reports `Untraceable failed: <STATUS>`
- If `getG2v2OpStatus()` is `null`, the RFID status text reports `Untraceable status unavailable`
- During plain inventory, no Untraceable response log should appear
- After `restorePublicAccess()`, response logs continue and TID/EPC access reads succeed
- After UI execution, selected memory bank and filter values are applied in AccessFilter

### Regression Checks

- Callback throughput remains stable under high tag volume
- `AsyncDataUpdate` still receives the tag batch
- No false Untraceable error interpretation from non-Gen2v2 tags

---

## Failure Modes and Observability

Failure classes:

- Parameter/config errors (mask/offset/bank mismatches)
- Authentication errors (incorrect access password)
- RF/protocol execution failures
- Telemetry interpretation mistakes (mixing inventory with Gen2v2 result logic or treating missing status as success)
- UI input format failures (invalid hex, invalid bit-count-to-length relationship)

Recommended observability fields:

- EPC
- Access/Gen2v2 operation code
- Operation status
- Gen2v2 response payload
- Memory bank data on successful access reads

---

## Security Notes

- Replace sample password before production deployment
- Limit debug log verbosity in production builds
- Validate target tag/filter assumptions per deployment site
