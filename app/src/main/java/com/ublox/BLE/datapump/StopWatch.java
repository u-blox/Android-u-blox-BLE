package com.ublox.BLE.datapump;

public class StopWatch {
    private NowProvider time;
    private boolean running;
    private long startTime;
    private long stopTime;

    public StopWatch(NowProvider time) {
        this.time = time;
        running = false;
        startTime = 0L;
        stopTime = 0L;
    }

    public void start() {
        if (running) return;
        startTime = time.now();
        running = true;
    }

    public void stop() {
        if (!running) return;
        stopTime = time.now();
        running = false;
    }

    public long elapsedTime() {
        return (running ? time.now() : stopTime) - startTime;
    }

    public interface NowProvider {
        long now();
    }
}
