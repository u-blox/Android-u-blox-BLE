package com.ublox.BLE.bluetooth;

import java.util.List;
import java.util.UUID;

public interface BluetoothCentral {
    void setDelegate(Delegate delegate);
    State getState();
    List<BluetoothPeripheral> getFoundPeripherals();

    void scan(List<UUID> withServices);
    void stop();

    enum State {OFF, ON, SCANNING}

    interface Delegate {
        void centralChangedState(BluetoothCentral central);
        void centralFoundPeripheral(BluetoothCentral central, BluetoothPeripheral peripheral);
    }
}
