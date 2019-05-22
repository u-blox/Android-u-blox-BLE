package com.ublox.BLE.services;

import java.util.UUID;

public interface BluetoothLeServiceReceiver {
    void onDescriptorWrite();
    void onPhyAvailable(boolean isUpdate);
    void onMtuUpdate(int mtu, int status);
    void onRssiUpdate(int rssi);
    void onDataAvailable(UUID uUid, int type, byte[] data);
    void onServicesDiscovered();
    void onGattDisconnected();
    void onGattConnected();
}
