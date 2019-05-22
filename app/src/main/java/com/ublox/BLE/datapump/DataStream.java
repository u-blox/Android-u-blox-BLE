package com.ublox.BLE.datapump;

public interface DataStream {
    void setDelegate(DataStreamDelegate delegate);
    void write(byte[] data);
}
