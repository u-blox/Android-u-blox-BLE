package com.ublox.BLE.interfaces;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Parcelable;

public interface BluetoothDeviceRepresentation extends Parcelable {
    BluetoothGattRepresentation connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport, int phy);
    BluetoothGattRepresentation connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport);
    BluetoothGattRepresentation connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback);
    String getAddress();
    int getBondState();
    String getName();
}
