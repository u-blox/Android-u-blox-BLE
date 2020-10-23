package com.ublox.BLE.mesh;

import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public interface MeshProtocol {

    State getState();
    void setDelegate(Delegate delegate);
    void open();
    void close();
    void write(int destination, MeshMessage message);

    enum State {CLOSED, NEGOTIATING, OPENED, ERROR}

    interface Delegate {
        void meshProtocolChangedState(MeshProtocol protocol);
        void meshProtocolSentMessage(MeshProtocol protocol, int destination, MeshMessage message);
        void meshProtocolReceivedMessage(MeshProtocol protocol, int source, int destination, int opCode, int ttl, byte[] data);
    }
}
