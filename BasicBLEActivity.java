package com.ublox.BLE.activities;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.ublox.BLE.R;
import com.ublox.BLE.services.BluetoothLeService;

import java.util.List;

public class BasicBLEActivity extends Activity {

    private static final String DEVICE_UUID = ""; // The UUID for the device that we wanna connect to.

    private boolean mConnected = false; // True if we are connected to a device.
    private BluetoothLeService mBluetoothLeService;
    private List<BluetoothGattService> mServices; // Were we save all services that the device have

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_ble);

        // Bind service with the activity
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register the receiver for the service to be able ro receive data
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            mBluetoothLeService.connect(DEVICE_UUID);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disconnect from the device and close the connection
        // Try/Catch because Androids Bluetooth implementation may crash on some devices.
        try {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            mConnected = false;
        } catch (Exception ignore) {}
        // Unregister the receiver
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service so that it can shutdown properly
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    /**
     * Tells the service that we wanna read some data
     * @param characteristic The characteristic that we wanna read
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        try {
            mBluetoothLeService.readCharacteristic(characteristic);
        } catch (Exception ignore) {}
    }

    /**
     * Function to tell the service that we wanna write some data to the device
     * @param characteristic The characteristic that we wanna write to
     * @param value The value that we wanna write to the characteristic
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        try {
            mBluetoothLeService.writeCharacteristic(characteristic, value);
        } catch (Exception ignore) {}
    }

    /**
     * Tells the service that we want notifications for the characteristic
     * @param characteristic The characteristic that we want to get notified about
     */
    public void notifyCharacteristic(BluetoothGattCharacteristic characteristic) {
        try {
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
        } catch (Exception ignore) {}
    }

    /**
     * Function for sending a message to the serial service on the device
     * @param characteristic The fifo characteristic
     * @param message The message that are going to be sent as a byte array
     */
    public void sendMessageToSerial(BluetoothGattCharacteristic characteristic, byte[] message) {
        try {
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
            mBluetoothLeService.writeCharacteristic(characteristic, message);
        } catch (Exception ignore) {}
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(DEVICE_UUID);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mServices = mBluetoothLeService.getSupportedGattServices();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                byte[] extraData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA); // The data that we received from the device

            } else if (BluetoothLeService.ACTION_RSSI_UPDATE.equals(action)) {
                int rssi = intent.getIntExtra(BluetoothLeService.EXTRA_RSSI, 0); // Device current rssi
            }
        }
    };

    /**
     * This is were we create the intent filter that will tell the service what we are interested in.
     * @return IntentFilter for handling the BLEService
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_UPDATE);
        return intentFilter;
    }

}
