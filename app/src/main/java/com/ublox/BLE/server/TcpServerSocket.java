package com.ublox.BLE.server;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;

public class TcpServerSocket implements DataStreamListener {
    private int port;
    private Delegate delegate;
    private ServerSocket socket;
    private LoopingThread listener;

    public TcpServerSocket(int port) {
        this.port = port;
    }

    @Override
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isListening() {
        return listener != null && listener.isAlive();
    }

    @Override
    public String identifier() {
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (!address.isLoopbackAddress()) {
                        String id = address.getHostAddress();
                        if (!id.contains(":")) {
                            return id;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void startListen() {
        if (identifier() == null || isListening()) return;

        try {
            socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            Handler handler = new Handler(Looper.getMainLooper());
            listener = new LoopingThread() {
                @Override
                public void iteration() {
                    try {
                        Socket client = socket.accept();
                        handler.post(() -> accept(client));
                    } catch (Exception e) {
                        stopRunning();
                        handler.post(() -> stopListen());
                    }
                }
            };
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void accept(Socket client) {
        if (delegate != null) delegate.dataStreamListenerAccepted(this, new TcpStream(client));
    }

    @Override
    public void stopListen() {
        if (!isListening()) return;
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
