package com.ublox.BLE;

import com.ublox.BLE.activities.MainActivity;

public class DeviceConnectionIdler extends TimeoutIdler {
    private MainActivity activity;

    public DeviceConnectionIdler(MainActivity activity, long timeout) {
        super(timeout);
        this.activity = activity;
    }

    @Override
    protected boolean ready() {
        return activity.isConnected();
    }
}
