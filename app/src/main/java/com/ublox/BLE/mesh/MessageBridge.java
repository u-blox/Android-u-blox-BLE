package com.ublox.BLE.mesh;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public interface MessageBridge {
    int getSource();
    int getDestination();
    int getOpCode();
    int getTimeToLive();
    byte[] getParameters();
}
