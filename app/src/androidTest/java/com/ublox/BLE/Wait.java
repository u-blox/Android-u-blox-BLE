package com.ublox.BLE;

public class Wait {
    public static void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted", e);
        }
    }
}
