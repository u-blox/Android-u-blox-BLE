package com.ublox.BLE;

import com.ublox.BLE.mesh.C209Node;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import no.nordicsemi.android.meshprovisioner.opcodes.ApplicationMessageOpCodes;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;

public class TestC209Node {
    private static final int SENSOR_STATUS_OP = 0x52;

    C209Node node;

    @Before
    public void setup() {
        node = new C209Node(1);
    }

    @Test
    public void correctlyParsesOutSensorValues() {
        byte[] parameters = {
            0x07, 0x4E, 0x0, 0x50, b(0xC3), 0x0, // 500 Lux
            0x03, 0x4F, 0x0, 0x32, // 25 C
            0x05, 0x6F, 0x2A, 0x64, 0x19, // 65% humidity
            0x09, 0x6D, 0x2A, 0x50, 0x69, 0x0F, 0x0, // 101 kPa
            0x0D, b(0xAA), b(0xAA), 0x0, 0x40, 0x01, 0x40, b(0xFF), 0x7F //16384;16385;32767
        };
        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);

        assertThat(node.getIlluminance().get(0).getValue(), is(500.0));
        assertThat(node.getTemperature().get(0).getValue(), is(25.0));
        assertThat(node.getHumidity().get(0).getValue(), is(65.0));
        assertThat(node.getPressure().get(0).getValue(), is(101000.0));
        assertThat(node.getX(), is(16384));
        assertThat(node.getY(), is(16385));
        assertThat(node.getZ(), is(32767));
    }

    @Test
    public void correctlySkipMissingIlluminance() {
        byte[] parameters = {
            0x03, 0x4F, 0x0, 0x32, // 25 C
            0x05, 0x6F, 0x2A, 0x64, 0x19, // 65% humidity
            0x09, 0x6D, 0x2A, 0x50, 0x69, 0x0F, 0x0, // 101 kPa
            0x0D, b(0xAA), b(0xAA), 0x0, 0x40, 0x01, 0x40, b(0xFF), 0x7F //16384;16385;32767
        };
        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);

        assertThat(node.getIlluminance(), empty());
    }

    @Test
    public void correctlySkipMissingTemperature() {
        byte[] parameters = {
            0x07, 0x4E, 0x0, 0x50, b(0xC3), 0x0, // 500 Lux
            0x05, 0x6F, 0x2A, 0x64, 0x19, // 65% humidity
            0x09, 0x6D, 0x2A, 0x50, 0x69, 0x0F, 0x0, // 101 kPa
            0x0D, b(0xAA), b(0xAA), 0x0, 0x40, 0x01, 0x40, b(0xFF), 0x7F //16384;16385;32767
        };
        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);

        assertThat(node.getTemperature(), empty());
    }

    @Test
    public void correctlySkipMissingHumidity() {
        byte[] parameters = {
            0x07, 0x4E, 0x0, 0x50, b(0xC3), 0x0, // 500 Lux
            0x03, 0x4F, 0x0, 0x32, // 25 C
            0x09, 0x6D, 0x2A, 0x50, 0x69, 0x0F, 0x0, // 101 kPa
            0x0D, b(0xAA), b(0xAA), 0x0, 0x40, 0x01, 0x40, b(0xFF), 0x7F //16384;16385;32767
        };
        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);

        assertThat(node.getHumidity(), empty());
    }

    @Test
    public void correctlySkipMissingPressure() {
        byte[] parameters = {
            0x07, 0x4E, 0x0, 0x50, b(0xC3), 0x0, // 500 Lux
            0x03, 0x4F, 0x0, 0x32, // 25 C
            0x05, 0x6F, 0x2A, 0x64, 0x19, // 65% humidity
            0x0D, b(0xAA), b(0xAA), 0x0, 0x40, 0x01, 0x40, b(0xFF), 0x7F //16384;16385;32767
        };
        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);

        assertThat(node.getPressure(), empty());
    }

    @Test
    public void correctlySkipMissingOrientation() {
        byte[] parameters = {
            0x07, 0x4E, 0x0, 0x50, b(0xC3), 0x0, // 500 Lux
            0x03, 0x4F, 0x0, 0x32, // 25 C
            0x05, 0x6F, 0x2A, 0x64, 0x19, // 65% humidity
            0x09, 0x6D, 0x2A, 0x50, 0x69, 0x0F, 0x0 // 101 kPa
        };
        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);

        assertThat(node.getX(), is(0));
        assertThat(node.getY(), is(0));
        assertThat(node.getZ(), is(0));
    }

    @Test
    public void correctlyKeepSensorData() {
        byte[] parameters = {
            0x07, 0x4E, 0x0, 0x50, b(0xC3), 0x0, // 500 Lux
            0x03, 0x4F, 0x0, 0x32, // 25 C
            0x05, 0x6F, 0x2A, 0x64, 0x19, // 65% humidity
            0x09, 0x6D, 0x2A, 0x50, 0x69, 0x0F, 0x0, // 101 kPa
            0x0D, b(0xAA), b(0xAA), 0x0, 0x40, 0x01, 0x40, b(0xFF), 0x7F //16384;16385;32767
        };

        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);
        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);

        assertThat(node.getIlluminance().size(), is(2));
    }

    @Ignore("Timing-dependant test, will fail sometimes")
    @Test
    public void correctlyClearOnlyOlderSensorData() {
        byte[] parameters = {
            0x07, 0x4E, 0x0, 0x50, b(0xC3), 0x0, // 500 Lux
            0x03, 0x4F, 0x0, 0x32, // 25 C
            0x05, 0x6F, 0x2A, 0x64, 0x19, // 65% humidity
            0x09, 0x6D, 0x2A, 0x50, 0x69, 0x0F, 0x0, // 101 kPa
            0x0D, b(0xAA), b(0xAA), 0x0, 0x40, 0x01, 0x40, b(0xFF), 0x7F //16384;16385;32767
        };

        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);
        long now = System.currentTimeMillis();
        node.offerMeshMessage(SENSOR_STATUS_OP, parameters);
        node.clearDataOlderThan(now);

        assertThat(node.getIlluminance().size(), is(1));
    }

    @Test
    public void dontCastExceptionOnEmptyParameter() {
        node.offerMeshMessage(SENSOR_STATUS_OP, new byte[0]);
    }

    @Test
    public void handleGenericOnOff() {
        byte[] parameters = {0x01};
        node.offerMeshMessage(ApplicationMessageOpCodes.GENERIC_ON_OFF_STATUS, parameters);

        assertThat(node.getLedLuminance(), is(0xFFFF));
        assertThat(node.getLedHue(), is(0xFFFF));
        assertThat(node.getLedSaturation(), is(0xFFFF));
    }

    @Test
    public void handleGenericPowerOnOff() {
        byte[] parameters = {0x01};
        node.offerMeshMessage(0x8212, parameters);

        assertThat(node.getLedLuminance(), is(0xFFFF));
        assertThat(node.getLedHue(), is(0xFFFF));
        assertThat(node.getLedSaturation(), is(0xFFFF));
    }

    @Test
    public void handleHslStatus() {
        byte[] parameters = {
            b(0xFF), 0x7F,
            0x00, 0x00,
            b(0xFF), b(0xFF)
        };
        node.offerMeshMessage(ApplicationMessageOpCodes.LIGHT_HSL_STATUS, parameters);

        assertThat(node.getLedLuminance(), is(0x7FFF));
        assertThat(node.getLedHue(), is(0));
        assertThat(node.getLedSaturation(), is(0xFFFF));
    }

    @Test
    public void presentGenericNameIfNoAlias() {
        assertThat(node.getName(), equalTo("Node: 1"));
    }

    @Test
    public void presentAliasIfPresent() {
        String alias = "Generic Alias";
        node.setAlias(alias);
        assertThat(node.getName(), equalTo(alias));
    }

    private static byte b(int i) {
        return (byte) i;
    }
}
