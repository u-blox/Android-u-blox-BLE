package com.ublox.BLE.mesh;

import no.nordicsemi.android.meshprovisioner.transport.ControlMessage;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class ControlBridge implements MessageBridge {
    private ControlMessage message;

    public ControlBridge(ControlMessage message) {
        this.message = message;
    }

    public int getSource() {
        return message.getSrc();
    }

    @Override
    public int getDestination() {
        return message.getDst();
    }

    public int getOpCode() {
        return message.getOpCode();
    }

    @Override
    public int getTimeToLive() {
        return message.getTtl();
    }

    public byte[] getParameters() {
        return message.getParameters();
    }
}
