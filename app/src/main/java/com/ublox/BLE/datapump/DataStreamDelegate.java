package com.ublox.BLE.datapump;

public interface DataStreamDelegate {
    void onWrite(byte[] data);
    void onRead(byte[] data);
}
