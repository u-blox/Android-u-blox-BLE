package com.ublox.BLE.mesh;

import android.content.Context;

import com.ublox.BLE.datapump.DataStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import no.nordicsemi.android.meshprovisioner.AllocatedGroupRange;
import no.nordicsemi.android.meshprovisioner.AllocatedSceneRange;
import no.nordicsemi.android.meshprovisioner.AllocatedUnicastRange;
import no.nordicsemi.android.meshprovisioner.ApplicationKey;
import no.nordicsemi.android.meshprovisioner.MeshManagerApi;
import no.nordicsemi.android.meshprovisioner.MeshManagerCallbacks;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.MeshStatusCallbacks;
import no.nordicsemi.android.meshprovisioner.NetworkKey;
import no.nordicsemi.android.meshprovisioner.Provisioner;
import no.nordicsemi.android.meshprovisioner.SecureNetworkBeacon;
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.transport.AccessMessage;
import no.nordicsemi.android.meshprovisioner.transport.ControlMessage;
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageUnacked;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import no.nordicsemi.android.meshprovisioner.utils.SecureUtils;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class MeshProxyProtocol implements MeshProtocol, DataStream.Delegate, MeshManagerCallbacks, MeshStatusCallbacks {
    private MeshBearer bearer;
    private MeshManagerApi meshApi;
    private final NetworkKey networkKey;
    private final ApplicationKey applicationKey;
    private int unicastAddress;
    private State state;
    private Delegate delegate;


    private MeshMessage messageInFlight;
    private int destination;
    private Queue<byte[]> packetQueue;

    public MeshProxyProtocol(MeshBearer bearer, byte[] networkKey, byte[] applicationKey, int address, Context context) {
        this.bearer = bearer;
        bearer.setDelegate(this);
        meshApi = new MeshManagerApi(context);
        meshApi.setMeshManagerCallbacks(this);
        meshApi.setMeshStatusCallbacks(this);
        this.networkKey = new NetworkKey(0, networkKey);
        this.applicationKey = new ApplicationKey(0, applicationKey);
        unicastAddress = address;
        state = State.CLOSED;
        packetQueue = new LinkedList<>();
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void open() {
        if (bearer.getState() == DataStream.State.OPENED) return;
        meshApi.loadMeshNetwork();
    }

    @Override
    public void close() {
        bearer.close();
    }

    @Override
    public void write(int dest, MeshMessage message) {
        if (state != State.OPENED || messageInFlight != null) return;
        messageInFlight = message;
        destination = dest;
        meshApi.createMeshPdu(dest, message);
    }

    private void setState(State newState) {
        if (newState == state || (state == State.ERROR && newState == State.CLOSED)) return;
        state = newState;
        if (delegate != null) delegate.meshProtocolChangedState(this);
    }

    @Override
    public void onNetworkLoaded(MeshNetwork meshNetwork) {
        setupMesh(meshNetwork);
        bearer.open();
    }

    private void setupMesh(MeshNetwork meshNetwork) {
        Provisioner oldSelf = clearOldNetwork(meshNetwork);
        setKeys(meshNetwork);
        setSelfNode(meshNetwork, oldSelf);
    }

    private Provisioner clearOldNetwork(MeshNetwork network) {
        Provisioner old = null;

        ArrayList<Provisioner> provisioners = new ArrayList<>(network.getProvisioners());
        for (Provisioner p : provisioners) {
            if (p.isLastSelected()) {
                old = p;
            }
            network.removeProvisioner(p);
        }

        return old;
    }

    private void setSelfNode(MeshNetwork meshNetwork, Provisioner old) {
        Provisioner p;
        if (old != null && old.getProvisionerAddress() == unicastAddress) {
            p = old;
        } else {
            p = meshNetwork.createProvisioner(
                String.valueOf(unicastAddress),
                new AllocatedUnicastRange(unicastAddress, unicastAddress),
                new AllocatedGroupRange(unicastAddress+0xC000,unicastAddress+0xC000),
                new AllocatedSceneRange(unicastAddress,unicastAddress));
            p.assignProvisionerAddress(unicastAddress);
            p.setSequenceNumber(old != null ? old.getSequenceNumber() : 0);
        }

        meshNetwork.addProvisioner(p);
        meshNetwork.selectProvisioner(p);
    }

    private void setKeys(MeshNetwork meshNetwork) {
        if (networkKey.equals(meshNetwork.getPrimaryNetworkKey()) &&
            meshNetwork.getAppKeys().contains(applicationKey)) return;

        // Seems silly to copy the lists, but since getKeys returns unmodifiable list views
        // and you can't remove items while iterating and there's not clear method...
        ArrayList<ApplicationKey> appKeys = new ArrayList<>(meshNetwork.getAppKeys());
        for (ApplicationKey k: appKeys) {
            meshNetwork.removeAppKey(k);
        }
        ArrayList<NetworkKey> netKeys = new ArrayList<>(meshNetwork.getNetKeys());
        for (NetworkKey k: netKeys) {
            meshNetwork.removeNetKey(k);
        }
        meshNetwork.addNetKey(networkKey);
        meshNetwork.addAppKey(applicationKey);
    }

    @Override
    public void dataStreamChangedState(DataStream stream) {
        switch (bearer.getState()) {
            case OPENED: setState(State.NEGOTIATING); break;
            case CLOSED: setState(State.CLOSED); break;
            case ERROR: setState(State.ERROR); break;
        }
    }

    @Override
    public void dataStreamWrote(DataStream stream, byte[] data) {
        if (packetQueue.isEmpty()) {
            MeshMessage message = messageInFlight;
            int dest = destination;
            messageInFlight = null;
            if (delegate != null) delegate.meshProtocolSentMessage(this, dest, message);
        } else {
            bearer.write(packetQueue.poll());
        }
    }

    @Override
    public void dataStreamRead(DataStream stream, byte[] data) {
        if (state != State.OPENED) {
            final byte[] receivedBeaconData = new byte[data.length - 1];
            System.arraycopy(data, 1, receivedBeaconData, 0, receivedBeaconData.length);
            SecureNetworkBeacon beacon = new SecureNetworkBeacon(receivedBeaconData);
            meshApi.getMeshNetwork().setIvIndex(beacon.getIvIndex());
            setState(State.OPENED);
        } else {
            int source = preExtractSource(data);
            addNodeForSource(source);
            meshApi.handleNotifications(getMtu(), data);
        }
    }

    @Override
    public void onNetworkUpdated(MeshNetwork meshNetwork) {}

    @Override
    public void onNetworkLoadFailed(String error) {
        setState(State.ERROR);
        close();
    }

    @Override
    public void onNetworkImported(MeshNetwork meshNetwork) {}

    @Override
    public void onNetworkImportFailed(String error) {}

    @Override
    public void sendProvisioningPdu(UnprovisionedMeshNode meshNode, byte[] pdu) {}

    @Override
    public void onMeshPduCreated(byte[] pdu) {
        packetQueue.offer(pdu);
    }

    @Override
    public int getMtu() {
        return bearer.maxTransmissionUnit();
    }

    @Override
    public void onTransactionFailed(int dst, boolean hasIncompleteTimerExpired) {

    }

    @Override
    public void onUnknownPduReceived(int src, byte[] accessPayload) {}

    @Override
    public void onBlockAcknowledgementProcessed(int dst, ControlMessage message) {

    }

    @Override
    public void onBlockAcknowledgementReceived(int src, ControlMessage message) {

    }

    @Override
    public void onMeshMessageProcessed(int dst, MeshMessage meshMessage) {
        bearer.write(packetQueue.poll());
    }

    @Override
    public void onMeshMessageReceived(int src, MeshMessage meshMessage) {
        // Some sillyness due to Message being package private
        MessageBridge bridge = (meshMessage.getMessage() instanceof AccessMessage)
            ? new AccessBridge((AccessMessage) meshMessage.getMessage())
            : new ControlBridge((ControlMessage) meshMessage.getMessage());

        if (delegate != null) delegate.meshProtocolReceivedMessage(
            this,
            bridge.getSource(),
            bridge.getDestination(),
            bridge.getOpCode(),
            bridge.getTimeToLive(),
            bridge.getParameters()
        );
    }

    @Override
    public void onMessageDecryptionFailed(String meshLayer, String errorMessage) {

    }

    private int preExtractSource(byte[] pdu) {
        SecureUtils.K2Output k2Output = SecureUtils.calculateK2(networkKey.getKey(), SecureUtils.K2_MASTER_INPUT);
        byte[] privacyKey = k2Output.getPrivacyKey();
        byte[] ivIndex = MeshParserUtils.intToBytes(meshApi.getMeshNetwork().getIvIndex());

        byte[] obfuscatedHeader = Arrays.copyOfRange(pdu, 2, 8);
        byte[] temp = new byte[16];

        System.arraycopy(ivIndex, 0, temp, 5, 4);
        System.arraycopy(pdu, 8, temp, 9, 7);

        byte [] pecb = SecureUtils.encryptWithAES(temp, privacyKey);

        byte[] header = new byte[6];
        for (int i = 0; i < 6; i++) {
            header[i] = (byte) (obfuscatedHeader[i] ^ pecb[i]);
        }

        return MeshParserUtils.unsignedBytesToInt(header[5], header[4]);
    }

    private void addNodeForSource(int source) {
        MeshNetwork network = meshApi.getMeshNetwork();

        ProvisionedMeshNode node = network.getNode(source);
        if (node != null) return;

        int group = 0xC000 + source;
        Provisioner p = network.createProvisioner(
            String.valueOf(source),
            new AllocatedUnicastRange(source, source),
            new AllocatedGroupRange(group, group),
            new AllocatedSceneRange(source, source)
        );
        p.assignProvisionerAddress(source);

        network.addProvisioner(p);

        // """subscribe""" to sensor data
        VendorModelMessageUnacked getSensorData = new VendorModelMessageUnacked(applicationKey, 0x1100, 0x0059, 0x52, new byte[0]);
        //SensorDataStatus getSensorData = new SensorDataStatus(applicationKey);
        meshApi.createMeshPdu(source, getSensorData);
    }

    public interface MeshBearer extends DataStream {
        int maxTransmissionUnit();
    }
}
