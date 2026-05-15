package com.zebra.rfid.demo.sdksample;

/**
 * Data model for RFID tag information.
 * Stores tag ID, signal strength, read count, memory bank data, and Untraceable operation parameters.
 */
class TagEntry {
    String tagId;
    short peakRSSI;
    int readCount;
    String memoryBank;
    String memoryBankData;

    // Untraceable operation parameters
    String password = "00000001";
    boolean showEpc = false;
    int epcLen = 2;
    String tidOption = "SHOW_TID"; // HIDE_ALL_TID, SHOW_TID, SHOW_USER
    
    // AccessFilter parameters
    String accessFilterMemoryBank = "MEMORY_BANK_EPC";
    String tagPattern = "33333333";
    int tagPatternBitCount = 32;
    int bitOffset = 32;
    String tagMask = "FFFFFFFF";
    int tagMaskBitCount = 32;

    TagEntry(String tagId, short peakRSSI, int readCount) {
        this.tagId = tagId;
        this.peakRSSI = peakRSSI;
        this.readCount = readCount;
    }
}
