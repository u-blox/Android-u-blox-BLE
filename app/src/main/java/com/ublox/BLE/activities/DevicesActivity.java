package com.ublox.BLE.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ublox.BLE.R;
import com.ublox.BLE.bluetooth.BluetoothCentral;
import com.ublox.BLE.bluetooth.BluetoothDevice;
import com.ublox.BLE.bluetooth.BluetoothPeripheral;
import com.ublox.BLE.bluetooth.BluetoothScanner;
import com.ublox.BLE.fragments.KeyEntryFragment;
import com.ublox.BLE.utils.GattAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static no.nordicsemi.android.meshprovisioner.utils.SecureUtils.calculateK3;

public class DevicesActivity extends Activity implements AdapterView.OnItemClickListener, BluetoothCentral.Delegate {
    /*
     * Experimental mesh features have been disabled.
     * You can re-enable them by setting this flag to true and re-compile.
     * Warning: Mesh features are very unstable and may provide a bad user experience.
     */
    private static boolean DEFINE_MESH_ACTIVE = false;

    private static final UUID SPS_SERVICE = UUID.fromString(GattAttributes.UUID_SERVICE_SERIAL_PORT);
    private static final UUID MESH_SERVICE = UUID.fromString(GattAttributes.UUID_SERVICE_MESH_PROXY);
    private static final int LOCATION_REQUEST = 255;
    private static final int BLUETOOTH_CONNECT_REQUEST = 103;
    private static final int BLUETOOTH_SCAN_REQUEST = 102;
    private static final int BLUETOOTH_ENABLE_REQUEST = 101;
    private static final byte[] DEFAULT_NET_KEY = {0x5F, 0x5F, 0x6E, 0x6F, 0x72, 0x64, 0x69, 0x63, 0x5F, 0x5F, 0x73, 0x65, 0x6D, 0x69, 0x5F, 0x50};
    private static final byte[] DEFAULT_APP_KEY = {0x5F, 0x11, 0x6E, 0x6F, 0x72, 0x64, 0x69, 0x63, 0x5F, 0x5F, 0x73, 0x65, 0x6D, 0x69, 0x5F, 0x5F};

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothCentral scanner;
    private byte[] currentNetworkId = calculateK3(DEFAULT_NET_KEY);
    private byte[] currentNetKey = DEFAULT_NET_KEY;
    private byte[] currentAppKey = DEFAULT_APP_KEY;

    private static final int REQUEST_ENABLE_BT = 1;

    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_NET_KEY = "netKey";
    public static final String EXTRA_APP_KEY = "appKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("");
        getActionBar().setLogo(R.drawable.logo);
        getActionBar().setDisplayUseLogoEnabled(true);

