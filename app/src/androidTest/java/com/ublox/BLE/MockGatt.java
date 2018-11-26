package com.ublox.BLE;

import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;

import com.ublox.BLE.interfaces.BluetoothGattRepresentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.PHY_LE_1M;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class MockGatt implements BluetoothGattRepresentation {
    private BluetoothGattCallback callback;
    private Handler mainHandler;
    private Set<BluetoothGattCharacteristic> notifications;
    private boolean connected;
    private List<byte[]> written;
    public final BluetoothGattService sps;
    private BluetoothGattService access;
    private BluetoothGattService deviceId;
    public final BluetoothGattCharacteristic fifo;
    public final BluetoothGattCharacteristic credits;

    public MockGatt(BluetoothGattCallback callback) {
        this.callback = callback;
        notifications = new HashSet<>();
        mainHandler = new Handler(Looper.getMainLooper());
        connected = false;
        written = new ArrayList<>();

        int properties = PROPERTY_INDICATE | PROPERTY_NOTIFY | PROPERTY_WRITE | PROPERTY_WRITE_NO_RESPONSE;
        int permissions = PERMISSION_WRITE | PERMISSION_READ;
        BluetoothGattDescriptor config = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), permissions);
        fifo = new BluetoothGattCharacteristic(UUID.fromString("2456e1b9-26e2-8f83-e744-f34f01e9d703"), properties, permissions);
        fifo.addDescriptor(config);
        credits = new BluetoothGattCharacteristic(UUID.fromString("2456e1b9-26e2-8f83-e744-f34f01e9d704"), properties, permissions);
        credits.addDescriptor(config);
        sps = new BluetoothGattService(UUID.fromString("2456e1b9-26e2-8f83-e744-f34f01e9d701"), SERVICE_TYPE_PRIMARY);
        sps.addCharacteristic(fifo);
        sps.addCharacteristic(credits);

        properties = PROPERTY_READ;
        permissions = PERMISSION_READ;

        BluetoothGattCharacteristic manufacturer = new BluetoothGattCharacteristic(UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"), properties, permissions);
        manufacturer.setValue("u-blox");
        BluetoothGattCharacteristic model = new BluetoothGattCharacteristic(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb"), properties, permissions);
        model.setValue("TEST-T0");
        deviceId = new BluetoothGattService(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"), SERVICE_TYPE_PRIMARY);
        deviceId.addCharacteristic(manufacturer);
        deviceId.addCharacteristic(model);

        BluetoothGattCharacteristic name = new BluetoothGattCharacteristic(UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"), properties, permissions);
        name.setValue("TEST-T0-012345");
        access = new BluetoothGattService(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"), SERVICE_TYPE_PRIMARY);
        access.addCharacteristic(name);
    }

    @Override
    public void close() {
        disconnect();
    }

    @Override
    public boolean connect() {
        connected = true;
        mainHandler.post(() -> {
           callback.onConnectionStateChange(null, GATT_SUCCESS, STATE_CONNECTED);
        });
        return true;
    }

    @Override
    public void disconnect() {
        connected = false;
        mainHandler.post(() -> {
            callback.onConnectionStateChange(null, GATT_SUCCESS, STATE_DISCONNECTED);
        });
    }

    @Override
    public boolean discoverServices() {
        mainHandler.post(() -> {
            callback.onServicesDiscovered(null, GATT_SUCCESS);
        });
        return true;
    }

    @Override
    public List<BluetoothGattService> getServices() {
        List<BluetoothGattService> services = new ArrayList<>();
        services.add(sps);
        services.add(access);
        services.add(deviceId);
        return services;
    }

    @Override
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        mainHandler.post(() -> {
            callback.onCharacteristicRead(null, characteristic, GATT_SUCCESS);
        });
        return true;
    }

    @Override
    public void readPhy() {
        mainHandler.post(()-> {
            callback.onPhyRead(null, PHY_LE_1M, PHY_LE_1M, GATT_SUCCESS);
        });
    }

    @Override
    public boolean readRemoteRssi() {
        mainHandler.post(() ->{
            callback.onReadRemoteRssi(null, -50, GATT_SUCCESS);
        });
        return true;
    }

    @Override
    public boolean requestConnectionPriority(int connectionParameter) {
        return true;
    }

    @Override
    public boolean requestMtu(int size) {
        if (!connected) return false;
        if (size < 23) return true;
        if (size > 247) size = 247;
        final int s = size;
        mainHandler.post(() -> {
           callback.onMtuChanged(null, s, GATT_SUCCESS);
        });
        return true;
    }

    @Override
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (enable) {
            notifications.add(characteristic);
        } else {
            notifications.remove(characteristic);
        }
        return true;
    }

    @Override
    public void setPreferredPhy(int txPhy, int rxPhy, int phyOptions) {
        mainHandler.post(() -> {
           callback.onPhyUpdate(null, txPhy, rxPhy, GATT_SUCCESS);
        });
    }

    @Override
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!connected) return false;
        mainHandler.post(() -> {
            callback.onCharacteristicWrite(null, characteristic, GATT_SUCCESS);
        });
        written.add(characteristic.getValue());
        return true;
    }

    @Override
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        mainHandler.post(() -> {
            callback.onDescriptorWrite(null, descriptor, GATT_SUCCESS);
        });
        return true;
    }

    public void notifyCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!notifications.contains(characteristic)) return;
        mainHandler.post(()-> {
            callback.onCharacteristicChanged(null, characteristic);
        });
    }

    public List<byte[]> getWritten() {
        return written;
    }
}
