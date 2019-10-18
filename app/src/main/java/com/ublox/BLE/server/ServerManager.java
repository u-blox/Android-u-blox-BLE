package com.ublox.BLE.server;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.ublox.BLE.activities.MainActivity;
import com.ublox.BLE.bluetooth.BluetoothCentral;
import com.ublox.BLE.bluetooth.BluetoothPeripheral;
import com.ublox.BLE.datapump.DataPump;
import com.ublox.BLE.datapump.DataStream;
import com.ublox.BLE.bluetooth.SpsStream;
import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.utils.WrongParameterException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerManager implements DataStreamListener.Delegate, DataStream.Delegate, BluetoothCentral.Delegate, DataPump.Delegate {

    public static final String UNKNOWN_COMMAND = "Unknown command. Use \"help\" for more details.\n\n\r";
    public static final String COMMAND_HELP = "help";
    public static final String COMMAND_SCAN = "scan";
    public static final String COMMAND_TEST = "test";
    public static final String COMMAND_STOP = "stop";
    public static final String BUNDLE_SHOW_TX = "showTx";
    public static final String BUNDLE_SHOW_RX = "showRx";
    public static final String COMMAND_PARAM_TX = "tx";
    public static final String COMMAND_PARAM_RX = "rx";
    public static final String COMMAND_PARAM_PACKAGESIZE = "packagesize=";
    public static final String COMMAND_PARAM_BYTECOUNT = "bytecount=";
    public static final String BUNDLE_MTU = "mtu";
    public static final String BUNDLE_PACKETSIZE = "packetsize";
    public static final String COMMAND_PARAM_CREDITS = "credits";
    public static final String BUNDLE_WITH_CREDITS = "with_credits";

    private DataStreamListener serverSocket;
    private DataStream client;
    //private BluetoothAdapter bluetoothAdapter;
    private BluetoothCentral central;
    private Set<BluetoothDeviceRepresentation> devices;
    private Delegate delegate;
    private Map<String, String> helpCommands = new HashMap<>();
    private boolean isScanning;
    private boolean isTesting;

    private SpsStream dataStream;
    private DataPump pump;

    private long txCount;
    private long rxCount;
    private long txTime;
    private long rxTime;
    private long errors;


    /*private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    devices.add(new UBloxDevice(device));
                }
            };*/

    @Override
    public void dataStreamChangedState(DataStream stream) {
        if (client.getState() != DataStream.State.OPENED) {
            client = null;
        }
    }

    @Override
    public void dataStreamWrote(DataStream stream, byte[] data) {

    }

    @Override
    public void dataStreamRead(DataStream stream, byte[] data) {
        String command = new String(data);
        command = command.replace("\r\n", "");
        parseCommand(command);
    }

    @Override
    public void dataStreamListenerAccepted(DataStreamListener listener, DataStream stream) {
        if (client != null) {
            stream.open();
            stream.write("Busy. Closing.".getBytes());
            stream.close();
        } else {
            client = stream;
            client.setDelegate(this);
            client.open();
            delegate.serverLogged("Connection established");
        }
    }

    @Override
    public void centralChangedState(BluetoothCentral central) {
        isScanning = central.getState() == BluetoothCentral.State.SCANNING;
    }

    @Override
    public void centralFoundPeripheral(BluetoothCentral central, BluetoothPeripheral peripheral) {

    }

    @Override
    public void onTx(long bytes, long duration) {
        txCount = bytes;
        txTime = duration;
    }

    @Override
    public void onRx(long bytes, long duration) {
        rxCount = bytes;
        txTime = duration;
    }

    @Override
    public void updateMTUSize(int size) {

    }

    public interface Delegate {
        void serverLogged(String update);
        void onConnectToDevice(Bundle connectInfoBundle);
        boolean isConnected();
        void onDisconnectFromDevice();
    }

    Handler stopScanningHandler = new Handler();

    public ServerManager(BluetoothCentral central) {
        //this.bluetoothAdapter = bluetoothAdapter;
        this.central = central;
        this.central.setDelegate(this);
        devices = new HashSet<>();
        addHelpCommands();
    }

    private void addHelpCommands() {
        helpCommands.put("scan", "This command scans area for specified time and shows all " +
                "visible peripherals.\r\nHow to use it:\r\nscan [$time]\r\nScans for $time. Default" +
                " time is 5s.\r\n\n");
        helpCommands.put("stop", "This command stops all tasks\r\nHow to use it:\r\nstop\r\n\n");
        helpCommands.put("test", "This command starts test\r\nHow to use it:\r\ntest deviceName" +
                " [args ...] \r\nPossible args: \r\ntx - shows tx stats " +
                "during test \r\nrx - shows rx stats during test \r\ncredits - runs test using" +
                " credits\r\npackagesize=[$size] - runs test using $size as packagesize." +
                " Default $size is 20\r\nbytecount=[$size] - sends $size bytes in one test" +
                " request. Default $size is infinity (runs continuous DataPump)\r\n\n");
        helpCommands.put("default", "Supported commands:\r\n- help\r\n- scan\r\n- stop\r\n- test\r\nUse " +
                "help $commandName for more info on how to use this command. \r\n\n");
    }

    public boolean isRunning() {
        return serverSocket != null && serverSocket.isListening();
    }

    public boolean isBluetoothAdapterEnabled() {
        return true; //bluetoothAdapter.isEnabled();
    }

    public void registerListener(Delegate remoteActionsListener) {
        this.delegate = remoteActionsListener;
    }

    public void startServer(DataStreamListener listener) {
        serverSocket = listener;
        serverSocket.setDelegate(this);
        serverSocket.startListen();
        delegate.serverLogged("Server started");
    }

    public void stopServer() {
        if (!isRunning()) return;
        serverSocket.stopListen();
        delegate.serverLogged("Server stopped");
    }

    private void parseCommand(String line) {
        String buffer[];
        if (!TextUtils.isEmpty(line)) {
            buffer = line.split(" ");
            switch (buffer[0]) {
                case COMMAND_HELP:
                    if (buffer.length == 1) {
                        writeToClient(helpCommands.get("default"));
                    } else if (buffer.length == 2 && helpCommands.containsKey(buffer[1])) {
                        writeToClient(helpCommands.get(buffer[1]));
                    } else {
                        writeToClient(UNKNOWN_COMMAND);
                    }
                    break;
                case COMMAND_SCAN:
                    if (buffer.length == 1) {
                        startScanDevices(5);
                    } else if(buffer.length == 2) {
                        try {
                            startScanDevices(Integer.parseInt(buffer[1]));
                        } catch (NumberFormatException e) {
                            writeToClient(UNKNOWN_COMMAND);
                        }
                    } else {
                        writeToClient(UNKNOWN_COMMAND);
                    }
                    break;
                case COMMAND_TEST:
                    if(buffer.length > 1 ) {
                        /*boolean isDeviceFound = false;
                        for (BluetoothDeviceRepresentation device : devices) {
                            if((!TextUtils.isEmpty(device.getName()) && device.getName().replaceAll(" ", "_").equals(buffer[1].replaceAll(" ", "_")))
                                    || (!TextUtils.isEmpty(device.getAddress()) && device.getAddress().equals(buffer[1]))) {
                                isDeviceFound = true;
                                if (mRemoteActionsListener.isConnected()) {
                                    mRemoteActionsListener.onDisconnectFromDevice();
                                    writeToClient("Test stopped!\n\r");
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                writeToClient("Connecting to device...\n\r");
                                try {
                                    Looper.prepare();
                                    mRemoteActionsListener.onConnectToDevice(createConnectInfoBundle(device, Arrays.asList(buffer)));
                                } catch(NumberFormatException e) {
                                    writeToClient(UNKNOWN_COMMAND);
                                } catch(ArrayIndexOutOfBoundsException e) {
                                    writeToClient(UNKNOWN_COMMAND);
                                } catch(WrongParameterException e) {
                                    writeToClient(UNKNOWN_COMMAND);
                                }
                                break;
                            }
                        }
                        if(!isDeviceFound) {
                            writeToClient("Cannot connect to device " + buffer[1] + "\n\r");
                        }*/
                        BluetoothPeripheral peripheral = deviceWith(buffer[1]);
                        if (peripheral != null) {
                            TestSettings settings = parseTestSettings(Arrays.asList(buffer));
                            doTest(peripheral, settings);
                        } else {
                            writeToClient("Cannot connect to device " + buffer[1] + "\n\r");
                        }
                    } else {
                        writeToClient(UNKNOWN_COMMAND);
                    }
                    break;
                case COMMAND_STOP:
                    if(buffer.length == 1) {
                        if (isScanning) {
                            writeToClient("Scanning stopped!\n\r");
                            stopScanDevices();
                        }
                        if (isTesting) {
                            stopTest();
                        }
                    } else {
                        writeToClient(UNKNOWN_COMMAND);
                    }
                    break;
                default:
                    writeToClient(UNKNOWN_COMMAND);
            }
        }
    }

    private void doTest(BluetoothPeripheral peripheral, TestSettings settings) {
        dataStream = new SpsStream(peripheral);
        dataStream.setDelegate(new DataStream.Delegate() {
            @Override
            public void dataStreamChangedState(DataStream stream) {
                if (stream.getState() == DataStream.State.OPENED) {
                    pump = new DataPump(dataStream, ServerManager.this);
                    pump.setContinuousMode(true);

                    isTesting = true;
                    pump.startDataPump();
                }
            }

            @Override
            public void dataStreamWrote(DataStream stream, byte[] data) {}
            @Override
            public void dataStreamRead(DataStream stream, byte[] data) {}
        });
        dataStream.open();
    }

    private void stopTest() {
        pump.stopDataPump();
        dataStream.close();
        isTesting = false;
        pump = null;
        dataStream = null;
        String result = "RESULT:\n\r" + "TX: " + txCount + ",  duration: " + txTime +  "\n\r" +
            "RX: " + rxCount + ",  duration: " + rxTime + ", Errors: " + errors + "\n\r";
        writeToClient(result);
    }

    private Bundle createConnectInfoBundle(BluetoothDeviceRepresentation bluetoothDevice, List<String> testCommandParams) throws WrongParameterException {
        Bundle bundle = new Bundle();
        bundle.putParcelable(MainActivity.EXTRA_DEVICE, bluetoothDevice);

        for (int i = 2; i < testCommandParams.size(); i++) { // Do not check test command and device name
            if(testCommandParams.get(i).equals(COMMAND_PARAM_TX)) {
                bundle.putBoolean(BUNDLE_SHOW_TX, true);
            } else if(testCommandParams.get(i).equals(COMMAND_PARAM_RX)) {
                bundle.putBoolean(BUNDLE_SHOW_RX, true);
            } else if(testCommandParams.get(i).contains(COMMAND_PARAM_PACKAGESIZE)) {
                int mtuSize = Integer.valueOf(testCommandParams.get(i).substring(COMMAND_PARAM_PACKAGESIZE.length()));
                if(mtuSize > 0) {
                    bundle.putInt(BUNDLE_MTU, mtuSize);
                } else {
                    throw new WrongParameterException();
                }
            } else if(testCommandParams.get(i).contains(COMMAND_PARAM_BYTECOUNT)) {
                int packageSize = Integer.valueOf(testCommandParams.get(i).substring(COMMAND_PARAM_BYTECOUNT.length()));
                if (packageSize > 0) {
                    bundle.putInt(BUNDLE_PACKETSIZE, packageSize);
                } else {
                    throw new WrongParameterException();
                }
            } else if(testCommandParams.get(i).equals(COMMAND_PARAM_CREDITS)) {
                bundle.putBoolean(BUNDLE_WITH_CREDITS, true);
            } else {
                throw new WrongParameterException();
            }
        }
        return bundle;
    }

    private BluetoothPeripheral deviceWith(String id) {
        for (BluetoothPeripheral peripheral: central.getFoundPeripherals()) {
            if ((peripheral.name() != null && id.equals(peripheral.name().replaceAll(" ", "_"))) || id.equals(peripheral.identifier())) {
                return peripheral;
            }
        }
        return null;
    }

    private TestSettings parseTestSettings(List<String> testCommandParams) {
        TestSettings settings = new TestSettings();

        for (int i = 2; i < testCommandParams.size(); i++) { // Do not check test command and device name
            if(testCommandParams.get(i).equals(COMMAND_PARAM_TX)) {
                settings.showTx = true;
            } else if(testCommandParams.get(i).equals(COMMAND_PARAM_RX)) {
                settings.showRx = true;
            } else if(testCommandParams.get(i).contains(COMMAND_PARAM_PACKAGESIZE)) {
                int mtuSize = parseInt(testCommandParams.get(i).substring(COMMAND_PARAM_PACKAGESIZE.length()));
                if(mtuSize > 0) {
                    settings.prefMtu = mtuSize;
                }
            } else if(testCommandParams.get(i).contains(COMMAND_PARAM_BYTECOUNT)) {
                int packageSize = parseInt(testCommandParams.get(i).substring(COMMAND_PARAM_BYTECOUNT.length()));
                if (packageSize > 0) {
                    settings.packetSize = packageSize;
                }
            } else if(testCommandParams.get(i).equals(COMMAND_PARAM_CREDITS)) {
                settings.credits = true;
            }
        }
        return settings;
    }

    private int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void startScanDevices(int searchTimeInSeconds) {
        if (searchTimeInSeconds <= 0) {
            writeToClient("Wrong parameter, check \"help scan\"\n\r");
            return;
        }
        devices.clear();
        writeToClient("Scanning...\n\n\r");
        //bluetoothAdapter.startLeScan(mLeScanCallback);
        central.scan(new ArrayList<>());

        stopScanningHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    stopScanDevices();
                }
            }
        }, searchTimeInSeconds * 1000);
    }


    private void stopScanDevices() {
        //bluetoothAdapter.stopLeScan(mLeScanCallback);
        central.stop();
        stopScanningHandler.removeCallbacksAndMessages(null);
        writeToClient("Found peripherals:\n\n\r");
        writeToClient(writeScannedDevices());
    }

    private String writeScannedDevices() {
        StringBuffer deviceInfo = new StringBuffer();
        for (BluetoothPeripheral device : central.getFoundPeripherals()) {
            if(!TextUtils.isEmpty(device.name())) {
                deviceInfo.append(device.name().replaceAll(" ", "_"));
                deviceInfo.append(" ");
            }
            deviceInfo.append(device.identifier());
            deviceInfo.append("\n\r");
        }
        deviceInfo.append("\n\r");
        return deviceInfo.toString();
    }

    public void writeToClient(String message) {
        if (client != null) client.write(message.getBytes());
    }

    private class TestSettings {
        public boolean showTx;
        public boolean showRx;
        public int prefMtu;
        public int packetSize;
        public boolean credits;

        public TestSettings() {
            showRx = false;
            showTx = false;
            prefMtu = 23;
            packetSize = 20;
            credits = false;
        }
    }
}
