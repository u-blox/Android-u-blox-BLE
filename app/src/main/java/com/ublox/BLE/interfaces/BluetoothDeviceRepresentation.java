package com.ublox.BLE.interfaces;

import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Parcelable;

public interface BluetoothDeviceRepresentation extends Parcelable {
    BluetoothGattRepresentation connectGatt(Context context, BluetoothGattCallback callback, boolean phy2M);

    String getAddress();
    int getBondState();
    String getName();
}
