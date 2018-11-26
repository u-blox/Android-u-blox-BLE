package com.ublox.BLE.services;

import android.annotation.TargetApi;
import android.app.Service;
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
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.interfaces.BluetoothGattRepresentation;
import com.ublox.BLE.utils.BLEQueue;
import com.ublox.BLE.utils.GattAttributes;
import com.ublox.BLE.utils.PhyMode;
import com.ublox.BLE.utils.QueueItem;
import com.ublox.BLE.utils.UBloxDevice;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

/**
 * This service handles all the interaction with the BLE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName(); // Tag for logging

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGattRepresentation mBluetoothGatt;
    private LocalBroadcastManager mLocalBroadcastManager;

    private PhyMode txPhyMode = PhyMode.PHY_UNDEFINED;
    private PhyMode rxPhyMode = PhyMode.PHY_UNDEFINED;

    private int mConnectionState = STATE_DISCONNECTED;

    private final IBinder mBinder = new LocalBinder();

    private BLEQueue bleQueue = new BLEQueue();
    private boolean bleQueueIsFree = true;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =           "com.ublox.BLE.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =        "com.ublox.BLE.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.ublox.BLE.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =           "com.ublox.BLE.ACTION_DATA_AVAILABLE";
    public final static String ACTION_RSSI_UPDATE =              "com.ublox.BLE.ACTION_RSSI_UPDATE";
    public final static String ACTION_MTU_UPDATE =               "com.ublox.BLE.ACTION_MTU_UPDATE";
    public final static String ACTION_PHY_MODE_AVAILABLE =       "com.ublox.BLE.ACTION_PHY_MODE_OBTAINED";
    public final static String ACTION_DESCRIPTOR_WRITE =         "com.ublox.BLE.ACTION_DESCRIPTOR_WRITE";


    public final static String EXTRA_TYPE =             "com.ublox.BLE.EXTRA_TYPE";
    public final static String EXTRA_UUID =             "com.ublox.BLE.EXTRA_UUID";
    public final static String EXTRA_DATA =             "com.ublox.BLE.EXTRA_DATA";
    public final static String EXTRA_RSSI =             "com.ublox.BLE.EXTRA_RSSI";
    public final static String EXTRA_MTU =              "com.ublox.BLE.EXTRA_MTU";
    public final static String EXTRA_STATUS =           "com.ublox.BLE.EXTRA_STATUS";
    public final static String EXTRA_IS_PHY_UPDATE =    "com.ublox.BLE.EXTRA_IS_PHY_UPDATE";

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if(mBluetoothAdapter.isLe2MPhySupported()) {
                            mBluetoothGatt.readPhy();
                        }
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
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, BLEQueue.ITEM_TYPE_READ);
            bleQueueIsFree = true;
            processQueue();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, BLEQueue.ITEM_TYPE_NOTIFICATION);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(GattAttributes.UUID_CHARACTERISTIC_FIFO.equals(descriptor.getCharacteristic().getUuid().toString())) {
                broadcastUpdate(ACTION_DESCRIPTOR_WRITE);
            }
            bleQueueIsFree = true;
            processQueue();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, BLEQueue.ITEM_TYPE_WRITE);
            bleQueueIsFree = true;
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

    /**
     * Sends a broadcast to registered receivers
     * @param action The Intent action that are going to be sent in the broadcast
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Sends a broadcast to registered receivers with the current rssi
     * @param rssi The current rssi
     */
    private void broadcastRssi(int rssi) {
        Intent intent = new Intent(ACTION_RSSI_UPDATE);
        intent.putExtra(EXTRA_RSSI, rssi);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastMtu(int mtu, int status) {
        Intent intent = new Intent(ACTION_MTU_UPDATE);
        intent.putExtra(EXTRA_MTU, mtu);
        intent.putExtra(EXTRA_STATUS, status);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    @RequiresApi(26)
    private void broadcastPhyModeAvailable(boolean isUpdate) {
        Intent intent = new Intent(ACTION_PHY_MODE_AVAILABLE);
        intent.putExtra(EXTRA_IS_PHY_UPDATE, isUpdate);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    /**
     *
     * @param action The action of the broadcast intent
     * @param characteristic The characteristics that it is about
     * @param itemType Item type from the class BLEQueue
     */
    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic, int itemType) {
        Intent intent = new Intent(action);

        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_TYPE, itemType);

        mLocalBroadcastManager.sendBroadcast(intent);
    }

    public BluetoothGattRepresentation getGatt() {
        return mBluetoothGatt;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Close to connection
        close();
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mHandler.postDelayed(rCheckRssi, 2000);
        return true;
    }

    // Code to receive rssi from connected device every two seconds
    Handler mHandler = new Handler();
    Runnable rCheckRssi = new Runnable() {
        @Override
        public void run() {
            if (mBluetoothGatt != null && mConnectionState == STATE_CONNECTED && bleQueue.hasItems() == 0) {
                try {
                    mBluetoothGatt.readRemoteRssi();
                } catch (Exception e) {}
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
        bleQueueIsFree = true;
        String address = device.getAddress();

        // If the address or bluetoothadapter is null we cant connect
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        // Checks if we successfully connected
        /*final BluetoothDevice rawDevice = mBluetoothAdapter.getRemoteDevice(address);
        if (rawDevice == null) {
            return false;
        }
        BluetoothDeviceRepresentation device = new UBloxDevice(rawDevice);*/

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mBluetoothAdapter.isLe2MPhySupported()) {
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback, TRANSPORT_LE,
                        BluetoothDevice.PHY_LE_CODED_MASK ^ BluetoothDevice.PHY_LE_2M_MASK);
            } else {
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback, TRANSPORT_LE,
                        BluetoothDevice.PHY_LE_CODED_MASK ^ BluetoothDevice.PHY_LE_1M_MASK);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback, TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }
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
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
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
            bleQueue.addRead(characteristic);
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
            bleQueue.addNotification(characteristic, enabled);
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
            bleQueue.addWrite(characteristic, data);
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
                return;
        }
    }

    /**
     * Function that is handling the request queue.
     * To think about is that BLE on Android only can handle one request at the time.
     * Android do not handle this by itself..
     */
    private void processQueue() {
        if (bleQueueIsFree) {
            bleQueueIsFree = false;
            QueueItem queueItem = bleQueue.getNextItem();
            if (queueItem == null) {
                bleQueueIsFree = true;
            } else {
                boolean status = false;
                switch (queueItem.itemType) {
                    case BLEQueue.ITEM_TYPE_READ:
                        if (mBluetoothGatt != null) {
                            status = mBluetoothGatt.readCharacteristic(queueItem.characteristic);
                        }
                        break;
                    case BLEQueue.ITEM_TYPE_WRITE:
                        if (mBluetoothGatt != null) {
                            status = mBluetoothGatt.writeCharacteristic(queueItem.characteristic);
                        }
                        break;
                    case BLEQueue.ITEM_TYPE_NOTIFICATION:
                        BluetoothGattDescriptor descriptor = queueItem.characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                        if (descriptor != null && mBluetoothGatt != null) {
                            byte[] value = descriptor.getValue();
                            if (value == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) {
                                mBluetoothGatt.setCharacteristicNotification(queueItem.characteristic, true);
                            } else if (value == BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) {
                                mBluetoothGatt.setCharacteristicNotification(queueItem.characteristic, false);
                            }
                            status = mBluetoothGatt.writeDescriptor(descriptor);
                        } else {
                            status = false;
                        }
                        break;
                }
                if (!status) {
                    bleQueueIsFree = true;
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
}