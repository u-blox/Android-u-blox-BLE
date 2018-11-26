package com.ublox.BLE.interfaces;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.List;

public interface BluetoothGattRepresentation {
    void close();
    boolean connect();
    void disconnect();
    boolean discoverServices();
    List<BluetoothGattService> getServices();
    boolean readCharacteristic(BluetoothGattCharacteristic characteristic);
    void readPhy();
    boolean readRemoteRssi();
    boolean requestConnectionPriority(int connectionParameter);
    boolean requestMtu(int size);
    boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable);
    void setPreferredPhy(int txPhy, int rxPhy, int phyOptions);
    boolean writeCharacteristic(BluetoothGattCharacteristic characteristic);
    boolean writeDescriptor(BluetoothGattDescriptor descriptor);
}
