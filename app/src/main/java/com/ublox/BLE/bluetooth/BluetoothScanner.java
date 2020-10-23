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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                    Map<UUID, byte[]> services = new HashMap<>();
                    List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();
                    if (serviceUuids != null) {
                        for (ParcelUuid puuid : serviceUuids) {
                            services.put(puuid.getUuid(), new byte[0]);
                        }
                    }
                    Map<ParcelUuid, byte[]> serviceData = result.getScanRecord().getServiceData();
                    for (ParcelUuid puuid : serviceData.keySet()) {
                        services.put(puuid.getUuid(),serviceData.get(puuid));
                    }
                    onPeripheralScan(result.getDevice(), result.getRssi(), services);
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
        onPeripheralScan(device, rssi, parseServices(scanRecord));
    }

    private void onPeripheralScan(android.bluetooth.BluetoothDevice device, int rssi, Map<UUID, byte[]> advertisedServices) {
        BluetoothDevice found = deviceWith(device.getAddress());
        if (found == null) {
            found = new BluetoothDevice(device);
            foundPeripherals.add(found);
        }
        found.updateAdvertisement(rssi, advertisedServices);
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

    /*No way around it, for supporting lower API devices (<21) we need to parse out services ourselves*/
    @Deprecated
    private Map<UUID, byte[]> parseServices(byte[] scanRecord) {
        int offset = 0;
        HashMap<UUID, byte[]> services = new HashMap<>();
        while (offset < scanRecord.length) {
            int length = offset + scanRecord[offset] + 1;
            offset++;
            byte type = scanRecord[offset++];
            switch (type) {
                case 2: case 3: parseServiceUuids(scanRecord, offset, length, 2, services); break;
                case 4: case 5: parseServiceUuids(scanRecord, offset, length, 4, services); break;
                case 6: case 7: parseServiceUuids(scanRecord, offset, length, 16, services); break;
                case 0x16: parseServiceData(scanRecord, offset, length, 2, services); break;
                case 0x20: parseServiceData(scanRecord, offset, length, 4, services); break;
                case 0x21: parseServiceData(scanRecord, offset, length, 16, services); break;
            }
            offset = length;
        }
        return services;
    }

    @Deprecated
    private void parseServiceUuids(byte[] record, int offset, int length, int uuidLength, Map<UUID, byte[]> services) {
        for (int i = offset; i < length; i += uuidLength) {
            UUID uuid = parseUUID(record, i, uuidLength);
            if (!services.containsKey(uuid)) {
                services.put(uuid, new byte[0]);
            }
        }
    }

    @Deprecated
    private void parseServiceData(byte[] record, int offset, int length, int uuidLength, HashMap<UUID, byte[]> services) {
        UUID uuid = parseUUID(record, offset, uuidLength);
        offset += uuidLength;
        byte[] data = Arrays.copyOfRange(record, offset, length);
        services.put(uuid, data);
    }

    @Deprecated
    private UUID parseUUID(byte[] record, int offset, int length) {
        long msb = length == 16
            ? fromBytesReverse(record, offset + 8, 8)
            : 0x0000000000001000L + (fromBytesReverse(record, offset, length) << 32);

        long lsb = length == 16
            ? fromBytesReverse(record, offset, 8)
            : 0x800000805F9B34FBL;

        return new UUID(msb, lsb);
    }

    @Deprecated
    private long fromBytesReverse(byte[] bytes, int offset, int length) {
        long value = 0L;
        for (int i = offset + length - 1; i >= offset; i--) {
            value <<= 8;
            value += (bytes[i] & 0xFF);
        }
        return value;
    }

    private interface DelegateConsumer {
        void consume(Delegate delegate);
    }
}
