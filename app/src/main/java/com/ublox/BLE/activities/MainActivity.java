package com.ublox.BLE.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.ublox.BLE.R;
import com.ublox.BLE.fragments.ChatFragment;
import com.ublox.BLE.fragments.OverviewFragment;
import com.ublox.BLE.fragments.ServicesFragment;
import com.ublox.BLE.services.BluetoothLeService;
import com.ublox.BLE.utils.BLEQueue;
import com.ublox.BLE.utils.GattAttributes;


public class MainActivity extends Activity implements ActionBar.TabListener, OverviewFragment.IOverviewFragmentInteraction, ServicesFragment.IServiceFragmentInteraction, ChatFragment.IChatInteractionListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICES = "device";

    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private int currentDevice = 0;

    private ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
    private BluetoothLeService mBluetoothLeService;
    private static boolean mConnected = false;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private BluetoothGattCharacteristic characteristicRedLED;
    private BluetoothGattCharacteristic characteristicGreenLED;

    private BluetoothGattCharacteristic characteristicFifo;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDevices.get(currentDevice).getAddress());
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
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                sendToActiveFragment(mBluetoothLeService.getSupportedGattServices());
                for (BluetoothGattService service : mBluetoothLeService.getSupportedGattServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String uuid = characteristic.getUuid().toString();
                        if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_RANGE)) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                            } catch (Exception ignore) {}
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_GREEN_LED)) {
                            try {
                                characteristicGreenLED = characteristic;
                                mBluetoothLeService.readCharacteristic(characteristic);
                            } catch (Exception ignore) {}
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RED_LED)) {
                            try {
                                characteristicRedLED = characteristic;
                                mBluetoothLeService.readCharacteristic(characteristic);
                            } catch (Exception ignore) {}
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_X)
                                || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Y)
                                || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Z)
                                || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_BATTERY_LEVEL)
                                || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TEMP_VALUE)
                                ) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                            } catch (Exception ignore) {}
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_FIFO)) {
                            characteristicFifo = characteristic;
                            sendToActiveFragment(characteristicFifo);
                        }
                    }
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                String extraUuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                int extraType = intent.getIntExtra(BluetoothLeService.EXTRA_TYPE, -1);
                byte[] extraData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                sendToActiveFragment(extraUuid, extraType, extraData);

            } else if (BluetoothLeService.ACTION_RSSI_UPDATE.equals(action)) {
                int rssi = intent.getIntExtra(BluetoothLeService.EXTRA_RSSI, 0);
                sendToActiveFragment(rssi);
            }
        }
    };

    private List<BluetoothGattService> mServices;

    private void sendToActiveFragment(List<BluetoothGattService> services) {
        mServices = services;
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (ServicesFragment.class.isInstance(fragment)) {
            ServicesFragment servicesFragment = (ServicesFragment) fragment;
            servicesFragment.displayGattServices(services);
        }
    }

    private void sendToActiveFragment(BluetoothGattCharacteristic characteristic) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (ChatFragment.class.isInstance(fragment)) {
            ChatFragment chatFragment = (ChatFragment) fragment;
            chatFragment.setCharacteristicFifo(characteristic);
        }
    }

    private void sendToActiveFragment(int rssi) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (OverviewFragment.class.isInstance(fragment)) {
            View v = fragment.getView();
            ((TextView) v.findViewById(R.id.tvRSSI)).setText(String.format("%d", rssi));
        }
    }

    private void sendToActiveFragment(String uuid, int type, byte[] data) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (OverviewFragment.class.isInstance(fragment)) {
            View v = fragment.getView();

            if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
                ((TextView) v.findViewById(R.id.tvBatteryLevel)).setText(String.format("%d", data[0]));
            }

            if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TEMP_VALUE)) {
                ((TextView) v.findViewById(R.id.tvTemperature)).setText(String.format("%d", data[0]));
            }

            if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_RANGE)) {
                ((TextView) v.findViewById(R.id.tvAccelerometerRange)).setText(String.format("+-%d", data[0]));
            }

            if (type == BLEQueue.ITEM_TYPE_READ) {

                StringBuilder stringBuilder = new StringBuilder(data.length);

                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_GREEN_LED)) {
                    Switch sGreen = (Switch) v.findViewById(R.id.sGreenLight);
                    if (data[0]  == 0) {
                        sGreen.setChecked(false);
                    } else {
                        sGreen.setChecked(true);
                    }
                } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RED_LED)) {
                    Switch sRed = (Switch) v.findViewById(R.id.sRedLight);
                    if (data[0]  == 0) {
                        sRed.setChecked(false);
                    } else {
                        sRed.setChecked(true);
                    }
                }

            } else if (type == BLEQueue.ITEM_TYPE_NOTIFICATION) {
                if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_X)) {
                    ((ProgressBar) v.findViewById(R.id.pbX)).setProgress(data[0] + 128);
                } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Y)) {
                    ((ProgressBar) v.findViewById(R.id.pbY)).setProgress(data[0] + 128);
                } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Z)) {
                    ((ProgressBar) v.findViewById(R.id.pbZ)).setProgress(data[0] + 128);
                }
            }
        } else if (ServicesFragment.class.isInstance(fragment)) {
            View v = fragment.getView();
            Log.i("SERVICE", "gotData, uuid: " + uuid);
            Log.i("SERVICE", "wanted __UUID: " + ((ServicesFragment) fragment).currentUuid);


            if (((ServicesFragment) fragment).currentUuid.equals(uuid)) {
                StringBuilder stringBuilder = new StringBuilder(data.length);

                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                TextView tvValue = (TextView) v.findViewById(R.id.tvValue);
                tvValue.setText(new String(data) + "\n<" + stringBuilder.toString() + ">");
                Log.i("SERVICE", "gotData: " + tvValue.getText().toString());
            }

        }  else if (ChatFragment.class.isInstance(fragment)) {
            if (type == BLEQueue.ITEM_TYPE_NOTIFICATION && uuid.equals(GattAttributes.UUID_CHARACTERISTIC_FIFO)) {
                ChatFragment chatFragment = (ChatFragment) fragment;
                chatFragment.addMessage(data);
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_UPDATE);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDevices.get(currentDevice).getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            mConnected = false;
        } catch (Exception ignore) {}
        unregisterReceiver(mGattUpdateReceiver);
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("");
        getActionBar().setLogo(R.drawable.logo);
        getActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_main);

        // Get a ref to the actionbar and set the navigation mode
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Initiate the view pager that holds our views
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        mViewPager.setOffscreenPageLimit(3); // Set to num tabs to keep the fragments in memory

        // Add the tabs and give them titles
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                actionBar.newTab()
                    .setText(mSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }

        // get the information from the device scan
        final Intent intent = getIntent();
        mDevices = intent.getParcelableArrayListExtra(EXTRAS_DEVICES);


        //getActionBar().setTitle(mDeviceName);
        getActionBar().setTitle("");

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        Spinner actionBarSpinner = new Spinner(this);

        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : mDevices) {
            if (device.getName() != null) {
                deviceNames.add(device.getName());
            } else {
                deviceNames.add("Unknown device");
            }
        }

        String[] spinner = new String[deviceNames.size()];
        deviceNames.toArray(spinner);

        ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                spinner);

        // Specify a SpinnerAdapter to populate the dropdown list.

        actionBarSpinner.setAdapter(spinnerArrayAdapter);

        // Set up the dropdown list navigation in the action bar.
        actionBarSpinner.setOnItemSelectedListener(this);

        actionBar.setCustomView(actionBarSpinner);

        actionBar.setDisplayShowCustomEnabled(true);
    }

    public boolean isConnected() {
        return mConnected;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_connected, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDevices.get(currentDevice).getAddress());
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onGreenLight(boolean enabled) {
        byte[] valueOn = {1};
        byte[] valueOff = {0};
        try {
            if (enabled) {
                mBluetoothLeService.writeCharacteristic(characteristicGreenLED, valueOn);
            } else {
                mBluetoothLeService.writeCharacteristic(characteristicGreenLED, valueOff);
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onRedLight(boolean enabled) {
        byte[] valueOn = {1};
        byte[] valueOff = {0};
        try {
            if (enabled) {
                mBluetoothLeService.writeCharacteristic(characteristicRedLED, valueOn);
            } else {
                mBluetoothLeService.writeCharacteristic(characteristicRedLED, valueOff);
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onRead(BluetoothGattCharacteristic characteristic) {
        try {
            mBluetoothLeService.readCharacteristic(characteristic);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onWrite(BluetoothGattCharacteristic characteristic, byte[] value) {
        try {
            mBluetoothLeService.writeCharacteristic(characteristic, value);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onNotify(BluetoothGattCharacteristic characteristic) {
        try {
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onSendMessage(BluetoothGattCharacteristic characteristic, byte[] message) {
        try {
            //APET setCharNot borde kanske göras när Chat-vyn öppnas och sedan inte varje gång man skickar data
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
            mBluetoothLeService.writeCharacteristic(characteristic, message);
        } catch (Exception ignore) {}
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        currentDevice = position;
        try {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        } catch (Exception ignore) {}
        try {
            mBluetoothLeService.connect(mDevices.get(currentDevice).getAddress());
        } catch (Exception ignore) {}
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private OverviewFragment mOverviewFragment = null;
        private ServicesFragment mServicesFragment = null;
        private ChatFragment mChatFragment = null;

        long lastSendLed = 0;

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                if (mOverviewFragment == null)
                    mOverviewFragment = OverviewFragment.newInstance();

                // Need to have a delay so that this don't get called to often.
                // Else the Bluetooth manager will crash..
                long timeout = (System.currentTimeMillis() - lastSendLed);
                if (timeout > 1000 || timeout < 0) {
                    lastSendLed = System.currentTimeMillis();
                    if (characteristicGreenLED != null)
                        mBluetoothLeService.readCharacteristic(characteristicGreenLED);
                    if (characteristicRedLED != null)
                        mBluetoothLeService.readCharacteristic(characteristicRedLED);
                }
                return mOverviewFragment;
            } else if (position == 1) {
                if (mServicesFragment == null) {
                    mServicesFragment = ServicesFragment.newInstance();
                }
                if (!mServicesFragment.hasGottenServices()) {
                    if (mServices != null)
                        mServicesFragment.displayGattServices(mServices);
                }
                return mServicesFragment;
            } else if (position == 2) {
                if (mChatFragment == null) {
                    mChatFragment = ChatFragment.newInstance();
                }
                if (characteristicFifo != null) {
                    mChatFragment.setCharacteristicFifo(characteristicFifo);
                }
                return mChatFragment;
            } else {
                return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }
}
