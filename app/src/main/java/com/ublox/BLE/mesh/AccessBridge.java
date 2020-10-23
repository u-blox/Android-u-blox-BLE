package com.ublox.BLE.mesh;

import no.nordicsemi.android.meshprovisioner.transport.AccessMessage;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class AccessBridge implements MessageBridge {
    private AccessMessage message;

    public AccessBridge(AccessMessage message) {
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

        byte[] accessPdu = message.getAccessPdu();
        int opCodeSize = (accessPdu[0] &0xFF) >> 6;
        if (opCodeSize != 3) return message.getOpCode();

        return
            (MeshParserUtils.unsignedByteToInt(accessPdu[0]) << 16) |
            (MeshParserUtils.unsignedByteToInt(accessPdu[1]) << 8) |
            MeshParserUtils.unsignedByteToInt(accessPdu[2]);
    }

    @Override
    public int getTimeToLive() {
        return message.getTtl();
    }

    public byte[] getParameters() {
        return message.getParameters();
    }
}
