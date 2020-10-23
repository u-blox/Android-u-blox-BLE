package com.ublox.BLE.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import com.ublox.BLE.R;
import com.ublox.BLE.bluetooth.BluetoothPeripheral;
import com.ublox.BLE.fragments.MeshListFragment;
import com.ublox.BLE.fragments.MeshNodeDetailsFragment;
import com.ublox.BLE.fragments.MeshProxyConnectionFragment;
import com.ublox.BLE.mesh.C209Network;
import com.ublox.BLE.mesh.C209Node;
import com.ublox.BLE.mesh.MeshProtocol;
import com.ublox.BLE.mesh.MeshProxyBearer;
import com.ublox.BLE.mesh.MeshProxyProtocol;

import java.util.ArrayList;

import no.nordicsemi.android.meshprovisioner.opcodes.ProxyConfigMessageOpCodes;
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;
import no.nordicsemi.android.meshprovisioner.transport.ProxyConfigAddAddressToFilter;
import no.nordicsemi.android.meshprovisioner.transport.ProxyConfigSetFilterType;
import no.nordicsemi.android.meshprovisioner.utils.AddressArray;
import no.nordicsemi.android.meshprovisioner.utils.ProxyFilterType;

import static com.ublox.BLE.activities.DevicesActivity.EXTRA_APP_KEY;
import static com.ublox.BLE.activities.DevicesActivity.EXTRA_DEVICE;
import static com.ublox.BLE.activities.DevicesActivity.EXTRA_NET_KEY;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class MeshActivity extends Activity implements MeshProtocol.Delegate, MeshListFragment.MeshListListener, MeshProxyConnectionFragment.MeshProxyConnectionListener {

    public static final int ADDRESS = 0x0278;
    private BluetoothPeripheral peripheral;
    private byte[] appKey;
    private byte[] netKey;
    private MeshProxyProtocol meshProtocol;
    private C209Network network;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getActionBar();
        bar.setTitle("");
        bar.setIcon(R.drawable.logo);
        bar.setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_mesh);
        network = new C209Network();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        peripheral = intent.getParcelableExtra(EXTRA_DEVICE);
        netKey = intent.getByteArrayExtra(EXTRA_NET_KEY);
        appKey = intent.getByteArrayExtra(EXTRA_APP_KEY);

        MeshProxyBearer bearer = new MeshProxyBearer(peripheral);
        meshProtocol = new MeshProxyProtocol(bearer, netKey, appKey, ADDRESS, this);
        meshProtocol.setDelegate(this);

        getActionBar().setTitle(peripheral.name());
        //fragment = MeshNodeDetailsFragment.newInstance();

        //setFragment(MeshListFragment.newInstance(), true);
        setFragment(MeshProxyConnectionFragment.newInstance(), false);

        meshProtocol.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        meshProtocol.close();
    }

    @Override
    public void meshProtocolChangedState(MeshProtocol messager) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment);
        if (fragment instanceof MeshProxyConnectionFragment) {
            runOnUiThread(()->{((MeshProxyConnectionFragment) fragment).setStatus("Status Changed");});
        }

        if (this.meshProtocol.getState() != MeshProtocol.State.OPENED) return;

        setFragment(MeshListFragment.newInstance(), true);
        ProxyConfigSetFilterType setFilterType = new ProxyConfigSetFilterType(new ProxyFilterType(ProxyFilterType.WHITE_LIST_FILTER));
        meshProtocol.write(0, setFilterType);
    }

    @Override
    public void meshProtocolSentMessage(MeshProtocol messager, int destination, MeshMessage message) {

    }

    @Override
    public void meshProtocolReceivedMessage(MeshProtocol messager, int src, int dst, int opCode, int ttl, byte[] data) {
        if (opCode == ProxyConfigMessageOpCodes.FILTER_STATUS) {
            int length = ((data[1] & 0xFF) << 8) + (data[2] & 0xFF);
            if (length == 0) {
                ArrayList<AddressArray> addresses = new ArrayList<>();
                addresses.add(new AddressArray((byte) 0xC1, (byte) 0x11));
                addresses.add(new AddressArray((byte) (ADDRESS >> 8), (byte) (ADDRESS & 0xFF)));
                ProxyConfigAddAddressToFilter addAddresses = new ProxyConfigAddAddressToFilter(addresses);
                meshProtocol.write(0, addAddresses);
            }
        }

        network.offerMeshMessage(src, dst, opCode, data);

        runOnUiThread(this::update);
    }

    @Override
    public void connect() {
        meshProtocol.open();
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void nodeSelected(C209Node node) {
        MeshNodeDetailsFragment fragment = MeshNodeDetailsFragment.newInstance();
        setFragment(fragment, true);
        fragment.setNode(node);
    }

    private void setFragment(Fragment fragment, boolean pushOnStack) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment, fragment);
        if (pushOnStack) transaction.addToBackStack(null);
        transaction.commit();
    }

    private void update() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment);

        if (fragment instanceof MeshListFragment) {
            ((MeshListFragment) fragment).setList(network.nodes());
        }

        if (fragment instanceof MeshNodeDetailsFragment) {
            ((MeshNodeDetailsFragment) fragment).update();
        }
    }

}
