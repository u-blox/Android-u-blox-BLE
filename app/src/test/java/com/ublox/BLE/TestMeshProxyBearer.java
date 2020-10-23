package com.ublox.BLE;

import com.ublox.BLE.bluetooth.BluetoothPeripheral;
import com.ublox.BLE.datapump.DataStream;
import com.ublox.BLE.mesh.MeshProxyBearer;
import com.ublox.BLE.utils.GattAttributes;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMeshProxyBearer {

    MeshProxyBearer bearer;
    BluetoothPeripheral mockPeripheral;
    DataStream.Delegate mockDelegate;

    @Before
    public void setup() {
        mockPeripheral = mock(BluetoothPeripheral.class);
        mockDelegate = mock(DataStream.Delegate.class);
        bearer = new MeshProxyBearer(mockPeripheral);
        bearer.setDelegate(mockDelegate);
    }

    @Test
    public void openSetsPreferredMtu() {
        bearer.open();

        verify(mockPeripheral, times(1)).setPreferredMtu(69);
    }

    @Test
    public void openCallsConnect() {
        bearer.open();

        verify(mockPeripheral, times(1)).connect();
    }

    @Test
    public void onConnectedDoDiscover() {
        when(mockPeripheral.getState()).thenReturn(BluetoothPeripheral.State.CONNECTED);

        bearer.open();

        bearer.bluetoothPeripheralChangedState(mockPeripheral);

        verify(mockPeripheral, times(1)).discover();
    }

    @Test
    public void onDiscoverySetNotifyOnDataOut() {
        UUID dataOut = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_MESH_PROXY_DATA_OUT);
        when(mockPeripheral.getState()).thenReturn(BluetoothPeripheral.State.CONNECTED);

        bearer.open();
        bearer.bluetoothPeripheralChangedState(mockPeripheral);
        bearer.bluetoothPeripheralDiscovered(mockPeripheral, true);

        verify(mockPeripheral, times(1)).set(dataOut, true);
    }

    @Test
    public void onNotifySetRequestHighPrio() {

        UUID dataOut = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_MESH_PROXY_DATA_OUT);
        when(mockPeripheral.getState()).thenReturn(BluetoothPeripheral.State.CONNECTED);

        bearer.open();
        bearer.bluetoothPeripheralChangedState(mockPeripheral);
        bearer.bluetoothPeripheralDiscovered(mockPeripheral, true);
        bearer.bluetoothPeripheralSet(mockPeripheral, dataOut, true, true);

        verify(mockPeripheral, times(1)).requestConnectionPriority(BluetoothPeripheral.Priority.HIGH);
    }

    @Test
    public void onProbablyBeaconReceivedRequestNormalPrio() {

        mockOpen();

        verify(mockPeripheral, times(1)).requestConnectionPriority(BluetoothPeripheral.Priority.BALANCED);
    }

    @Test
    public void onProbablyBeaconReceivedSetOpened() {

        mockOpen();

        verify(mockDelegate, times(1)).dataStreamChangedState(bearer);
        assertThat(bearer.getState(), equalTo(DataStream.State.OPENED));
    }

    @Test
    public void readDataComesThrough() {

        mockOpen();

        verify(mockDelegate, times(1)).dataStreamRead(bearer, new byte[0]);
    }

    @Test
    public void writeToDataIn() {
        mockOpen();

        UUID dataIn = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_MESH_PROXY_DATA_IN);
        byte[] data = {0, 1, 0};

        bearer.write(data);

        verify(mockPeripheral, times(1)).write(dataIn, data, false);
    }

    @Test
    public void writeCallsBack() {

        mockOpen();

        UUID dataIn = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_MESH_PROXY_DATA_IN);
        byte[] data = {0, 1, 0};

        bearer.write(data);
        bearer.bluetoothPeripheralWrote(mockPeripheral, dataIn, true);

        verify(mockDelegate).dataStreamWrote(bearer, data);
    }

    private void mockOpen() {
        UUID dataOut = UUID.fromString(GattAttributes.UUID_CHARACTERISTIC_MESH_PROXY_DATA_OUT);
        when(mockPeripheral.getState()).thenReturn(BluetoothPeripheral.State.CONNECTED);

        bearer.open();
        bearer.bluetoothPeripheralChangedState(mockPeripheral);
        bearer.bluetoothPeripheralDiscovered(mockPeripheral, true);
        bearer.bluetoothPeripheralSet(mockPeripheral, dataOut, true, true);
        bearer.bluetoothPeripheralRead(mockPeripheral, dataOut, new byte[0], true);
    }
}
