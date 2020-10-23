package com.ublox.BLE.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import com.ublox.BLE.utils.GattAttributes;
import com.ublox.BLE.utils.UBloxDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.PHY_LE_1M_MASK;
import static android.bluetooth.BluetoothDevice.PHY_LE_2M_MASK;
import static android.bluetooth.BluetoothDevice.PHY_LE_CODED_MASK;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED;
import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH;
import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class BluetoothDevice extends BluetoothGattCallback implements BluetoothPeripheral {
    static final UUID CCCD = UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG);

    private android.bluetooth.BluetoothDevice device;
    private BluetoothGatt connection;
    private Map<UUID, byte[]> serviceData;
    private Map<UUID, BluetoothGattCharacteristic> characteristicCache;
    private ActionQueue queue;
    private State state;
    private int rssi;
    private int preferredMtu;
    private int maximumData;
    private Delegate delegate;

    public BluetoothDevice(android.bluetooth.BluetoothDevice device) {
        this.device = device;
        state = State.DISCONNECTED;
        preferredMtu = 23;
        maximumData = 20;
        serviceData = new HashMap<>();
        characteristicCache = new HashMap<>();
        queue = new ActionQueue();
    }

    public void updateAdvertisement(int rssi, Map<UUID, byte[]> services) {
        this.rssi = rssi;
        for (UUID uuid: services.keySet()) {
            byte[] data = serviceData.get(uuid);
            if (data == null || data.length == 0) {
                serviceData.put(uuid, services.get(uuid));
            }
        }
    }

    @Override
    public String identifier() {
        return device.getAddress();
    }

    @Override
    public String name() {
        return device.getName();
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public int bondState() {
        return device.getBondState();
    }

    @Override
    public int rssi() {
        return rssi;
    }

    @Override
    public boolean advertisedService(UUID service) {
        return serviceData.containsKey(service);
    }

    @Override
    public byte[] serviceDataFor(UUID service) {
        byte[] data = serviceData.get(service);
        return data != null ? Arrays.copyOf(data, data.length) : new byte[0];
    }

    @Override
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void connect() {
        if (connection != null) return;

        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= Build.VERSION_CODES.O) {
            connection = device.connectGatt(null, true, this, TRANSPORT_LE, PHY_LE_1M_MASK | PHY_LE_2M_MASK | PHY_LE_CODED_MASK);
        } else if (sdkInt >= Build.VERSION_CODES.M) {
            connection = device.connectGatt(null, true, this, TRANSPORT_LE);
        } else {
            connection = device.connectGatt(null, true, this);
        }

        if (attemptConnection()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (state == State.CONNECTED) return;
                connection.disconnect();
                attemptConnection();
            }, 30000);
        }
    }

    private boolean attemptConnection() {
        if (connection.connect()) {
            return true;
        } else {
            failConnection();
            return false;
        }
    }

    private void failConnection() {
        cleanupConnection();
        new Handler(Looper.getMainLooper()).post(() -> setState(State.ERROR));
    }

    private void cleanupConnection() {
        if (connection == null) return;
        connection.close();
        connection = null;
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public void discover() {
        if (state == State.CONNECTED) {
            queue.enqueue(() -> connection.discoverServices());
        }
    }

    @Override
    public List<UUID> services() {
        List<UUID> result = new ArrayList<>();
        if (state != State.CONNECTED) return result;

        for (BluetoothGattService service: connection.getServices()) {
            result.add(service.getUuid());
        }

        return result;
    }

    @Override
    public List<UUID> characteristics(UUID service) {
        List<UUID> result = new ArrayList<>();
        if (state != State.CONNECTED) return result;

        if (service == null) {
            result.addAll(characteristicCache.keySet());
        } else {
            BluetoothGattService gattService = connection.getService(service);
            if (gattService == null) return result;

            for (BluetoothGattCharacteristic characteristic: gattService.getCharacteristics()) {
                result.add(characteristic.getUuid());
            }
        }

        return result;
    }

    @Override
    public void set(UUID characteristic, boolean notify) {
        BluetoothGattCharacteristic gatt = characteristicCache.get(characteristic);
        if (gatt == null) return;
        BluetoothGattDescriptor descriptor = gatt.getDescriptor(CCCD);
        if (descriptor == null) return;
        queue.enqueue(() -> {
            descriptor.setValue(notify ? ENABLE_NOTIFICATION_VALUE : DISABLE_NOTIFICATION_VALUE);
            return connection.writeDescriptor(descriptor);
        });
    }

    @Override
    public void write(UUID characteristic, byte[] data, boolean withResponse) {
        BluetoothGattCharacteristic gatt = characteristicCache.get(characteristic);
        if (gatt == null) return;
        queue.enqueue(() -> {
            gatt.setValue(Arrays.copyOf(data, data.length));
            gatt.setWriteType(withResponse ? WRITE_TYPE_DEFAULT : WRITE_TYPE_NO_RESPONSE);
            return connection.writeCharacteristic(gatt);
        });

    }

    @Override
    public void read(UUID characteristic) {
        BluetoothGattCharacteristic gatt = characteristicCache.get(characteristic);
        if (gatt != null) {
            queue.enqueue(() -> connection.readCharacteristic(gatt));
        }
    }

    @Override
    public void readRssi() {
        if (state == State.CONNECTED) {
            queue.enqueue(() -> connection.readRemoteRssi());
        }
    }

    @Override
    public void setPreferredMtu(int mtu) {
        preferredMtu = mtu;
    }

    @Override
    public void requestConnectionPriority(Priority priority) {
        if (state == State.CONNECTED && Build.VERSION.SDK_INT >= 21) {
            int prio;
            switch (priority) {
                case HIGH: prio = CONNECTION_PRIORITY_HIGH; break;
                case LOW: prio = CONNECTION_PRIORITY_LOW_POWER; break;
                default: prio = CONNECTION_PRIORITY_BALANCED;
            }
            queue.enqueue(() -> {
                connection.requestConnectionPriority(prio);
                return false;
            });
        }
    }

    @Override
    public int maximumDataCount(boolean withResponse) {
        return maximumData;
    }

    private void setState(State newState) {
        if (state == newState) return;
        state = newState;
        if (delegate != null) delegate.bluetoothPeripheralChangedState(this);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == GATT_SUCCESS && newState == STATE_CONNECTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                setState(State.CONNECTED);
            } else {
                queue.enqueue(() -> connection.requestMtu(preferredMtu));
            }
        } else {
            if (status != GATT_SUCCESS || newState == STATE_DISCONNECTED) {
                cleanupConnection();
            }
            characteristicCache.clear();
            setState(status != GATT_SUCCESS ? State.ERROR : State.DISCONNECTED);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (status == GATT_SUCCESS) {
            maximumData = mtu - 3;
        }
        setState(State.CONNECTED);
        queue.finishCurrent();
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        for (BluetoothGattService service: gatt.getServices()) {
            for (BluetoothGattCharacteristic characteristic: service.getCharacteristics()) {
                characteristicCache.put(characteristic.getUuid(), characteristic);
            }
        }
        if (delegate != null) delegate.bluetoothPeripheralDiscovered(this, status == GATT_SUCCESS);
        queue.finishCurrent();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        boolean ok = status == GATT_SUCCESS;
        BluetoothGattCharacteristic c = descriptor.getCharacteristic();
        boolean enabled = Arrays.equals(descriptor.getValue(), ENABLE_NOTIFICATION_VALUE);
        if (ok) {
            ok = connection.setCharacteristicNotification(c, enabled);
        }
        if (delegate != null) delegate.bluetoothPeripheralSet(this, c.getUuid(), enabled, ok);
        queue.finishCurrent();
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        boolean ok = status == GATT_SUCCESS;
        byte[] data = ok ? characteristic.getValue() : new byte[0];
        if (delegate != null) delegate.bluetoothPeripheralRead(this, characteristic.getUuid(), data, ok);
        queue.finishCurrent();
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (delegate != null) delegate.bluetoothPeripheralRead(this, characteristic.getUuid(), characteristic.getValue(), true);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (delegate != null) delegate.bluetoothPeripheralWrote(this, characteristic.getUuid(), status == GATT_SUCCESS);
        queue.finishCurrent();
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        boolean ok = status == GATT_SUCCESS;
        if (ok) {
            this.rssi = rssi;
        }
        if (delegate != null) delegate.bluetoothPeripheralReadRssi(this, ok);
        queue.finishCurrent();
    }

    @Override
    public int describeContents() {
        return device.describeContents();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        device.writeToParcel(parcel, flags);
        parcel.writeInt(rssi);
        parcel.writeInt(serviceData.size());
        for (UUID uuid : serviceData.keySet()) {
            ParcelUuid puuid = new ParcelUuid(uuid);
            puuid.writeToParcel(parcel, flags);
            byte[] data = serviceDataFor(uuid);
            parcel.writeInt(data.length);
            parcel.writeByteArray(data);
        }
    }

    public static Parcelable.Creator<BluetoothDevice> CREATOR = new Parcelable.Creator<BluetoothDevice>() {

        @Override
        public BluetoothDevice createFromParcel(Parcel parcel) {
            BluetoothDevice device = new BluetoothDevice(
                android.bluetooth.BluetoothDevice.CREATOR.createFromParcel(parcel)
            );
            device.rssi = parcel.readInt();
            int n = parcel.readInt();
            for (int i = 0; i < n; i++) {
                ParcelUuid puuid = ParcelUuid.CREATOR.createFromParcel(parcel);
                int length = parcel.readInt();
                byte[] data = new byte[length];
                parcel.readByteArray(data);
                device.serviceData.put(puuid.getUuid(), data);
            }

            return device;
        }

        @Override
        public BluetoothDevice[] newArray(int size) {
            return new BluetoothDevice[size];
        }
    };

    @Deprecated
    public UBloxDevice toUbloxDevice() {
        return new UBloxDevice(device);
    }

    private class ActionQueue {
        private Queue<Action> queue;
        private boolean busy;

        public ActionQueue() {
            queue = new LinkedList<>();
            busy = false;
        }

        public synchronized void enqueue(Action action) {
            queue.offer(action);
            startNext();
        }

        public synchronized void finishCurrent() {
            busy = false;
            startNext();
        }

        private void startNext() {
            if (busy) return;
            Action action = queue.poll();
            if (action == null) return;
            busy = action.execute();
            startNext();
        }
    }

    private interface Action {
        boolean execute();
    }
}
