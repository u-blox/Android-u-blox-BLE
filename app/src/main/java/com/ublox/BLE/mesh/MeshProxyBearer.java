package com.ublox.BLE.mesh;

import com.ublox.BLE.bluetooth.BluetoothPeripheral;
import com.ublox.BLE.utils.GattAttributes;

import java.util.UUID;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class MeshProxyBearer implements MeshProxyProtocol.MeshBearer, BluetoothPeripheral.Delegate {
    private static final int DEFAULT_PROXY_MTU = 69;
    private static final int GATT_PACKET_OVERHEAD = 3;
    private static final UUID DATA_IN = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_MESH_PROXY_DATA_IN);
    private static final UUID DATA_OUT = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_MESH_PROXY_DATA_OUT);

    private BluetoothPeripheral peripheral;
    private State state;
    private Delegate delegate;

    private byte[] pendingData;

    public MeshProxyBearer(BluetoothPeripheral peripheral) {
        this.peripheral = peripheral;
        this.state = State.CLOSED;
        peripheral.setDelegate(this);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void open() {
        if (state == State.OPENED) return;
        peripheral.setPreferredMtu(DEFAULT_PROXY_MTU);
        peripheral.connect();
    }

    @Override
    public void close() {
        closeWith(State.CLOSED);
    }

    @Override
    public void write(byte[] data) {
        if (state != State.OPENED) return;
        pendingData = data;
        peripheral.write(DATA_IN, data, false);
    }

    @Override
    public int maxTransmissionUnit() {
        return state == State.OPENED
            ? peripheral.maximumDataCount(false) + GATT_PACKET_OVERHEAD
            : 0;
    }

    @Override
    public void bluetoothPeripheralChangedState(BluetoothPeripheral peripheral) {
        if (peripheral.getState() == BluetoothPeripheral.State.CONNECTED) {
            peripheral.discover();
        } else if (state == State.OPENED) {
            closeWith(peripheral.getState() == BluetoothPeripheral.State.DISCONNECTED ? State.CLOSED : State.ERROR);
        }
    }

    @Override
    public void bluetoothPeripheralDiscovered(BluetoothPeripheral peripheral, boolean ok) {
        if (ok) {
            peripheral.set(DATA_OUT, true);
        } else {
            closeWith(State.ERROR);
        }
    }

    @Override
    public void bluetoothPeripheralSet(BluetoothPeripheral peripheral, UUID characteristic, boolean notify, boolean ok) {
        if (DATA_OUT.equals(characteristic) && notify && ok) {
            peripheral.requestConnectionPriority(BluetoothPeripheral.Priority.HIGH);
        } else if (DATA_OUT.equals(characteristic)) {
            closeWith(State.ERROR);
        }
    }

    @Override
    public void bluetoothPeripheralWrote(BluetoothPeripheral peripheral, UUID characteristic, boolean ok) {
        if (!DATA_IN.equals(characteristic)) return;

        byte[] data = pendingData;
        pendingData = null;
        if (ok && delegate != null) delegate.dataStreamWrote(this, data);
    }

    @Override
    public void bluetoothPeripheralRead(BluetoothPeripheral peripheral, UUID characteristic, byte[] data, boolean ok) {
        if (!DATA_OUT.equals(characteristic)) return;

        if (state != State.OPENED) {
            peripheral.requestConnectionPriority(BluetoothPeripheral.Priority.BALANCED);
            setState(State.OPENED);
        }
        if (ok && delegate != null) delegate.dataStreamRead(this, data);
    }

    @Override
    public void bluetoothPeripheralReadRssi(BluetoothPeripheral peripheral, boolean ok) {}

    private void closeWith(State state) {
        setState(state);
        peripheral.disconnect();
    }

    private void setState(State newState) {
        if (state == newState) return;
        state = newState;
        if (delegate != null) delegate.dataStreamChangedState(this);
    }
}
