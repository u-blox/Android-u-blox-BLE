package com.ublox.BLE;

import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import android.widget.Adapter;

public class DeviceDiscoverIdler extends TimeoutIdler {
    private Adapter list;
    //todo prefer not to have android components here, but don't have access to backing structure
    private String device;

    public DeviceDiscoverIdler(String device, Adapter list, long timeout) {
        super(timeout);
        this.device = device;
        this.list = list;
    }

    @Override
    protected boolean ready() {
        int count = list.getCount();
        for (int i = 0; i < count; i++) {
            BluetoothDeviceRepresentation dev = (BluetoothDeviceRepresentation) list.getItem(i);
            if (device.equals(dev.getAddress()))  {
                return true;
            }
        }
        return false;
    }
}
