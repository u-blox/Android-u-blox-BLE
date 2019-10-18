package com.ublox.BLE.services;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.interfaces.BluetoothGattRepresentation;
import com.ublox.BLE.utils.BLEQueue;
import com.ublox.BLE.utils.GattAttributes;
import com.ublox.BLE.utils.PhyMode;

import java.util.List;
import java.util.UUID;

/**
 * This service handles all the interaction with the BLE device.
 */
public class BluetoothLeService {
    private final static String TAG = BluetoothLeService.class.getSimpleName(); // Tag for logging

    private boolean initiated;
    private boolean supports2MPhy;
    private String mBluetoothDeviceAddress;
    private BluetoothGattRepresentation mBluetoothGatt;
    private Receiver mReceiver;

    private PhyMode txPhyMode = PhyMode.PHY_UNDEFINED;
    private PhyMode rxPhyMode = PhyMode.PHY_UNDEFINED;

    private int mConnectionState = STATE_DISCONNECTED;

    private BLEQueue bleQueue = new BLEQueue();
    private boolean readyForRequest = true;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private final static String ACTION_GATT_CONNECTED =           "com.ublox.BLE.ACTION_GATT_CONNECTED";
    private final static String ACTION_GATT_DISCONNECTED =        "com.ublox.BLE.ACTION_GATT_DISCONNECTED";
    private final static String ACTION_GATT_SERVICES_DISCOVERED = "com.ublox.BLE.ACTION_GATT_SERVICES_DISCOVERED";
    private final static String ACTION_DESCRIPTOR_WRITE =         "com.ublox.BLE.ACTION_DESCRIPTOR_WRITE";

