package com.ublox.BLE;

import com.ublox.BLE.mesh.C209Network;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;

public class TestC209Network {

    @Test
    public void freshNetworkIsEmpty() {
        C209Network network = new C209Network();

        assertThat(network.nodes(), empty());
    }

    @Test
    public void nodeIsCreatedWhenItDoesntExist() {
        C209Network network = new C209Network();
        network.offerMeshMessage(1, 0, 0, new byte[0]);

        assertThat(network.nodes().get(0).address(), is(1));
    }

    @Test
    public void doNotCreateMultiplesOfNode() {
        C209Network network = new C209Network();
        network.offerMeshMessage(1, 0, 0, new byte[0]);
        network.offerMeshMessage(1, 1, 0, new byte[0]);

        assertThat(network.nodes().size(), is(1));
    }
}
