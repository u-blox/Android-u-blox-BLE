package com.ublox.BLE.mesh;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import no.nordicsemi.android.meshprovisioner.opcodes.ApplicationMessageOpCodes;

import static no.nordicsemi.android.meshprovisioner.opcodes.ApplicationMessageOpCodes.LIGHT_HSL_STATUS;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class C209Node {
    private static final int GENERIC_ON_POWER_UP_STATUS = 0x8212;
    private static final int SENSOR_STATUS = 0x52;

    private int address;
    private LinkedList<C209SensorData> illuminance;
    private LinkedList<C209SensorData> temperature;
    private LinkedList<C209SensorData> humidity;
    private LinkedList<C209SensorData> pressure;
    private int ledLightness, ledHue, ledSaturation;
    private int x, y, z;
    private String alias;

    public C209Node(int address) {
        this.address = address;
        illuminance = new LinkedList<>();
        temperature = new LinkedList<>();
        humidity = new LinkedList<>();
        pressure = new LinkedList<>();
        x = y = z = 0;
        ledLightness = ledHue = ledSaturation = 0;
    }

    public int address() {
        return address;
    }

    public String getName() {
        return alias == null || alias.isEmpty() ? String.format("Node: %d", address) : alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void offerMeshMessage(int opCode, byte[] parameters) {
        if (opCode == ApplicationMessageOpCodes.GENERIC_ON_OFF_STATUS ||
            opCode == GENERIC_ON_POWER_UP_STATUS) {
            if (parameters[0] == 0x00) {
                ledLightness = ledSaturation = ledHue = 0;
            } else if (parameters[0] == 0x01) {
                ledLightness = ledSaturation = ledHue = 0xFFFF;
            }
        }
        if (opCode == LIGHT_HSL_STATUS && parameters.length >= 6) {
            ledLightness = toIntLittleEndian(parameters, 0, 2);
            ledHue = toIntLittleEndian(parameters, 2, 4);
            ledSaturation = toIntLittleEndian(parameters, 4, 6);
        }
        if (opCode == SENSOR_STATUS) {
            long now = System.currentTimeMillis();
            byte[][] sensorData = parseSensorData(parameters);
            if (sensorData[0].length > 0) illuminance.add(new C209SensorData(now, 0.01 * toIntLittleEndian(sensorData[0])));
            if (sensorData[1].length > 0) temperature.add(new C209SensorData(now, 0.5 * sensorData[1][0]));
            if (sensorData[2].length > 0) humidity.add(new C209SensorData(now, 0.01 * toIntLittleEndian(sensorData[2])));
            if (sensorData[3].length > 0) pressure.add(new C209SensorData(now, 0.1 * toIntLittleEndian(sensorData[3])));
            if (sensorData[4].length >= 6) {
                x = toIntLittleEndian(sensorData[4], 0, 2);
                y = toIntLittleEndian(sensorData[4], 2, 4);
                z = toIntLittleEndian(sensorData[4], 4, 6);
            }
        }
    }

    public List<C209SensorData> getIlluminance() {
        return Collections.unmodifiableList(illuminance);
    }

    public List<C209SensorData> getTemperature() {
        return Collections.unmodifiableList(temperature);
    }

    public List<C209SensorData> getHumidity() {
        return Collections.unmodifiableList(humidity);
    }

    public List<C209SensorData> getPressure() {
        return Collections.unmodifiableList(pressure);
    }

    public int getLedLuminance() {
        return ledLightness;
    }

    public int getLedHue() {
        return ledHue;
    }

    public int getLedSaturation() {
        return ledSaturation;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void clearDataOlderThan(long timeStamp) {
        clearOldDataFor(illuminance, timeStamp);
        clearOldDataFor(temperature, timeStamp);
        clearOldDataFor(humidity, timeStamp);
        clearOldDataFor(pressure, timeStamp);
    }

    private void clearOldDataFor(LinkedList<C209SensorData> sensorData, long olderThan) {
        while (!sensorData.isEmpty() && sensorData.getFirst().getTime() < olderThan) {
            sensorData.removeFirst();
        }
    }

    private byte[][] parseSensorData(byte[] rawData) {
        byte[][] sensorData = new byte[5][0];
        int i = 0;
        while (i < rawData.length) {
            int propertyId;
            int next;
            if (rawData[i] % 2 == 0) {
                propertyId = ((rawData[i] & 0xE0) >> 5) + ((rawData[i + 1] & 0xFF) << 3);
                next = (rawData[i] & 0x1E) >> 1;
                i += 2;
            } else {
                propertyId = (rawData[i + 1] & 0xFF) + ((rawData[i + 2] & 0xFF) << 8);
                next = (rawData[i] & 0xFE) >> 1;
                i += 3;
            }
            next += i;

            int index = -1;
            switch (propertyId) {
                case 0x004E:
                    index = 0;
                    break;
                case 0x004F:
                    index = 1;
                    break;
                case 0x2A6F:
                    index = 2;
                    break;
                case 0x2A6D:
                    index = 3;
                    break;
                case 0xAAAA:
                    index = 4;
                    break;
            }
            if (index != -1) {
                sensorData[index] = Arrays.copyOfRange(rawData, i, next);
            }
            i = next;
        }

        return sensorData;
    }

    private int toIntLittleEndian(byte[] bytes) {
        return toIntLittleEndian(bytes, 0, bytes.length);
    }

    private int toIntLittleEndian(byte[] bytes, int from, int to) {
        int value = 0;
        int i = to - 1;
        while (i >= from) {
            value = value << 8;
            value += (bytes[i] & 0xFF);
            i--;
        }
        return value;
    }

}
