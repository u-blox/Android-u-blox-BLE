package com.ublox.BLE.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ublox.BLE.R;
import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.utils.UBloxDevice;

import java.util.ArrayList;
import java.util.HashMap;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class DevicesActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final String TAG = DevicesActivity.class.getSimpleName();
    private static final int LOCATION_REQUEST = 255;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;

    private static final int REQUEST_ENABLE_BT = 1;

    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_REMOTE = "remote";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("");
        getActionBar().setLogo(R.drawable.logo);
        getActionBar().setDisplayUseLogoEnabled(true);

        setContentView(R.layout.activity_devices);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_devices, menu);
        if (!mScanning) {
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
                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.putExtra(EXTRA_REMOTE, true);
                startActivity(mainIntent);
                break;
            case R.id.menu_about:
                scanLeDevice(false);
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
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
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @TargetApi(23)
    private void verifyPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            startScan();
        } else {
            requestPermissions(new String[] {ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != LOCATION_REQUEST) return;

        if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
            startScan();
        } else {
            Toast.makeText(this, R.string.location_permission_toast, Toast.LENGTH_LONG).show();
        }
    }

    private void startScan() {
        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mLeDeviceListAdapter.getCount() > position) {
            Log.d(TAG, "onListItemClick");
            BluetoothDeviceRepresentation device = mLeDeviceListAdapter.getDevice(position);

            if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            }

            Intent intent = new Intent(this, MainActivity.class);
            Log.w(TAG, "Putting " + EXTRA_DEVICE + " " + String.valueOf(device == null));
            intent.putExtra(EXTRA_DEVICE, device);
            startActivity(intent);
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDeviceRepresentation> mLeDevices;
        private LayoutInflater mInflator;

        private HashMap<BluetoothDeviceRepresentation, Integer> mDevicesRssi = new HashMap<>();

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = DevicesActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDeviceRepresentation device, int rssi) {
            if (mDevicesRssi.containsKey(device)) {
                int oldRssi = mDevicesRssi.get(device);
                if (Math.abs(oldRssi - rssi) > 10) {
                    mDevicesRssi.put(device, rssi);
                    notifyDataSetChanged();
                }
            } else {
                mDevicesRssi.put(device, rssi);
                notifyDataSetChanged();
            }
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                notifyDataSetChanged();
            }
        }

        public BluetoothDeviceRepresentation getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            BluetoothDeviceRepresentation device = mLeDevices.get(i);
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
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final View finalView = view;
            view.setOnClickListener(v -> onItemClick(null, finalView, i, i));

            final String deviceName = device.getName();
            final String deviceAddress = device.getAddress();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
                viewHolder.deviceName.setVisibility(View.VISIBLE);
                viewHolder.deviceAddress.setTypeface(null, Typeface.NORMAL);
            }
            else {
                viewHolder.deviceName.setVisibility(View.INVISIBLE);
                //viewHolder.deviceAddress.setTypeface(null, Typeface.BOLD);
            }
            viewHolder.deviceAddress.setText(deviceAddress);
            updateBondedStateComponents(device, viewHolder);
            updateRssiComponents(device, viewHolder);

            return view;
        }

        private void updateBondedStateComponents(BluetoothDeviceRepresentation device, ViewHolder viewHolder) {
            switch(device.getBondState()) {
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

        private void updateRssiComponents(BluetoothDeviceRepresentation device, ViewHolder viewHolder) {
            final int rssi = mDevicesRssi.get(device);
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

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(() -> mLeDeviceListAdapter.addDevice(new UBloxDevice(device), rssi));
                }
            };

    static class ViewHolder {
        ImageView imgRssi;
        TextView deviceRssi;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceBonded;
    }
}
