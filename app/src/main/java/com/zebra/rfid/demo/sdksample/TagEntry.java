package com.zebra.rfid.demo.sdksample;

class TagEntry {
    String tagId;
    short peakRSSI;
    int readCount;

    TagEntry(String tagId, short peakRSSI, int readCount) {
        this.tagId = tagId;
        this.peakRSSI = peakRSSI;
        this.readCount = readCount;
    }
}
