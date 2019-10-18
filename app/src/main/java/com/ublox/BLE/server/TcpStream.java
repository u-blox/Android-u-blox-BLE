package com.ublox.BLE.server;

import android.os.Handler;
import android.os.Looper;

import com.ublox.BLE.datapump.DataStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class TcpStream implements DataStream {
    private State state;
    private Socket socket;
    private LoopingThread reader;
    private Delegate delegate;

    public TcpStream(Socket socket) {
        state = State.CLOSED;
        this.socket = socket;
    }

    public State getState() {
        return state;
    }

    public void open() {
        if (state == State.OPENED) return;
        try {
            startReading();
            setState(State.OPENED);
        } catch (Exception e) {
            setState(State.ERROR);
        }
    }

    private void startReading() throws IOException {
        Handler handler = new Handler(Looper.getMainLooper());
        InputStream input = socket.getInputStream();
        reader = new LoopingThread() {
            @Override
            public void iteration() {
                try {
                    byte[] buf = new byte[1024];
                    int read = input.read(buf);
                    if (read > 0) {
                        byte[] data = Arrays.copyOf(buf, read);
                        handler.post(() -> {
                            didRead(data);
                        });
                    } else {
                        closeStreamWith(DataStream.State.CLOSED);
                    }
                } catch (Exception e) {
                    closeStreamWith(DataStream.State.ERROR);
                }
            }

            private void closeStreamWith(DataStream.State state) {
                stopRunning();
                handler.post(() -> closeWith(state));
            }
        };
        reader.start();
    }

    public void close() {
        closeWith(State.CLOSED);
    }

    private void didRead(byte[] data) {
        if (delegate != null) delegate.dataStreamRead(this, data);
    }

    private void didWrite(byte[] data) {
        if (delegate != null) delegate.dataStreamWrote(this, data);
    }

    private void closeWith(State state) {
        if (this.state != State.OPENED) return;
        try {
            socket.close();
        } catch (IOException ignored) {
            // Literally nothing to do if we exception here.
        }
        reader.stopRunning();
        setState(state);
    }

    @Override
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(byte[] data) {
        byte[] buf = Arrays.copyOf(data, data.length);
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread() {
            @Override
            public void run() {
                try {
                    OutputStream output = socket.getOutputStream();
                    output.write(buf);
                    output.flush();
                    handler.post(() -> didWrite(buf));
                } catch (Exception e) {
                    handler.post(() -> closeWith(DataStream.State.ERROR));
                }
            }
        }.start();
    }

    private void setState(State newState) {
        if (newState != state) {
            state = newState;
            if (delegate != null) delegate.dataStreamChangedState(this);
        }
    }
}
