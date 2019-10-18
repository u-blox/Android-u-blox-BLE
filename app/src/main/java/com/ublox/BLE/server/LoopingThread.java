package com.ublox.BLE.server;

public abstract class LoopingThread extends Thread {
    private boolean keepRunning;

    public LoopingThread() {
        keepRunning = true;
    }

    @Override
    public void run() {
        while (keepRunning) {
            iteration();
        }
    }

    public void stopRunning() {
        if (isAlive()) {
            keepRunning = false;
        }
    }

    public abstract void iteration();
}
