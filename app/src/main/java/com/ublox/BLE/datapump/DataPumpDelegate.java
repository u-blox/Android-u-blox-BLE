package com.ublox.BLE.datapump;

/**
 * Interface defining data pump test events
 */
public interface DataPumpDelegate {
    void onTx(long bytes, long duration);
    void onRx(long bytes, long duration);
    void updateMTUSize(int size);
}
