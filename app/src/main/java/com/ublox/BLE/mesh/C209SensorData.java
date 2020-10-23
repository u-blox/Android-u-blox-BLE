package com.ublox.BLE.mesh;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class C209SensorData {
    private long time;
    private double value;

    public C209SensorData(long time, double value) {
        this.time = time;
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public double getValue() {
        return value;
    }

}
