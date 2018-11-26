package com.ublox.BLE.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ublox.BLE.R;
import com.ublox.BLE.activities.MainActivity;
import com.ublox.BLE.server.ServerManager;


import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.WIFI_SERVICE;

public class RemoteControlFragment extends Fragment implements ServerManager.IRemoteActionsListener{

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MAX_LOG_LINES = 500;
    private static final int NUMBER_OF_REMOVABLE_LINES = 100;

    private Button btStartServer;
    private TextView tvIPAddress;
    private TextView tvLogs;
    private boolean isResultPresented;
    private long lastUpdateOfRx;
    private long lastUpdateOfTx;
    private String txData = "0 B";
    private String rxData = "0 B";
    private String txAvg = "0 kbps";
    private String rxAvg = "0 kbps";

    private ServerManager sm;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remote, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvIPAddress = view.findViewById(R.id.tvIPAddress);
        btStartServer = view.findViewById(R.id.btStart);
        tvLogs = view.findViewById(R.id.tvLogs);
        tvLogs.setMovementMethod(new ScrollingMovementMethod());
        final EditText etPort = view.findViewById(R.id.etPort);

        setIPAddress();

        etPort.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    etPort.setText("");
                }
            }
        });

        BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);

        sm = ServerManager.getInstance(bluetoothManager.getAdapter());
        sm.registerListener(this);

        btStartServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(etPort.getText())) {
                    sm.setPort(Integer.parseInt(etPort.getText().toString()));
                    if (!sm.isRunning()) {
                        sm.startServer();
                        btStartServer.setText(R.string.remote_start_button);
                    } else {
                        sm.stopServer();
                        btStartServer.setText(R.string.remote_stop_button);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!sm.isBluetoothAdapterEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            getActivity().finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String intToStringAddress(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    private void setIPAddress() {
        WifiManager wm = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wm.getConnectionInfo();
        int address = wifiInfo.getIpAddress();
        tvIPAddress.setText(intToStringAddress(address));
    }

    @Override
    public void onLogUpdate(final String update) {
        SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");
        final String currentTime = sdf.format(new Date());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLogs.append(currentTime + update + "\n");
                if(tvLogs.getLineCount() >= MAX_LOG_LINES) {
                    removeLinesFromLog();
                }
            }
        });
    }

    @Override
    public void onConnectToDevice(Bundle connectInfoBundle) {
        isResultPresented = false;
        ((MainActivity)getActivity()).connectToDeviceByRemoteControl(connectInfoBundle);
    }

    @Override
    public void onDisconnectFromDevice() {
        ((MainActivity)getActivity()).disconnectFromDeviceByRemoteControl();
        writeResult();
        resetTransmissionValues();
    }

    @Override
    public boolean isConnected() {
        return ((MainActivity)getActivity()).isConnected();
    }

    private void removeLinesFromLog() {
        tvLogs.getEditableText().delete(tvLogs.getLayout().getLineStart(0),
                tvLogs.getLayout().getLineEnd(NUMBER_OF_REMOVABLE_LINES));
    }

    public void writeRxData(String rxValue, String average) {
        rxData = rxValue;
        rxAvg = average;
        if(((System.currentTimeMillis() - lastUpdateOfRx) > 1000)) {
            onLogUpdate("RX: " + rxValue + ", Avg. speed: " + average + "\n\r");
            lastUpdateOfRx = System.currentTimeMillis();
        }
    }

    public void writeTxData(String txValue, String average) {
        txData = txValue;
        txAvg = average;
        if(((System.currentTimeMillis() - lastUpdateOfTx) > 1000)) {
            onLogUpdate("TX: " + txValue + ", Avg. speed: " + average + "\n\r");
            lastUpdateOfTx = System.currentTimeMillis();
        }
    }


    public void writeResult() {
        if (!isResultPresented) {
            isResultPresented = true;
            String result = "RESULT:\n\r" + "TX: " + txData + ",  Avg. speed: " + txAvg +  "\n\r" +
                    "RX: " + rxData + ",  Avg. speed: " + rxAvg + "\n\r";
            onLogUpdate(result);
        }
    }

    public void writeTransferData(String data) {
        sm.writeToServer(data);
    }

    public void resetTransmissionValues() {
        lastUpdateOfRx = 0;
        lastUpdateOfTx = 0;
        txData = "0 B";
        rxData = "0 B";
        txAvg = "0 kbps";
        rxAvg = "0 kbps";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sm != null) {
            sm.stopServer();
        }
    }
}