        setContentView(R.layout.activity_devices);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        ((TextView) findViewById(R.id.filterText)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mLeDeviceListAdapter.setFreeTextFilter(s.toString());
            }
        });

        ((CheckBox) findViewById(R.id.checkBoxSps)).setOnCheckedChangeListener((checkBox, isChecked) -> {
            mLeDeviceListAdapter.setSpsFilter(isChecked);
        });

        if (DEFINE_MESH_ACTIVE) {
            CheckBox meshCheckBox = findViewById(R.id.checkBoxMesh);
            meshCheckBox.setVisibility(View.VISIBLE);
            meshCheckBox.setOnCheckedChangeListener((checkBox, isChecked) -> {
                mLeDeviceListAdapter.setMeshFilter(isChecked);
            });
        }

        scanner = new BluetoothScanner(mBluetoothAdapter);
        scanner.setDelegate(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_devices, menu);
        if (DEFINE_MESH_ACTIVE) {
            menu.findItem(R.id.menu_mesh_keys).setVisible(true);
        }
        if (scanner.getState() != BluetoothScanner.State.SCANNING) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_remote_control:
                Intent remoteIntent = new Intent(this, RemoteActivity.class);
                startActivity(remoteIntent);
                break;
            case R.id.menu_about:
                scanLeDevice(false);
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                break;
            case R.id.menu_mesh_keys:
                showMeshDialog(null);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) == PERMISSION_GRANTED) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                requestPermissions(new String[] { BLUETOOTH_CONNECT }, BLUETOOTH_ENABLE_REQUEST);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mLeDeviceListAdapter.setFreeTextFilter(((EditText) findViewById(R.id.filterText)).getText().toString());
        mLeDeviceListAdapter.setSpsFilter(((CheckBox) findViewById(R.id.checkBoxSps)).isChecked());
        mLeDeviceListAdapter.setMeshFilter(((CheckBox) findViewById(R.id.checkBoxMesh)).isChecked());

        setListAdapter(mLeDeviceListAdapter);
    }

    private void setListAdapter(BaseAdapter baseAdapter) {
        ListView lvDevices = findViewById(R.id.lvDevices);
        lvDevices.setAdapter(baseAdapter);
        lvDevices.setOnItemClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            verifyPermissionAndScan();
        } else {
            scanner.stop();
        }
        invalidateOptionsMenu();
    }

    private void showMeshDialog(BluetoothPeripheral andJoin) {
        if (!DEFINE_MESH_ACTIVE) return;

        boolean doJoin = andJoin != null;
        boolean doSkipDialog = doJoin && hasMatchingKey(andJoin);

        if (doSkipDialog) {
            joinMesh(andJoin);
            return;
        }

        String acceptText = doJoin ? "Join" : "Set";
        KeyEntryFragment dialog = new KeyEntryFragment();
        dialog.setDefaultKeys(DEFAULT_NET_KEY, DEFAULT_APP_KEY);
        dialog.setAcceptKeys(acceptText, (netKey, appKey) -> {
            currentNetKey = netKey;
            currentAppKey = appKey;
            currentNetworkId = calculateK3(netKey);

            if (!doJoin) return;
            joinMesh(andJoin);
        });
        dialog.show(getFragmentManager(), "KeyEntry");
    }

    private boolean hasMatchingKey(BluetoothPeripheral peripheral) {
        byte[] serviceData = peripheral.serviceDataFor(MESH_SERVICE);
        return serviceData.length > 8 && serviceData[0] == 0 && Arrays.equals(currentNetworkId, Arrays.copyOfRange(serviceData, 1, serviceData.length));
    }

    private void joinMesh(BluetoothPeripheral peripheral) {
        Intent intent = new Intent(this, MeshActivity.class);
        intent.putExtra(EXTRA_DEVICE, peripheral);
        intent.putExtra(EXTRA_NET_KEY, currentNetKey);
        intent.putExtra(EXTRA_APP_KEY, currentAppKey);
        startActivity(intent);
    }

    @TargetApi(23)
    private void verifyPermissionAndScan() {
        String[] request = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? new String[] {BLUETOOTH_SCAN, BLUETOOTH_CONNECT}
                : new String[] {ACCESS_FINE_LOCATION};
        int requestCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? BLUETOOTH_SCAN_REQUEST
                : LOCATION_REQUEST;

        if (ContextCompat.checkSelfPermission(this, request[0]) == PERMISSION_GRANTED) {
            scanner.scan(new ArrayList<>());
        } else {
            requestPermissions(request, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_REQUEST || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && requestCode == BLUETOOTH_SCAN_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                scanner.scan(new ArrayList<>());
            } else {
                int messageID = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? R.string.scan_permission_toast
                    : R.string.location_permission_toast;
                Toast.makeText(this, messageID, Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == LOCATION_REQUEST || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && requestCode == BLUETOOTH_SCAN_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                scanner.scan(new ArrayList<>());
            } else {
                int messageID = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? R.string.scan_permission_toast
                        : R.string.location_permission_toast;
                Toast.makeText(this, messageID, Toast.LENGTH_LONG).show();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && requestCode == BLUETOOTH_ENABLE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Toast.makeText(this, R.string.enable_permission_toast, Toast.LENGTH_LONG).show();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && requestCode == BLUETOOTH_CONNECT_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                //TODO automatically connect
                Toast.makeText(this, R.string.connect_retry_toast, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.connect_permission_toast, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mLeDeviceListAdapter.getCount() > position) {
            scanner.stop();
            BluetoothPeripheral device = mLeDeviceListAdapter.getDevice(position);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) == PERMISSION_GRANTED) {
                if (DEFINE_MESH_ACTIVE && device.advertisedService(MESH_SERVICE)) {
                    showMeshDialog(device);
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra(EXTRA_DEVICE, ((BluetoothDevice) device).toUbloxDevice());
                    startActivity(intent);
                }
            } else {
                requestPermissions(new String[] { BLUETOOTH_CONNECT }, BLUETOOTH_CONNECT_REQUEST);
            }
        }
    }

    @Override
    public void centralChangedState(BluetoothCentral central) {
        invalidateOptionsMenu();
    }

    @Override
    public void centralFoundPeripheral(BluetoothCentral central, BluetoothPeripheral peripheral) {
        runOnUiThread(() -> mLeDeviceListAdapter.addDevice(peripheral));
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothPeripheral> mFilteredSortedPeripherals;
        private HashMap<BluetoothPeripheral, Integer> mPeripheralsStaticRssi;
        private LayoutInflater mInflator;
        private String mFreeTextFilter;
        private boolean mSpsFilter;
        private boolean mMeshFilter;

        public LeDeviceListAdapter() {
            super();
            mFilteredSortedPeripherals = new ArrayList<>();
            mPeripheralsStaticRssi = new HashMap<>();
            mInflator = DevicesActivity.this.getLayoutInflater();
            mFreeTextFilter = "";
        }

        public void setFreeTextFilter(String filter) {
            if (mFreeTextFilter.equalsIgnoreCase(filter)) return;
            mFreeTextFilter = filter != null ? filter : "";
            reFilter();
        }

        public void setSpsFilter(boolean spsFilter) {
            if (mSpsFilter == spsFilter) return;
            mSpsFilter = spsFilter;
            reFilter();
        }

        public void setMeshFilter(boolean meshFilter) {
            if (mMeshFilter == meshFilter) return;
            mMeshFilter = meshFilter;
            reFilter();
        }

        public void addDevice(BluetoothPeripheral device) {
            int rssi = device.rssi();
            if (mPeripheralsStaticRssi.containsKey(device) && Math.abs(mPeripheralsStaticRssi.get(device) - rssi) <= 10) {
                return;
            }

            mPeripheralsStaticRssi.put(device, rssi);

            reFilter();
        }

        public BluetoothPeripheral getDevice(int position) {
            return mFilteredSortedPeripherals.get(position);
        }

        public void clear() {
            mFilteredSortedPeripherals.clear();
            mPeripheralsStaticRssi.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mFilteredSortedPeripherals.size();
        }

        @Override
        public Object getItem(int i) {
            return mFilteredSortedPeripherals.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            BluetoothPeripheral device = mFilteredSortedPeripherals.get(i);
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceRssi = view.findViewById(R.id.device_rssi);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceBonded = view.findViewById(R.id.device_bonded);
                viewHolder.imgRssi = view.findViewById(R.id.img_rssi);
                viewHolder.deviceMeshProxy = view.findViewById(R.id.tvMesh);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final View finalView = view;
            view.setOnClickListener(v -> onItemClick(null, finalView, i, i));

            final String deviceName = device.name();
            final String deviceAddress = device.identifier();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
                viewHolder.deviceName.setVisibility(View.VISIBLE);
                viewHolder.deviceAddress.setTypeface(null, Typeface.NORMAL);
            }
            else {
                viewHolder.deviceName.setVisibility(View.INVISIBLE);
                //viewHolder.deviceAddress.setTypeface(null, Typeface.BOLD);
            }
            if (DEFINE_MESH_ACTIVE) {
                viewHolder.deviceMeshProxy.setVisibility(device.advertisedService(MESH_SERVICE) ? View.VISIBLE : View.INVISIBLE);
            }
            viewHolder.deviceAddress.setText(deviceAddress);
            updateBondedStateComponents(device, viewHolder);
            updateRssiComponents(device, viewHolder);

            return view;
        }

        private boolean peripheralMatchesFilter(BluetoothPeripheral peripheral) {
            if (!mMeshFilter || peripheral.advertisedService(MESH_SERVICE))
                if (!mSpsFilter || peripheral.advertisedService(SPS_SERVICE))
                    return containsFilterIgnoreCase(peripheral.name()) ||
                        containsFilterIgnoreCase(peripheral.identifier());

            return false;
        }

        private boolean containsFilterIgnoreCase(String search) {
            if (search == null) return false;
            search = search.toLowerCase();
            String pattern = mFreeTextFilter.toLowerCase();
            return search.contains(pattern);
        }

        private void reFilter() {
            ArrayList<BluetoothPeripheral> filtered = new ArrayList<>();
            for (BluetoothPeripheral peripheral: mPeripheralsStaticRssi.keySet()) {
                if (peripheralMatchesFilter(peripheral)) {
                    filtered.add(peripheral);
                }
            }

            Collections.sort(filtered, (a, b) -> mPeripheralsStaticRssi.get(b) - mPeripheralsStaticRssi.get(a));
            if (filtered.equals(mFilteredSortedPeripherals)) return;
            mFilteredSortedPeripherals = filtered;
            notifyDataSetChanged();
        }

        private void updateBondedStateComponents(BluetoothPeripheral device, ViewHolder viewHolder) {
            switch(device.bondState()) {
                case BOND_NONE:
                    viewHolder.deviceBonded.setVisibility(View.INVISIBLE);
                    break;
                case BOND_BONDING:
                    viewHolder.deviceBonded.setText(R.string.bonding_state);
                    viewHolder.deviceBonded.setVisibility(View.VISIBLE);
                    break;
                case BOND_BONDED:
                    viewHolder.deviceBonded.setText(R.string.bonded_state);
                    viewHolder.deviceBonded.setVisibility(View.VISIBLE);
                    break;
            }
        }

        private void updateRssiComponents(BluetoothPeripheral device, ViewHolder viewHolder) {
            final int rssi = mPeripheralsStaticRssi.get(device);
            viewHolder.deviceRssi.setText(String.format("%s dBm", String.valueOf(rssi)));
            if(rssi <= -100) {
                viewHolder.imgRssi.setImageResource(R.drawable.signal_indicator_0);
            } else if (rssi < -85) {
                viewHolder.imgRssi.setImageResource(R.drawable.signal_indicator_1);
            } else if (rssi < -70) {
                viewHolder.imgRssi.setImageResource(R.drawable.signal_indicator_2);
            } else if (rssi < -55) {
                viewHolder.imgRssi.setImageResource(R.drawable.signal_indicator_3);
            } else {
                viewHolder.imgRssi.setImageResource(R.drawable.signal_indicator_4);
            }
        }

    }

    static class ViewHolder {
        ImageView imgRssi;
        TextView deviceRssi;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceBonded;
        TextView deviceMeshProxy;
    }
}
