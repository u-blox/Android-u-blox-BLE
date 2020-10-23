package com.ublox.BLE.bluetooth;

import android.os.Parcelable;

import java.util.List;
import java.util.UUID;

public interface BluetoothPeripheral extends Parcelable {
    String identifier();
    String name();
    State getState();
    int bondState();
    int rssi();
    boolean advertisedService(UUID service);
    byte[] serviceDataFor(UUID service);
    void setDelegate(Delegate delegate);

    void connect();
    void disconnect();

    void discover();
    List<UUID> services();
    List<UUID> characteristics(UUID service);

    void set(UUID characteristic, boolean notify);
    void write(UUID characteristic, byte[] data, boolean withResponse);
    void read(UUID characteristic);

    void readRssi();
    void setPreferredMtu(int mtu);
    void requestConnectionPriority(Priority priority);
    int maximumDataCount(boolean withResponse);

    enum State {DISCONNECTED, CONNECTED, ERROR}
    enum Priority {BALANCED, LOW, HIGH}

    interface Delegate {
        void bluetoothPeripheralChangedState(BluetoothPeripheral peripheral);
        void bluetoothPeripheralDiscovered(BluetoothPeripheral peripheral, boolean ok);

        void bluetoothPeripheralSet(BluetoothPeripheral peripheral, UUID characteristic, boolean notify, boolean ok);
        void bluetoothPeripheralWrote(BluetoothPeripheral peripheral, UUID characteristic, boolean ok);
        void bluetoothPeripheralRead(BluetoothPeripheral peripheral, UUID characteristic, byte[] data, boolean ok);

        void bluetoothPeripheralReadRssi(BluetoothPeripheral peripheral, boolean ok);
    }
}
