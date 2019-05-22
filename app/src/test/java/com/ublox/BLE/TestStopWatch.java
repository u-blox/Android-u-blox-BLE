package com.ublox.BLE;

import com.ublox.BLE.datapump.StopWatch;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestStopWatch {
    StopWatch stopWatch;

    @Before
    public void setup() {
        stopWatch = new StopWatch(new IncrementTimer()); //In production should be like SystemClock::elapsedRealtimeNanos
    }

    @Test
    public void newStopWatchHasElapsedTimeZero() {
        assertThat(stopWatch.elapsedTime(), equalTo(0L));
    }

    @Test
    public void startedStopWatchIncreasesInTime() {
        stopWatch.start();
        assertThat(stopWatch.elapsedTime(), equalTo(1L));
    }

    @Test
    public void stopWatchDoesNotIncreaseAfterStop() {
        stopWatch.start();
        stopWatch.stop();
        assertThat(stopWatch.elapsedTime(), equalTo(stopWatch.elapsedTime()));
    }

    @Test
    public void startingAstartedStopWatchDoesNothing() {
        stopWatch.start();
        stopWatch.elapsedTime();
        stopWatch.start();
        assertThat(stopWatch.elapsedTime(), equalTo(2L));
    }

    @Test
    public void stoppingAStoppedStopWatchDoesNothing() {
        stopWatch.start();
        stopWatch.stop();
        long middleTime = stopWatch.elapsedTime();
        stopWatch.stop();
        assertThat(stopWatch.elapsedTime(), equalTo(middleTime));
    }

    @Test
    public void startingAStoppedStopWatchResetsTime() {
        stopWatch.start();
        stopWatch.stop();
        stopWatch.start();
        assertThat(stopWatch.elapsedTime(), equalTo(1L));
    }

    /** Mocks a monotonically increasing clock */
    class IncrementTimer implements StopWatch.NowProvider {
        private long ns = 0;

        public long now() {
            return ++ns;
        }
    }
}
