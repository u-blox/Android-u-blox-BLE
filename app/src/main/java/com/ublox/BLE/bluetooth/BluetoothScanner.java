package com.ublox.BLE.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothScanner implements BluetoothCentral, BluetoothAdapter.LeScanCallback {
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private ScanCallback callback;
    private State state;
    private Delegate delegate;
    private List<BluetoothDevice> foundPeripherals;

    public BluetoothScanner(BluetoothAdapter adapter) {
        this.adapter = adapter;
        if (!usingDeprecated()) {
            scanner = adapter.getBluetoothLeScanner();
            callback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    onLeScan(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                }

                @Override
                public void onScanFailed(int errorCode) {
                    setState(State.ON);
                }
            };
        }
        state = State.ON;
        foundPeripherals = new ArrayList<>();
    }

    @Override
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public List<BluetoothPeripheral> getFoundPeripherals() {
        return new ArrayList<>(foundPeripherals);
    }

    @Override
    public void scan(List<UUID> withServices) {
        if (state == State.SCANNING) return;
        foundPeripherals.clear();
        if (usingDeprecated()) {
            boolean started = adapter.startLeScan(withServices.toArray(new UUID[0]), this);
            if (started) {
                setState(State.SCANNING);
            }
        } else {
            ScanSettings.Builder settings = new ScanSettings.Builder();
            if (notLegacy()) settings.setLegacy(false);
            scanner.startScan(filtersFrom(withServices), settings.build(), callback);
            setState(State.SCANNING);
        }
    }

    @TargetApi(21)
    private List<ScanFilter> filtersFrom(List<UUID> withServices) {
        ArrayList<ScanFilter> filters = new ArrayList<>();
        for (UUID uuid : withServices) {
            ScanFilter.Builder filter = new ScanFilter.Builder();
            filter.setServiceUuid(new ParcelUuid(uuid));
            filters.add(filter.build());
        }
        return filters;
    }

    @Override
    public void stop() {
        if (state != State.SCANNING) return;
        if (usingDeprecated()) {
            adapter.stopLeScan(this);
        } else {
            scanner.stopScan(callback);
        }
        setState(State.ON);
    }

    @Override
    public void onLeScan(android.bluetooth.BluetoothDevice device, int rssi, byte[] scanRecord) {
        BluetoothDevice found = deviceWith(device.getAddress());
        if (found == null) {
            found = new BluetoothDevice(device, rssi);
            foundPeripherals.add(found);
        } else {
            found.updateAdvertisement(rssi);
        }
        BluetoothPeripheral peripheral = found;
        delegateOnMain(d-> d.centralFoundPeripheral(this, peripheral));
    }

    private boolean usingDeprecated() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }

    private boolean notLegacy() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    private BluetoothDevice deviceWith(String identifier) {
        for (BluetoothDevice peripheral : foundPeripherals) {
            if (peripheral.identifier().equals(identifier)) return peripheral;
        }
        return null;
    }

    private void setState(State newState) {
        if (newState == state) return;
        state = newState;
        delegateOnMain(d -> d.centralChangedState(this));
    }

    private void delegateOnMain(DelegateConsumer consumer) {
        if (delegate == null) return;
        Handler main = new Handler(Looper.getMainLooper());
        main.post(() -> consumer.consume(delegate));
    }

    private interface DelegateConsumer {
        void consume(Delegate delegate);
    }
}
