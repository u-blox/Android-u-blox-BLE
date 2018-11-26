package com.ublox.BLE.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;

import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.interfaces.BluetoothGattRepresentation;

public class UBloxDevice implements BluetoothDeviceRepresentation {
    private BluetoothDevice device;

    public static Parcelable.Creator<UBloxDevice> CREATOR = new Parcelable.Creator<UBloxDevice>() {

        @Override
        public UBloxDevice createFromParcel(Parcel parcel) {
            return new UBloxDevice(BluetoothDevice.CREATOR.createFromParcel(parcel));
        }

        @Override
        public UBloxDevice[] newArray(int size) {
            return new UBloxDevice[size];
        }
    };

    public UBloxDevice(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public String getAddress() {
        return device.getAddress();
    }

    @Override
    public String getName() {
        return device.getName();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public BluetoothGattRepresentation connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport, int phy) {
        BluetoothGatt gatt = device.connectGatt(context, autoConnect, callback, transport, phy);
        return gatt != null ? new UBloxGatt(gatt) : null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public BluetoothGattRepresentation connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport) {
        BluetoothGatt gatt = device.connectGatt(context, autoConnect, callback, transport);
        return gatt != null ? new UBloxGatt(gatt) : null;
    }

    @Override
    public BluetoothGattRepresentation connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback) {
        BluetoothGatt gatt = device.connectGatt(context, autoConnect, callback);
        return gatt != null ? new UBloxGatt(gatt) : null;
    }

    @Override
    public int getBondState() {
        return device.getBondState();
    }

    @Override
    public int describeContents() {
        return device.describeContents();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        device.writeToParcel(parcel, i);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UBloxDevice && device.equals(((UBloxDevice) o).device);
    }

    @Override
    public int hashCode() {
        return device.hashCode();
    }
}
