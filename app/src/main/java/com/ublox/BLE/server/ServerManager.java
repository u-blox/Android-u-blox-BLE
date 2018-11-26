package com.ublox.BLE.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.ublox.BLE.activities.MainActivity;
import com.ublox.BLE.interfaces.BluetoothDeviceRepresentation;
import com.ublox.BLE.utils.UBloxDevice;
import com.ublox.BLE.utils.WrongParameterException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerManager {

    private static final String TAG = ServerManager.class.getSimpleName();

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
    public static final String MESSAGE_DATA = "data";
    public static final String COMMAND_PARAM_CREDITS = "credits";
    public static final String BUNDLE_WITH_CREDITS = "with_credits";
    private static volatile ServerManager instance;

    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDeviceRepresentation> devices;
    private IRemoteActionsListener mRemoteActionsListener;
    private Map<String, String> helpCommands = new HashMap<>();
    private Integer port;
    private boolean isRunning;
    private boolean isScanning;

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    devices.add(new UBloxDevice(device));
                }
            };

    public interface IRemoteActionsListener {
        void onLogUpdate(String update);
        void onConnectToDevice(Bundle connectInfoBundle);
        boolean isConnected();
        void onDisconnectFromDevice();
    }

    Handler stopScanningHandler = new Handler();

    private ServerManager(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        devices = new HashSet<>();
        addHelpCommands();
    }

    public static ServerManager getInstance(BluetoothAdapter bluetoothAdapter) {
        if (instance == null) {
            synchronized (ServerManager.class) {
                if (instance == null) {
                    instance = new ServerManager(bluetoothAdapter);
                }
            }
        }
        return instance;
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
        return isRunning;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isBluetoothAdapterEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    public void registerListener(IRemoteActionsListener remoteActionsListener) {
        this.mRemoteActionsListener = remoteActionsListener;
    }

    public void startServer() {
        if (port != null && serverThread == null) {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverThread = new ServerThread();
            serverThread.start();
            isRunning = true;
            mRemoteActionsListener.onLogUpdate("Server started");
        }
    }

    public void stopServer() {
        if (serverThread != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverThread.interrupt();
            serverThread = null;
            isRunning = false;
            mRemoteActionsListener.onLogUpdate("Server stopped");
        }
    }

    private void parseCommand(String line) {
        String buffer[];
        if (!TextUtils.isEmpty(line)) {
            buffer = line.split(" ");
            switch (buffer[0]) {
                case COMMAND_HELP:
                    if (buffer.length == 1) {
                        writeToServer(helpCommands.get("default"));
                    } else if (buffer.length == 2 && helpCommands.containsKey(buffer[1])) {
                        writeToServer(helpCommands.get(buffer[1]));
                    } else {
                        writeToServer(UNKNOWN_COMMAND);
                    }
                    break;
                case COMMAND_SCAN:
                    if (buffer.length == 1) {
                        startScanDevices(5);
                    } else if(buffer.length == 2) {
                        try {
                            startScanDevices(Integer.parseInt(buffer[1]));
                        } catch (NumberFormatException e) {
                            writeToServer(UNKNOWN_COMMAND);
                        }
                    } else {
                        writeToServer(UNKNOWN_COMMAND);
                    }
                    break;
                case COMMAND_TEST:
                    if(buffer.length > 1 ) {
                        boolean isDeviceFound = false;
                        for (BluetoothDeviceRepresentation device : devices) {
                            if((!TextUtils.isEmpty(device.getName()) && device.getName().replaceAll(" ", "_").equals(buffer[1].replaceAll(" ", "_")))
                                    || (!TextUtils.isEmpty(device.getAddress()) && device.getAddress().equals(buffer[1]))) {
                                isDeviceFound = true;
                                if (mRemoteActionsListener.isConnected()) {
                                    mRemoteActionsListener.onDisconnectFromDevice();
                                    writeToServer("Test stopped!\n\r");
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                writeToServer("Connecting to device...\n\r");
                                try {
                                    mRemoteActionsListener.onConnectToDevice(createConnectInfoBundle(device, Arrays.asList(buffer)));
                                } catch(NumberFormatException e) {
                                    writeToServer(UNKNOWN_COMMAND);
                                } catch(ArrayIndexOutOfBoundsException e) {
                                    writeToServer(UNKNOWN_COMMAND);
                                } catch(WrongParameterException e) {
                                    writeToServer(UNKNOWN_COMMAND);
                                }
                                break;
                            }
                        }
                        if(!isDeviceFound) {
                            writeToServer("Cannot connect to device " + buffer[1] + "\n\r");
                        }
                    } else {
                        writeToServer(UNKNOWN_COMMAND);
                    }
                    break;
                case COMMAND_STOP:
                    if(buffer.length == 1) {
                        if (isScanning) {
                            writeToServer("Scanning stopped!\n\r");
                            stopScanDevices();
                        }
                        if (mRemoteActionsListener.isConnected()) {
                            mRemoteActionsListener.onDisconnectFromDevice();
                            writeToServer("Test stopped!\n\r");
                        }
                    } else {
                        writeToServer(UNKNOWN_COMMAND);
                    }
                    break;
                default:
                    writeToServer(UNKNOWN_COMMAND);
            }
        }
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

    private void startScanDevices(int searchTimeInSeconds) {
        if (searchTimeInSeconds <= 0) {
            writeToServer("Wrong parameter, check \"help scan\"\n\r");
            return;
        }
        devices.clear();
        writeToServer("Scanning...\n\n\r");
        bluetoothAdapter.startLeScan(mLeScanCallback);
        isScanning = true;

        stopScanningHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    stopScanDevices();
                }
            }
        }, searchTimeInSeconds * 1000);
    }


    private synchronized void stopScanDevices() {
        bluetoothAdapter.stopLeScan(mLeScanCallback);
        isScanning = false;
        stopScanningHandler.removeCallbacksAndMessages(null);
        writeToServer("Found peripherals:\n\n\r");
        writeToServer(writeScannedDevices(devices));
    }

    private String writeScannedDevices(Set<BluetoothDeviceRepresentation> devices) {
        StringBuffer deviceInfo = new StringBuffer();
        for (BluetoothDeviceRepresentation device : devices) {
            if(!TextUtils.isEmpty(device.getName())) {
                deviceInfo.append(device.getName().replaceAll(" ", "_"));
                deviceInfo.append(" ");
            }
            deviceInfo.append(device.getAddress());
            deviceInfo.append("\n\r");
        }
        deviceInfo.append("\n\r");
        return deviceInfo.toString();
    }

    public void writeToServer(String message) {
        if (serverThread != null) {
            serverThread.writeToServer(message);
        }
    }

    public class ServerThread extends Thread {

        Socket client;
        BufferedReader reader;

        private WriterRunnable writer;

        public void run() {
            client = null;
            try {
                serverSocket.bind(new InetSocketAddress(port));
                client = serverSocket.accept();

                String clientAddress = client.getInetAddress().getHostAddress();
                mRemoteActionsListener.onLogUpdate("Connection established with " + clientAddress);
                reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                writer = new WriterRunnable(client);
                writer.start();

                String line;
                try {
                    while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                        parseCommand(line);
                    }
                    reader.close();
                } catch (SocketException se) {
                    Log.w(TAG, "TCP server stop caused socket closing");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                close();

            } catch (SocketException se) {
                Log.w(TAG,"Closed socket in accepting state!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void close(){
            if (client != null) {
                try {
                    client.close();
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            writer.interrupt();
        }


        public void writeToServer(String data) {
            Bundle bundle = new Bundle();
            bundle.putString(MESSAGE_DATA, data);
            Message message = new Message();
            message.setData(bundle);
            writer.handler.sendMessage(message);
        }
    }

    public class WriterRunnable extends Thread {

        Handler handler;
        BufferedWriter writer;

        public WriterRunnable(Socket client) throws IOException {
            writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        }

        @Override
        public void interrupt() {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            handler.removeCallbacksAndMessages(null);
            super.interrupt();
        }

        @Override
        public void run() {
            Looper.prepare();
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    try {
                        writer.write(msg.getData().getString(MESSAGE_DATA));
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            Looper.loop();
        }
    }
}
