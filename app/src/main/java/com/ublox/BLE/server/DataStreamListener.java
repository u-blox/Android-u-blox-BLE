package com.ublox.BLE.server;

import com.ublox.BLE.datapump.DataStream;

public interface DataStreamListener {
    void setDelegate(Delegate delegate);
    boolean isListening();
    String identifier();

    void startListen();
    void stopListen();

    interface Delegate {
        void dataStreamListenerAccepted(DataStreamListener listener, DataStream stream);
    }
}
