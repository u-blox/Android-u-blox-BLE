package com.ublox.BLE.bluetooth;

import android.os.Handler;
import android.os.Looper;

import com.ublox.BLE.datapump.DataStream;
import com.ublox.BLE.utils.GattAttributes;

import java.util.UUID;

public class SpsStream implements DataStream, BluetoothPeripheral.Delegate {
    private static final byte MAX_CREDITS = 32;
    private static final byte CLOSING_CREDITS = -1;
    private static final UUID CREDITS = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_CREDITS);
    private static final UUID FIFO = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_FIFO);

    private State state;
    private boolean flowControl;
    private BluetoothPeripheral peripheral;
    private Delegate delegate;

    private byte txCredits;
    private byte rxCredits;
    private byte[] pendingData;
    private boolean pendingQueued;

    public SpsStream(BluetoothPeripheral peripheral) {
        state = State.CLOSED;
        this.peripheral = peripheral;
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
        open(true);
    }

    public void open(boolean withFlowControl) {
        if (state == State.OPENED) return;

        flowControl = withFlowControl;
        peripheral.connect();
    }

    @Override
    public void close() {
        if (state == State.OPENED) {
            peripheral.disconnect();
        }
    }

    @Override
    public void write(byte[] data) {
        if (state != State.OPENED) return;
        pendingData = data;

        if (!flowControl || txCredits > 0) {
            peripheral.write(FIFO, data, false);
        } else {
            pendingQueued = true;
        }
        if (flowControl && txCredits > 0) {
            txCredits--;
        }
    }

    private void setState(State newState) {
        if (state == newState) return;
        state = newState;
        if (delegate != null) new Handler(Looper.getMainLooper()).postDelayed(() -> {
            delegate.dataStreamChangedState(this);
        }, 10000);
    }

    @Override
    public void bluetoothPeripheralChangedState(BluetoothPeripheral peripheral) {
        switch (peripheral.getState()) {
            case CONNECTED: peripheral.discover(); break;
            case ERROR: setState(State.ERROR); break;
            case DISCONNECTED: setState(State.CLOSED); break;
        }
    }

    @Override
    public void bluetoothPeripheralDiscovered(BluetoothPeripheral peripheral, boolean ok) {
        if (ok) {
            this.peripheral.set(flowControl ? CREDITS : FIFO , true);
        } else {
            close();
            setState(State.ERROR);
        }
    }

    @Override
    public void bluetoothPeripheralSet(BluetoothPeripheral peripheral, UUID characteristic, boolean notify, boolean ok) {
        if (ok) {
            if (characteristic.equals(CREDITS) && notify) {
                this.peripheral.set(FIFO, true);
            } else if (characteristic.equals(FIFO) && notify) {
                if (flowControl) {
                    txCredits = 0;
                    rxCredits = MAX_CREDITS;
                    byte[] credits = {MAX_CREDITS};
                    this.peripheral.write(CREDITS, credits, false);
                } else {
                    setState(State.OPENED);
                }
            }
        } else {
            close();
            setState(State.ERROR);
        }
    }

    @Override
    public void bluetoothPeripheralWrote(BluetoothPeripheral peripheral, UUID characteristic, boolean ok) {
        if (!characteristic.equals(FIFO)) return;

        byte[] data = pendingData;
        if (data == null || !ok) data = new byte[0];
        pendingData = null;
        pendingQueued = false;

        if (delegate != null) delegate.dataStreamWrote(this, data);
    }

    @Override
    public void bluetoothPeripheralRead(BluetoothPeripheral peripheral, UUID characteristic, byte[] data, boolean ok) {
        if (characteristic.equals(CREDITS) && ok) {
            if (data[0] == CLOSING_CREDITS) {
                close();
            } else {
                txCredits += data[0];
                setState(State.OPENED);
                if (pendingQueued) write(pendingData);
            }
        }

        if (characteristic.equals(FIFO) && ok) {
            if (flowControl) {
                rxCredits--;
                byte half = MAX_CREDITS / 2;
                if (rxCredits <= half) {
                    rxCredits += half;
                    byte[] credits = {half};
                    peripheral.write(CREDITS, credits, false);
                }
            }
            if (delegate != null) delegate.dataStreamRead(this, data);
        }
    }

    @Override
    public void bluetoothPeripheralReadRssi(BluetoothPeripheral peripheral, boolean ok) {}
}
