package com.ublox.BLE.mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class C209Network {
    private List<C209Node> nodes;

    public C209Network() {
        nodes = new ArrayList<>();
    }

    public List<C209Node> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void offerMeshMessage(int source, int destination, int opCode, byte[] parameters) {
        getNodeForSource(source).offerMeshMessage(opCode, parameters);
    }

    private C209Node getNodeForSource(int source) {
        for (C209Node node: nodes) {
            if (node.address() == source) return node;
        }

        C209Node node = new C209Node(source);
        nodes.add(node);
        return node;
    }
}
