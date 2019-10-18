package com.ublox.BLE.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ublox.BLE.R;
import com.ublox.BLE.datapump.DataPump;
import com.ublox.BLE.datapump.SpsStream;
import com.ublox.BLE.fragments.ChatFragment;
import com.ublox.BLE.fragments.OverviewFragment;
import com.ublox.BLE.fragments.ServicesFragment;
import com.ublox.BLE.fragments.TestFragment;
import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.interfaces.ITestInteractionListener;
import com.ublox.BLE.services.BluetoothLeService;
import com.ublox.BLE.utils.ConnectionState;
import com.ublox.BLE.utils.GattAttributes;
import com.ublox.BLE.utils.PhyMode;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;


public class MainActivity extends Activity implements ActionBar.TabListener,
        OverviewFragment.IOverviewFragmentInteraction, ServicesFragment.IServiceFragmentInteraction,
        ChatFragment.IChatInteractionListener, AdapterView.OnItemSelectedListener,
        ITestInteractionListener, DataPump.Delegate {

    public static final double RATE_CONVERSION = 8000000.0;
    public static final double KILO_BYTE = 1024.0;
    public static final double MEGA_BYTE = 1048576.0;
    public static final double GIGA_BYTE = 1073741824.0;
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRA_DEVICE = "device";

    private boolean needToWaitForMtuUpdate = false;
    private boolean isFirstTimeToSetFifo = true;

    private TextView tvStatus;
    private RelativeLayout rlProgress;

    private BluetoothDeviceRepresentation mDevice;

    private BluetoothLeService mBluetoothLeService;
    private static ConnectionState mConnectionState = ConnectionState.DISCONNECTED;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private BluetoothGattCharacteristic characteristicRedLED;
    private BluetoothGattCharacteristic characteristicGreenLED;

    private BluetoothGattCharacteristic characteristicFifo;
    private BluetoothGattCharacteristic characteristicCredits;

    private DataPump mDataPump;

    public void onServiceConnected() {
        if (!mBluetoothLeService.initialize(this)) {
            finish();
        }
        mSpsStream.setBluetoothService(mBluetoothLeService);
    }


    public final MyBroadcastReceiver mGattUpdateReceiver = new MyBroadcastReceiver();

    private List<BluetoothGattService> mServices;
    private SpsStream mSpsStream;
    private long timeOfLastUiUpdate;

    /**
     * Converts the total number of bytes to a human-readable string.
     * Prints as integer B or decimal KB, MB, or GB as needed.
     */
    public static String transferAmount(long bytes) {
        if (bytes < KILO_BYTE) return String.format(Locale.US, "%d B", bytes);
        if (bytes < MEGA_BYTE) return String.format(Locale.US, "%.2f KB", bytes / KILO_BYTE);
        if (bytes < GIGA_BYTE) return String.format(Locale.US, "%.2f MB", bytes / MEGA_BYTE);
        return String.format(Locale.US, "%.2f GB", bytes / GIGA_BYTE);
    }

    /**
     * Returns a String representing the transfer rate in kbps.
     * Note: duration is given in nanoseconds.
     */
    public static String transferRate(long bytes, long duration) {
        double kbps = RATE_CONVERSION * bytes / duration;
        return String.format(Locale.US, "%.2f kbps", kbps);
    }

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

    private void sendToActiveFragment(boolean connected) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (TestFragment.class.isInstance(fragment)) {
            if (!connected) {
                View v = fragment.getView();
                TestFragment testFragment = (TestFragment) fragment;
                testFragment.setTxTestToOff(v);
                testFragment.setSwCreditsToOff(v);
                testFragment.updateMTUSize(23);
                mDataPump.reset();
                timeOfLastUiUpdate = 0;
            }
        }
    }

    @RequiresApi(26)
    private void sendToTestFragment(PhyMode txPhyMode) {
        final int testPosition = 3;
        Fragment fragment = mSectionsPagerAdapter.getItem(testPosition);
        if (TestFragment.class.isInstance(fragment)) {
            TestFragment testFragment = (TestFragment) fragment;
            testFragment.setIsLE2MPhySupported(txPhyMode);
        }
    }

    private String phyStateToString() {
        PhyMode txPhyMode = mBluetoothLeService.getTxPhyMode();
        PhyMode rxPhyMode = mBluetoothLeService.getRxPhyMode();

        if (txPhyMode == rxPhyMode && txPhyMode != PhyMode.PHY_UNDEFINED) {
            return "central and peripheral have " + txPhyMode.toString();
        } else if (txPhyMode != PhyMode.PHY_UNDEFINED && rxPhyMode != PhyMode.PHY_UNDEFINED){
            return "central has " + txPhyMode.toString() + ", peripheral has " + rxPhyMode;
        } else {
            return "gatt connected";
        }

    }


    private void sendToActiveFragment(final BluetoothGattCharacteristic characteristic, boolean isFifoCharacteristic) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (ChatFragment.class.isInstance(fragment) && isFifoCharacteristic) {
            ChatFragment chatFragment = (ChatFragment) fragment;
            chatFragment.setCharacteristicFifo(characteristic);
        }

        if (TestFragment.class.isInstance(fragment)) {
                if(isFifoCharacteristic) {
                    mSpsStream.setFifo(characteristic);
                } else {
                    mSpsStream.setCredits(characteristic);
                }
        }
    }

    private void sendToActiveFragment(int mtu, int status) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (TestFragment.class.isInstance(fragment)) {
            if (status == GATT_SUCCESS) {
                mDataPump.updateMtuSizeChanged(mtu);
            } else {
                Toast.makeText(fragment.getActivity(), ("Request MTU - error: " + status), Toast.LENGTH_LONG).show();
            }
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

    private void sendToActiveFragment(UUID uuid, int type, byte[] data) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (OverviewFragment.class.isInstance(fragment)) {
            runOnUiThread(() -> {
                View v = fragment.getView();

                if (uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
                    ((TextView) v.findViewById(R.id.tvBatteryLevel)).setText(String.format("%d", data[0]));
                }

                if (uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_TEMP_VALUE)) {
                    ((TextView) v.findViewById(R.id.tvTemperature)).setText(String.format("%d", data[0]));
                }

                if (uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_ACC_RANGE)) {
                    ((TextView) v.findViewById(R.id.tvAccelerometerRange)).setText(String.format("+-%d", data[0]));
                }

                if (type == BluetoothLeService.ITEM_TYPE_READ) {

                    StringBuilder stringBuilder = new StringBuilder(data.length);

                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                    if (uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_GREEN_LED)) {
                        Switch sGreen = v.findViewById(R.id.sGreenLight);
                        if (data[0] == 0) {
                            sGreen.setChecked(false);
                        } else {
                            sGreen.setChecked(true);
                        }
                    } else if (uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_RED_LED)) {
                        Switch sRed = v.findViewById(R.id.sRedLight);
                        if (data[0] == 0) {
                            sRed.setChecked(false);
                        } else {
                            sRed.setChecked(true);
                        }
                    }

                } else if (type == BluetoothLeService.ITEM_TYPE_NOTIFICATION) {
                    if (uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_ACC_X)) {
                        ((ProgressBar) v.findViewById(R.id.pbX)).setProgress(data[0] + 128);
                    } else if (uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Y)) {
                        ((ProgressBar) v.findViewById(R.id.pbY)).setProgress(data[0] + 128);
                    } else if (uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Z)) {
                        ((ProgressBar) v.findViewById(R.id.pbZ)).setProgress(data[0] + 128);
                    }
                }
            });
        } else if (ServicesFragment.class.isInstance(fragment)) {
            runOnUiThread(() -> {
                View v = fragment.getView();
                Log.i("SERVICE", "gotData, uuid: " + uuid);
                Log.i("SERVICE", "wanted __UUID: " + ((ServicesFragment) fragment).currentUuid);


                if (((ServicesFragment) fragment).currentUuid.equals(uuid.toString())) {
                    StringBuilder stringBuilder = new StringBuilder(data.length);

                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                    TextView tvValue = v.findViewById(R.id.tvValue);
                    tvValue.setText(new String(data) + "\n<" + stringBuilder.toString() + ">");
                    Log.i("SERVICE", "gotData: " + tvValue.getText().toString());
                }
            });
        }  else if (ChatFragment.class.isInstance(fragment)) {
            runOnUiThread(() -> {
                if (type == BluetoothLeService.ITEM_TYPE_NOTIFICATION && uuid.toString().equals(GattAttributes.UUID_CHARACTERISTIC_FIFO)) {
                    ChatFragment chatFragment = (ChatFragment) fragment;
                    chatFragment.addMessage(data);
                }
            });
        } else if (TestFragment.class.isInstance(fragment)) {
            mSpsStream.notifyGattUpdate(uuid, type, data);
        }
    }

    private void updateStatus() {
        switch (mConnectionState) {
            case DISCONNECTED:
                tvStatus.setText(R.string.status_disconnected);
                break;
            case CONNECTING:
                tvStatus.setText(R.string.status_connecting);
                break;
            case CONNECTED:
                StringBuilder status = new StringBuilder(phyStateToString());
                status.append(", ");
                switch (mViewPager.getCurrentItem()){
                    case 2:
                        if (characteristicFifo != null) {
                            status.append(getString(R.string.status_chat_available));
                        } else {
                            status.append(getString(R.string.status_fifo_unavailable));
                        }
                        break;
                    case 3:
                        if (characteristicFifo != null && characteristicCredits != null) {
                            status.append(getString(R.string.status_sps_available));
                        } else {
                            status.append(getString(R.string.status_fifo_credits_unavailable));
                        }
                        break;
                    default:
                        if (mServices != null && !mServices.isEmpty()) {
                            status.append(getString(R.string.status_discovered));
                        }
                        break;
                }
                tvStatus.setText(status);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.register(mGattUpdateReceiver);
            final boolean result = mBluetoothLeService.connect(mDevice);
            Log.d(TAG, "Connect request result=" + result);
            mConnectionState = ConnectionState.CONNECTING;
            invalidateOptionsMenu();
            updateStatus();
            rlProgress.setVisibility(View.VISIBLE);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            mConnectionState = ConnectionState.DISCONNECTED;
            mBluetoothLeService.unregister();
        } catch (Exception ignore) {}
        invalidateOptionsMenu();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("");
        getActionBar().setLogo(R.drawable.logo);
        getActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_main);

        mViewPager = findViewById(R.id.pager);

        tvStatus = findViewById(R.id.tvStatus);
        rlProgress = findViewById(R.id.rlProgress);

        final Intent intent = getIntent();
        updateStatus();

        mSpsStream = new SpsStream();
        mDataPump = new DataPump(mSpsStream, this);
        connectToDevice(intent.getParcelableExtra(EXTRA_DEVICE));

        // Get a ref to the actionbar and set the navigation mode
        final ActionBar actionBar = getActionBar();

        setTabsWithPagerAdapter(actionBar);

        final String name = mDevice.getName();
        if (!TextUtils.isEmpty(name)) {
            getActionBar().setTitle(name);
        } else {
            getActionBar().setTitle(mDevice.getAddress());
        }

        actionBar.setDisplayShowCustomEnabled(true);
    }

    private void connectToDevice(BluetoothDeviceRepresentation bluetoothDevice) {
        // get the information from the device scan
        mDevice = bluetoothDevice;

        mBluetoothLeService = new BluetoothLeService();
        onServiceConnected();
    }

    private void setTabsWithPagerAdapter(final ActionBar actionBar) {
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Initiate the view pager that holds our views
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
                updateStatus();
                switch (position) {
                    case 1:
                        ServicesFragment mServicesFragment = (ServicesFragment) mSectionsPagerAdapter.getItem(position);

                        if (!mServicesFragment.hasGottenServices()) {
                            if (mServices != null)
                                mServicesFragment.displayGattServices(mServices);
                        }
                        break;
                    case 2:
                        ChatFragment mChatFragment = (ChatFragment) mSectionsPagerAdapter.getItem(position);

                        if (characteristicFifo != null) {
                            mChatFragment.setCharacteristicFifo(characteristicFifo);
                        }
                        break;
                    case 3:
                        if (characteristicCredits != null) {
                            mSpsStream.setCredits(characteristicCredits);
                        }
                        if (characteristicFifo != null) {
                            mSpsStream.setFifo(characteristicFifo);
                        }
                        break;
                    default:
                        break;
                }
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
    }

    public boolean isConnected() {
        return mConnectionState == ConnectionState.CONNECTED;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_connected, menu);
        switch (mConnectionState) {
            case CONNECTED:
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(true);
                break;
            case CONNECTING:
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                break;
            case DISCONNECTED:
                menu.findItem(R.id.menu_connect).setVisible(true);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDevice);
                mConnectionState = ConnectionState.CONNECTING;
                invalidateOptionsMenu();
                updateStatus();
                rlProgress.setVisibility(View.VISIBLE);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                updateStatus();
                rlProgress.setVisibility(View.VISIBLE);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
        if(fragment instanceof ServicesFragment &&
            ((ServicesFragment) fragment).isCharacteristicViewVisible()) {
            ((ServicesFragment) fragment).backButtonPressed();
        } else {
            super.onBackPressed();
        }
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
    public void onNotify(BluetoothGattCharacteristic characteristic, boolean enabled) {
        try {
            mBluetoothLeService.setCharacteristicNotification(characteristic, enabled);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onSendMessage(BluetoothGattCharacteristic characteristic, byte[] message) {
        try {
            mBluetoothLeService.writeCharacteristic(characteristic, message);
        } catch (Exception ignore) {}
    }

    @Override
    public void onPhyModeChange(PhyMode mode) {
        mBluetoothLeService.setPreferredPhy(mode);
    }

    @Override
    public void onSwitchCredits(boolean enabled) {
        mSpsStream.toggleCreditsConnection(enabled);
    }

    @Override
    public void onSwitchTest(boolean enabled) {
        if (enabled) {
            mDataPump.startDataPump();
            timeOfLastUiUpdate = 0;
        } else {
            mDataPump.stopDataPump();
        }
    }

    @Override
    public void onModeSet(boolean continuousEnabled) {
        mDataPump.setContinuousMode(continuousEnabled);
    }

    @Override
    public void onMtuSizeChanged(int size) {
        mSpsStream.setMtuSize(size);
    }

    @Override
    public void onPacketSizeChanged(int size) {
        mDataPump.setPacketSize(size);
    }

    @Override
    public void onBitErrorChanged(boolean enabled) {
        mDataPump.setBitErrorActive(enabled);
    }

    @Override
    public void onReset() {
        mDataPump.resetDataPump();
        timeOfLastUiUpdate = 0;
    }

    @Override
    public void updateConnectionPrio(int connectionParameter) {
        mSpsStream.connectionPrioRequest(connectionParameter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        try {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        } catch (Exception ignore) {}
        try {
            mBluetoothLeService.connect(mDevice);
            mConnectionState = ConnectionState.CONNECTING;
            invalidateOptionsMenu();
            updateStatus();
            rlProgress.setVisibility(View.VISIBLE);
        } catch (Exception ignore) {}
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void updateMTUSize(int size) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (TestFragment.class.isInstance(fragment)) {
            ((TestFragment)fragment).updateMTUSize(size);
        }
    }

    @Override
    public void onTx(long bytes, long duration) {
        if (mDataPump.isTestRunning() && duration - timeOfLastUiUpdate < 100000000) return;
        timeOfLastUiUpdate = duration;

        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        runOnUiThread(() -> {
            String value = MainActivity.transferAmount(bytes);
            String average = MainActivity.transferRate(bytes, duration);

            if (TestFragment.class.isInstance(fragment)) {
                ((TestFragment) fragment).updateTxCounter(value, average);
            }
        });
    }

    @Override
    public void onRx(long bytes, long duration) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        runOnUiThread(() -> {
            String rxValue = MainActivity.transferAmount(bytes);
            String average = MainActivity.transferRate(bytes, duration);

            if (TestFragment.class.isInstance(fragment)) {
                ((TestFragment) fragment).updateRxCounter(rxValue, average);
            }
        });
    }

    public BluetoothLeService getLeService() {
        return mBluetoothLeService;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private OverviewFragment mOverviewFragment = null;
        private ServicesFragment mServicesFragment = null;
        private ChatFragment mChatFragment = null;
        private TestFragment mTestFragment = null;

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

                return mServicesFragment;
            } else if (position == 2) {
                if (mChatFragment == null) {
                    mChatFragment = ChatFragment.newInstance();
                }
                return mChatFragment;
            } else if (position == 3) {
                if (mTestFragment == null) {
                    mTestFragment = TestFragment.newInstance();
                }

                return mTestFragment;
            } else {
                return null;
            }
        }

        @Override
        public int getCount() {
            return 4;
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
                case 3:
                    return getString(R.string.title_section4).toUpperCase(l);
            }
            return null;
        }
    }

    private class MyBroadcastReceiver implements BluetoothLeService.Receiver {
        @Override
        public void onDescriptorWrite() {
            // TODO check other solution
            runOnUiThread(() -> {
                if (false && isFirstTimeToSetFifo) {
                    isFirstTimeToSetFifo = false;
                    mSpsStream.setCredits(characteristicCredits);
                    if (true) {
                        mSpsStream.toggleCreditsConnection(true);
                    }
                    if (!needToWaitForMtuUpdate && !mDataPump.isTestRunning()) {
                        mDataPump.startDataPump();
                    }
                }
            });
        }

        @Override
        public void onPhyAvailable(boolean isUpdate) {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    final PhyMode txPhyMode = mBluetoothLeService.getTxPhyMode();
                    if (!isUpdate) {
                        sendToTestFragment(txPhyMode);
                    }
                    updateStatus();
                }
            });
        }

        @Override
        public void onMtuUpdate(int mtu, int status) {
            runOnUiThread(() -> sendToActiveFragment(mtu, status));
        }

        @Override
        public void onRssiUpdate(int rssi) {
            runOnUiThread(() -> sendToActiveFragment(rssi));
        }

        @Override
        public void onDataAvailable(UUID uUid, int type, byte[] data) {
            sendToActiveFragment(uUid, type, data);
        }

        @Override
        public void onServicesDiscovered() {
            runOnUiThread(() -> {
                sendToActiveFragment(mBluetoothLeService.getSupportedGattServices());
                updateStatus();
                for (BluetoothGattService service : mBluetoothLeService.getSupportedGattServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String uuid = characteristic.getUuid().toString();
                        if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_RANGE)) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                            } catch (Exception ignore) {
                            }
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_GREEN_LED)) {
                            try {
                                characteristicGreenLED = characteristic;
                                mBluetoothLeService.readCharacteristic(characteristic);
                            } catch (Exception ignore) {
                            }
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RED_LED)) {
                            try {
                                characteristicRedLED = characteristic;
                                mBluetoothLeService.readCharacteristic(characteristic);
                            } catch (Exception ignore) {
                            }
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_X)
                            || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Y)
                            || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Z)
                            || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_BATTERY_LEVEL)
                            || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TEMP_VALUE)
                            ) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                            } catch (Exception ignore) {
                            }
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_FIFO)) {
                            characteristicFifo = characteristic;
                            sendToActiveFragment(characteristicFifo, true);
                            updateStatus();
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_CREDITS)) {
                            characteristicCredits = characteristic;
                            sendToActiveFragment(characteristicCredits, false);
                            updateStatus();
                        }
                    }
                }
            });
        }

        @Override
        public void onGattDisconnected() {
            runOnUiThread(() -> {
                mConnectionState = ConnectionState.DISCONNECTED;
                invalidateOptionsMenu();
                sendToActiveFragment(false);
                updateStatus();
                rlProgress.setVisibility(View.GONE);
            });
        }

        @Override
        public void onGattConnected() {
            runOnUiThread(() -> {
                mConnectionState = ConnectionState.CONNECTED;
                invalidateOptionsMenu();
                updateStatus();
                sendToActiveFragment(true);
                rlProgress.setVisibility(View.GONE);
            });
        }
    }
}
