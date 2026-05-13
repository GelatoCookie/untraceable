package com.zebra.rfid.demo.sdksample;

/**
 * Data model for RFID tag information.
 * Stores tag ID, signal strength, read count, and optional memory bank data.
 */
class TagEntry {
    String tagId;
    short peakRSSI;
    int readCount;
    String memoryBank;
    String memoryBankData;

    TagEntry(String tagId, short peakRSSI, int readCount) {
        this.tagId = tagId;
        this.peakRSSI = peakRSSI;
        this.readCount = readCount;
    }
}
