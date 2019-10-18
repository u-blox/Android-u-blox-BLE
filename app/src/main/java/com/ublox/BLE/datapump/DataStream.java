package com.ublox.BLE.datapump;

public interface DataStream {
    State getState();
    void setDelegate(Delegate delegate);
    void open();
    void close();
    void write(byte[] data);

    enum State {CLOSED, OPENED, ERROR}

    interface Delegate {
        void dataStreamChangedState(DataStream stream);
        void dataStreamWrote(DataStream stream, byte[] data);
        void dataStreamRead(DataStream stream, byte[] data);
    }
}
