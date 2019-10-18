package com.ublox.BLE.datapump;

import android.os.SystemClock;

/**
 * Class encapsulating data pump test feature based on bluetoothLeService communication
 */
public class DataPump implements DataStream.Delegate {
    private DataStream stream;
    private Delegate dataPumpListener;
    private boolean isTestRunning;
    private boolean continuousMode;
    private boolean bitErrorActive;
    private long txCounter;
    private long rxCounter;
    private StopWatch txTimer;
    private StopWatch rxTimer;
    private int packetSize = 20; // default value, to be discussed when changing test fragment flow
    private int mtuSize = 23;
    private int sizeOfRemainingPacket;
    private int sizeOfDividedPacket;

    /**
     * Constructs a new DataPump over a stream, reporting to a listener and fetching timing events from the NowProvider
     * This is intended for testing purposes to mock out any default systems clocks
     */
    public DataPump(DataStream stream, Delegate dataPumpListener, StopWatch.NowProvider time) {
        this.dataPumpListener = dataPumpListener;
        this.stream = stream;
        stream.setDelegate(this);
        txTimer = new StopWatch(time);
        rxTimer = new StopWatch(time);
    }

    /**
     * Constructs a new DataPump over a stream and reporting to a listener with a default NowProvider of android.os.SystemClock.elapsedRealtimeNanos
     */
    public DataPump(DataStream stream, Delegate dataPumpListener) {
        this(stream, dataPumpListener, SystemClock::elapsedRealtimeNanos);
    }

    @Override
    public void dataStreamChangedState(DataStream stream) {

    }

    /**
     * Callback for handling async responses from underlying stream
     */
    @Override
    public void dataStreamWrote(DataStream stream, byte[] data) {
        long duration = txTimer.elapsedTime();
        txCounter += data.length;
        isTestRunning = isTestRunning && (continuousMode || sizeOfRemainingPacket != 0);
        if (isTestRunning) {
            sendData();
        }
        dataPumpListener.onTx(txCounter, duration);
    }

    /**
     * Callback for handling async responses from underlying stream
     */
    @Override
    public void dataStreamRead(DataStream stream, byte[] data) {
        rxTimer.start();
        if (dataPumpListener != null) {
            rxCounter += data.length;
            dataPumpListener.onRx(rxCounter, rxTimer.elapsedTime());
        }
    }

    /**
     * Returns whether or not the datapump is currently pumping data
     */
    public boolean isTestRunning() {
        return isTestRunning;
    }

    /**
     * Set to use bit error, if true any byte 5 will be corrupted to 0
     */
    public void setBitErrorActive(boolean bitErrorActive) {
        this.bitErrorActive = bitErrorActive;
    }

    /**
     * Sets the packet size
     */
    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    /**
     * Starts pumping data down the stream
     */
    public void startDataPump() {
        txCounter = 0;
        isTestRunning = true;
        txTimer.start();
        sizeOfRemainingPacket = 0;
        sendData();
    }

    /**
     * Stop pumping data.
     */
    public void stopDataPump() {
        isTestRunning = false;
        txTimer.stop();
    }

    /**
     * Reset data pump state
     */
    public void resetDataPump() {
        rxCounter = 0;
        txCounter = 0;
        txTimer.stop();
        rxTimer.stop();
        if (isTestRunning) txTimer.start();
    }

    /**
     * Reset more of data pump state
     */
    public void reset() {
        resetDataPump();
        packetSize = 20;
        mtuSize = 23;
        bitErrorActive = false;
    }

    /**
     * Broadcast MTU size change to listener
     */
    public void updateMtuSizeChanged(int size) {
        dataPumpListener.updateMTUSize(size);
        mtuSize = size;
    }

    /**
     * Sets whether or not to continuously pump data (if false will stop after sending first packet)
     */
    public void setContinuousMode(boolean enabled) {
        this.continuousMode = enabled;
    }

    private void sendData() {
        stream.write(createPacket());
    }

    private byte[] createPacket() {
        if(sizeOfRemainingPacket == 0) {
            sizeOfRemainingPacket = packetSize;
        }

        int from = packetSize - sizeOfRemainingPacket;
        sizeOfDividedPacket = mtuSize - 3 > sizeOfRemainingPacket ?
            sizeOfRemainingPacket : mtuSize - 3;
        sizeOfRemainingPacket -= sizeOfDividedPacket;
        return fillPacketWithBytes(from);
    }

    private byte[] fillPacketWithBytes(int from) {
        int data = from;
        byte[] packet = new byte[sizeOfDividedPacket];
        for (int i = 0; i < sizeOfDividedPacket; i++, data++) {
            packet[i] = (bitErrorActive && i == 5) ? (byte) 0 : (byte) data;
        }
        return packet;
    }

    /**
     * Interface defining data pump test events
     */
    public interface Delegate {
        void onTx(long bytes, long duration);
        void onRx(long bytes, long duration);
        void updateMTUSize(int size);
    }
}