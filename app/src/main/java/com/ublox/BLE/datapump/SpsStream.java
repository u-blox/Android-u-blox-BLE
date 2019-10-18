package com.ublox.BLE.datapump;

import android.bluetooth.BluetoothGattCharacteristic;

import com.ublox.BLE.services.BluetoothLeService;

import java.util.UUID;

import static com.ublox.BLE.services.BluetoothLeService.ITEM_TYPE_NOTIFICATION;
import static com.ublox.BLE.services.BluetoothLeService.ITEM_TYPE_WRITE;

/**
 * A class for handling communication over Serial Port Service as a Stream.
 */
public class SpsStream implements DataStream {
    private static final byte MAX_NUMBER_OF_CREDITS = 0x20;
    private static final byte CLOSE_CONNECTION = (byte) 0xff;

    private BluetoothLeService service;
    private BluetoothGattCharacteristic fifo;
    private BluetoothGattCharacteristic credits;
    private Delegate delegate;
    private boolean withFlowControl;
    private int txCredits;
    private byte[] pendingData;
    private int rxCredits;

    @Override
    public State getState() {
        return isReady() ? State.OPENED : State.CLOSED;
    }

    /**
     * Set the delegate to handle responses from async stream requests
     */
    @Override
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void open() {

    }

    @Override
    public void close() {

    }

    /**
     * Writes the byte-array data to the stream
     */
    @Override
    public void write(byte[] data) {
        if (isReady()) {
            if (!withFlowControl) {
                service.writeCharacteristic(fifo, data);
            } else if (txCredits > 0) {
                txCredits--;
                service.writeCharacteristic(fifo, data);
            } else {
                pendingData = data;
            }
        }
    }

    /**
     * Set the bluetooth low energy service to operate on.
     */
    public void setBluetoothService(BluetoothLeService service) {
        this.service = service;
    }

    /**
     * Sets the characteristic to be treated as SPS FIFO
     */
    public void setFifo(BluetoothGattCharacteristic fifo) {
        this.fifo = fifo;
        if (service != null) {
            service.setCharacteristicNotification(fifo, true);
        }
    }

    /**
     * Sets the characteristic to be treated as SPS credits
     */
    public void setCredits(BluetoothGattCharacteristic credits) {
        this.credits = credits;
    }

    /**
     * Sets whether to use SPS with or without flow control
     */
    public void toggleCreditsConnection(boolean enabled) {
        if (isReady()) {
            if (withFlowControl) {
                service.writeCharacteristic(credits, new byte[]{CLOSE_CONNECTION});
            }
            service.setCharacteristicNotification(fifo, false);
            service.setCharacteristicNotification(credits, false);
            withFlowControl = enabled;
            if (withFlowControl) service.setCharacteristicNotification(credits, true);
            service.setCharacteristicNotification(fifo, true);
            if (withFlowControl) {
                service.writeCharacteristic(credits, new byte[]{MAX_NUMBER_OF_CREDITS});
                txCredits = 0;
                rxCredits = MAX_NUMBER_OF_CREDITS;
            }
        }
    }

    /**
     * Request connection priority
     */
    public void connectionPrioRequest(int prio) {
        if (service != null) {
            service.connectionPrioRequest(prio);
        }
    }

    /**
     * Requests to negotiate MTU size
     */
    public void setMtuSize(int size) {
        if (service != null) {
            service.mtuRequest(size);
        }
    }

    /**
     * Callback for BluetoothLeService
     */
    public void notifyGattUpdate(UUID uuid, int type, byte[] data) {
        if (type == ITEM_TYPE_WRITE) {
            onCharacteristicWrite(uuid, data);
        } else if (type == ITEM_TYPE_NOTIFICATION) {
            onCharacteristicNotification(uuid, data);
        }
    }

    // callback to handle updates from peer
    private void onCharacteristicNotification(UUID uUid, byte[] data) {
        if (credits.getUuid().equals(uUid) && data[0] != CLOSE_CONNECTION) {
            txCredits += data[0];
            if (pendingData != null) {
                byte[] retryData = pendingData;
                pendingData = null;
                write(retryData);
            }
        } else if (fifo.getUuid().equals(uUid)) {
            rxCredits--;
            delegate.dataStreamRead(this, data);
            byte half = MAX_NUMBER_OF_CREDITS / 2;
            if (rxCredits <= half) {
                rxCredits += half;
                service.writeCharacteristic(credits, new byte[]{half});
            }
        }
    }

    // callback to handle successful writes to peer
    private void onCharacteristicWrite(UUID uUid, byte[] data) {
        if (fifo.getUuid().equals(uUid)) {
            delegate.dataStreamWrote(this, data);
        }
    }

    private boolean isReady() {
        return service != null && fifo != null && credits != null;
    }
}
