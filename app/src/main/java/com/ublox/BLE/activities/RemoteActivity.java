package com.ublox.BLE.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ublox.BLE.R;
import com.ublox.BLE.bluetooth.BluetoothScanner;
import com.ublox.BLE.server.DataStreamListener;
import com.ublox.BLE.server.ServerManager;
import com.ublox.BLE.server.TcpServerSocket;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RemoteActivity extends Activity implements ServerManager.Delegate {
    static final int MAX_LOG_LINES = 500;
    static final int NUMBER_OF_REMOVABLE_LINES = 100;

    Button startStopButton;
    TextView ipAddress;
    EditText portInput;
    TextView logArea;
    private ServerManager server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_remote);

        startStopButton = findViewById(R.id.btStart);
        ipAddress = findViewById(R.id.tvIPAddress);
        portInput = findViewById(R.id.etPort);
        logArea = findViewById(R.id.tvLogs);
        logArea.setMovementMethod(new ScrollingMovementMethod());

        BluetoothAdapter adapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        server = new ServerManager(new BluetoothScanner(adapter));
        server.registerListener(this);

        startStopButton.setOnClickListener(view -> {
            if(!TextUtils.isEmpty(portInput.getText())) {
                if (server.isRunning()) {
                    server.stopServer();
                } else {
                    try {
                        int port = Integer.parseInt(portInput.getText().toString());
                        DataStreamListener socket = new TcpServerSocket(port);
                        server.startServer(socket);
                        ipAddress.setText(socket.identifier());
                    } catch (NumberFormatException ignored){}
                }
                startStopButton.setText(server.isRunning() ? R.string.remote_start_button : R.string.remote_stop_button);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        server.stopServer();
    }

    @Override
    public void serverLogged(String update) {
        SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");
        final String currentTime = sdf.format(new Date());
        runOnUiThread(() -> {
            logArea.append(currentTime + update + "\n");
            if (logArea.getLineCount() >= MAX_LOG_LINES) {
                removeLinesFromLog();
            }
        });
    }

    private void removeLinesFromLog() {
        logArea.getEditableText().delete(logArea.getLayout().getLineStart(0),
            logArea.getLayout().getLineEnd(NUMBER_OF_REMOVABLE_LINES));
    }

    @Override
    public void onConnectToDevice(Bundle connectInfoBundle) {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void onDisconnectFromDevice() {

    }
}
