package com.ublox.BLE.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.interfaces.BluetoothGattRepresentation;

import static android.bluetooth.BluetoothDevice.PHY_LE_1M_MASK;
import static android.bluetooth.BluetoothDevice.PHY_LE_2M_MASK;
import static android.bluetooth.BluetoothDevice.PHY_LE_CODED_MASK;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

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

    public BluetoothGattRepresentation connectGatt(Context context, BluetoothGattCallback callback, boolean phy2M) {
        BluetoothGatt gatt;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (phy2M) {
                gatt = device.connectGatt(context, false, callback, TRANSPORT_LE,PHY_LE_CODED_MASK ^ PHY_LE_2M_MASK ^ PHY_LE_1M_MASK);
            } else {
                gatt = device.connectGatt(context, false, callback, TRANSPORT_LE,PHY_LE_CODED_MASK ^ PHY_LE_1M_MASK);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gatt = device.connectGatt(context, false, callback, TRANSPORT_LE);
        } else {
            gatt = device.connectGatt(context, false, callback);
        }
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
