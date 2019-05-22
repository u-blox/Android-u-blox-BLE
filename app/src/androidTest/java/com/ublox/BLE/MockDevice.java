package com.ublox.BLE;

import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;

import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.interfaces.BluetoothGattRepresentation;

import static android.bluetooth.BluetoothDevice.BOND_NONE;

public class MockDevice implements BluetoothDeviceRepresentation {
    public static Parcelable.Creator<MockDevice> CREATOR = new Parcelable.Creator<MockDevice>() {

        @Override
        public MockDevice createFromParcel(Parcel parcel) {
            return new MockDevice();
        }

        @Override
        public MockDevice[] newArray(int i) {
            return new MockDevice[i];
        }
    };

    @Override
    public BluetoothGattRepresentation connectGatt(Context context, BluetoothGattCallback callback, boolean phy2M) {
        MockGatt gatt = new MockGatt(callback);
        Handler main = new Handler(Looper.getMainLooper());
        main.post(gatt::connect);
        return gatt;
    }

    @Override
    public String getAddress() {
        return "E8:47:5E:19:6A:06";
    }

    @Override
    public int getBondState() {
        return BOND_NONE;
    }

    @Override
    public String getName() {
        return "TEST-T1-012345";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }
}
