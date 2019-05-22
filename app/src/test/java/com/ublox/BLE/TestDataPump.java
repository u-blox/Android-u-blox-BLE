package com.ublox.BLE;

import com.ublox.BLE.datapump.DataPump;
import com.ublox.BLE.datapump.DataPumpDelegate;
import com.ublox.BLE.datapump.DataStream;
import com.ublox.BLE.datapump.DataStreamDelegate;
import com.ublox.BLE.datapump.StopWatch;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

public class TestDataPump {
    MockDataStream stream;
    MockDataPumpDelegate delegate;
    DataPump pump;

    @Before
    public void setup() {
        stream = new MockDataStream();
        delegate = new MockDataPumpDelegate();
        pump = new DataPump(stream, delegate, new MockNowProvider());
    }

    @Test
    public void startingDataPumpSendsFirstPacket() {
        pump.startDataPump();
        assertThat(stream.writeCalls, equalTo(1));
    }

    @Test
    public void dataPumpIsNotRunningAfterFinishSendingPacketInSingleMode() {
        pump.startDataPump();
        pump.onWrite(new byte[20]);
        assertThat(pump.isTestRunning(), equalTo(false));
    }

    @Test
    public void dataPumpIsStillRunningAfterSendingPacketInContinuous() {
        pump.setContinuousMode(true);
        pump.startDataPump();
        pump.onWrite(new byte[20]);
        assertThat(pump.isTestRunning(), equalTo(true));
    }

    @Test
    public void dataPumpIsStillRunningWhenSendingSinglePacketLargerThanMtu() {
        pump.setPacketSize(32);
        pump.startDataPump();
        pump.onWrite(new byte[20]);
        assertThat(pump.isTestRunning(), equalTo(true));
    }

    @Test
    public void continuousDoesntRestartStoppedPumpOnCallback() {
        pump.startDataPump();
        pump.stopDataPump();
        pump.onWrite(new byte[20]);
        assertThat(pump.isTestRunning(), equalTo(false));
    }

    @Test
    public void startingDataPumpStartStopWatch() {
        pump.startDataPump();
        pump.onWrite(new byte[20]);
        assertThat(delegate.lastDuration, greaterThan(0L));
    }

    @Test
    public void stoppingDataPumpStopsStopWatch() {
        pump.startDataPump();
        pump.stopDataPump();
        pump.onWrite(new byte[20]);
        long duration = delegate.lastDuration;
        pump.onWrite(new byte[20]);
        assertThat(delegate.lastDuration, equalTo(duration));
    }

    @Test
    public void receivingDataStartsStopWatch() {
        pump.onRead(new byte[20]);
        pump.onRead(new byte[20]);
        assertThat(delegate.lastDuration, greaterThan(0L));
    }

    @Test
    public void resetEnsuresReceiveStopWatchIsReset() {
        pump.onRead(new byte[20]);
        long duration = delegate.lastDuration;
        pump.resetDataPump();
        pump.onRead(new byte[20]);
        assertThat(delegate.lastDuration, lessThanOrEqualTo(duration));
    }

    @Test
    public void correctlyKeepTrackOfReportedSentBytes() {
        pump.onWrite(new byte[20]);
        pump.onWrite(new byte[12]);
        assertThat(delegate.lastBytes, equalTo(32L));
    }

    @Test
    public void correctlyKeepTrackOfReceivedBytes() {
        pump.onRead(new byte[20]);
        pump.onRead(new byte[20]);
        pump.onRead(new byte[20]);
        pump.onRead(new byte[4]);
        assertThat(delegate.lastBytes, equalTo(64L));
    }

    @Test
    public void resetDataPumpResetsReceivedBytes() {
        pump.onRead(new byte[20]);
        pump.resetDataPump();
        pump.onRead(new byte[20]);
        assertThat(delegate.lastBytes, equalTo(20L));
    }

    class MockDataStream implements DataStream {
        DataStreamDelegate delegate;
        int writeCalls;

        @Override
        public void setDelegate(DataStreamDelegate delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(byte[] data) {
            writeCalls++;
        }
    }

    class MockDataPumpDelegate implements DataPumpDelegate {
        long lastBytes;
        long lastDuration;

        @Override
        public void onTx(long bytes, long duration) {
            lastBytes = bytes;
            lastDuration = duration;
        }

        @Override
        public void onRx(long bytes, long duration) {
            lastBytes = bytes;
            lastDuration = duration;
        }

        @Override
        public void updateMTUSize(int size) {

        }
    }

    class MockNowProvider implements StopWatch.NowProvider {
        private long ns;

        @Override
        public long now() {
            return ++ns;
        }
    }
}