    public static final int ITEM_TYPE_NOTIFICATION = 2;
    public static final int ITEM_TYPE_READ = 1;
    public static final int ITEM_TYPE_WRITE = 0;
    // Implements callback methods for GATT events that the app cares about. For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                // Attempts to discover services after successful connection.
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.discoverServices();
                    if (supports2MPhy) {
                        mBluetoothGatt.readPhy();
                    }
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(characteristic, ITEM_TYPE_READ);
            readyForRequest = true;
            processQueue();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(characteristic, ITEM_TYPE_NOTIFICATION);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(GattAttributes.UUID_CHARACTERISTIC_FIFO.equals(descriptor.getCharacteristic().getUuid().toString())) {
                broadcastUpdate(ACTION_DESCRIPTOR_WRITE);
            }
            readyForRequest = true;
            processQueue();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(characteristic, ITEM_TYPE_WRITE);
            readyForRequest = true;
            processQueue();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            broadcastRssi(rssi);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            broadcastMtu(mtu, status);
        }

        @RequiresApi(26)
        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            updateModes(status, txPhy, rxPhy, false);
        }

        @RequiresApi(26)
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            updateModes(status, txPhy, rxPhy, true);
        }

        @RequiresApi(26)
        private void updateModes(int status, int txPhy, int rxPhy, boolean isUpdate) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setTxPhyMode(txPhy);
                setRxPhyMode(rxPhy);
                broadcastPhyModeAvailable(isUpdate);
            }
        }
    };
    private Context mContext;

    /**
     * Sends a broadcast to registered receivers
     * @param action The Intent action that are going to be sent in the broadcast
     */
    private void broadcastUpdate(final String action) {
        if (mReceiver == null) return;
        switch (action) {
            case ACTION_GATT_CONNECTED:
                mReceiver.onGattConnected();
                break;
            case ACTION_GATT_DISCONNECTED:
                mReceiver.onGattDisconnected();
                break;
            case ACTION_GATT_SERVICES_DISCOVERED:
                mReceiver.onServicesDiscovered();
                break;
            case ACTION_DESCRIPTOR_WRITE:
                mReceiver.onDescriptorWrite();
                break;
        }
    }

    /**
     * Sends a broadcast to registered receivers with the current rssi
     * @param rssi The current rssi
     */
    private void broadcastRssi(int rssi) {
        if (mReceiver == null) return;
        mReceiver.onRssiUpdate(rssi);
    }

    private void broadcastMtu(int mtu, int status) {
        if (mReceiver == null) return;
        mReceiver.onMtuUpdate(mtu, status);
    }

    @RequiresApi(26)
    private void broadcastPhyModeAvailable(boolean isUpdate) {
        if (mReceiver == null) return;
        mReceiver.onPhyAvailable(isUpdate);
    }

    /**
     *  @param characteristic The characteristics that it is about
     * @param itemType Item type from the class BLEQueue
     */
    private void broadcastUpdate(BluetoothGattCharacteristic characteristic, int itemType) {
        if (mReceiver == null) return;
        UUID uuid = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        mReceiver.onDataAvailable(uuid, itemType, data);
    }

    public BluetoothGattRepresentation getGatt() {
        return mBluetoothGatt;
    }

    public boolean initialize(Context context) {
        if (initiated) return true;

        mContext = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return false;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        initiated = true;
        supports2MPhy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bluetoothAdapter.isLe2MPhySupported();
        mHandler.postDelayed(rCheckRssi, 2000);
        return true;
    }

    public void register(Receiver receiver) {
        mReceiver = receiver;
    }

    public void unregister() {
        mReceiver = null;
    }

    // Code to receive rssi from connected device every two seconds
    private Handler mHandler = new Handler();
    private Runnable rCheckRssi = new Runnable() {
        @Override
        public void run() {
            if (mBluetoothGatt != null && mConnectionState == STATE_CONNECTED && bleQueue.hasItems() == 0) {
                try {
                    mBluetoothGatt.readRemoteRssi();
                } catch (Exception ignored) {} // Todo: What exception can even occur here?
            }
            mHandler.postDelayed(rCheckRssi, 2000);
        }
    };

    /**
     * Connect to BLE device
     *
     * @param device The device to connect
     *
     * @return Return true if the connection is initiated successfully.
     */
    public boolean connect(BluetoothDeviceRepresentation device) {
        // Init the request queue for this device
        bleQueue = new BLEQueue();
        readyForRequest = true;
        String address = device.getAddress();

        // If the address or bluetoothadapter is null we cant connect
        if (!initiated || address == null) {
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        mBluetoothGatt = device.connectGatt(mContext, mGattCallback, supports2MPhy);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * BluetoothGattCallback.onConnectionStateChange(BluetoothGatt, int, int)
     * callback.
     */
    public void disconnect() {
        if (!initiated || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given BluetoothGattCharacteristic. The read result is reported
     * asynchronously through the BluetoothGattCallback.onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            bleQueue.addAction(() -> mBluetoothGatt != null && mBluetoothGatt.readCharacteristic(characteristic));
        }
        processQueue();
    }

    /**
     * Request a notifications on a given BluetoothGattCharacteristic. The read result is reported
     * asynchronously through the BluetoothGattCallback.onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic)
     * callback.
     *
     * @param characteristic The characteristic to read from.
     * @param enabled true to enable notifications, false to disable
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            bleQueue.addAction(() -> {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                if (descriptor == null || mBluetoothGatt == null) return false;

                // Set write type to WRITE_TYPE_DEFAULT to fix an issue seen on some phone models and
                // Android versions that enable notification with a write_command message instead of a
                // write_request message (as specified by the Bluetooth specification).
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
                return mBluetoothGatt.writeDescriptor(descriptor);
            });
        }
        processQueue();
    }

    /**
     * Request a notifications on a given BluetoothGattCharacteristic. The read result is reported
     * asynchronously through the BluetoothGattCallback.onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic)
     * callback.
     *
     * @param characteristic The characteristic to read from.
     * @param data byte array with the data to write
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            bleQueue.addAction(() -> {
                if (mBluetoothGatt == null) return false;
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                characteristic.setValue(data);
                return  mBluetoothGatt.writeCharacteristic(characteristic);
            });
        }
        processQueue();
    }

    @TargetApi(26)
    public void setPreferredPhy(PhyMode mode) {
        switch (mode) {
            case PHY_1M:
                mBluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_1M_MASK,
                        BluetoothDevice.PHY_LE_1M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED);
                return;
            case PHY_2M:
                mBluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M_MASK,
                        BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED);
                return;
            default:
        }
    }

    /**
     * Function that is handling the request queue.
     * To think about is that BLE on Android only can handle one request at the time.
     * Android do not handle this by itself..
     */
    private void processQueue() {
        if (readyForRequest) {
            readyForRequest = false;
            BLEQueue.QueueAction queueItem = bleQueue.getNextItem();
            if (queueItem == null) {
                readyForRequest = true;
            } else {
                if (!queueItem.execute()) {
                    readyForRequest = true;
                }
            }
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after BluetoothGatt.discoverServices() completes successfully.
     *
     * A List of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void mtuRequest(int size) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothGatt != null) {
            mBluetoothGatt.requestMtu(size);
        }
    }

    public void connectionPrioRequest(int connectionParameter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothGatt != null) {
            mBluetoothGatt.requestConnectionPriority(connectionParameter);
        }
    }

    public PhyMode getTxPhyMode() {
        return txPhyMode;
    }

    private void setTxPhyMode(int txPhy) {
        this.txPhyMode = PhyMode.fromInteger(txPhy);
    }

    public PhyMode getRxPhyMode() {
        return rxPhyMode;
    }

    private void setRxPhyMode(int rxPhy) {
        this.rxPhyMode = PhyMode.fromInteger(rxPhy);
    }

    public interface Receiver {
        void onDescriptorWrite();
        void onPhyAvailable(boolean isUpdate);
        void onMtuUpdate(int mtu, int status);
        void onRssiUpdate(int rssi);
        void onDataAvailable(UUID uUid, int type, byte[] data);
        void onServicesDiscovered();
        void onGattDisconnected();
        void onGattConnected();
    }
}