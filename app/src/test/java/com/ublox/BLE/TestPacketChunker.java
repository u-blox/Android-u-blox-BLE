package com.ublox.BLE;

import com.ublox.BLE.utils.PacketChunker;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestPacketChunker {

    @Test
    public void exactChunk() {
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7};

        ArrayList<byte[]> expected = new ArrayList<>();
        expected.add(new byte[]{0, 1, 2, 3});
        expected.add(new byte[]{4, 5, 6, 7});

        assertEqual(PacketChunker.chunkify(data, 4), expected);
    }

    @Test
    public void chunkWithRest() {
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        ArrayList<byte[]> expected = new ArrayList<>();
        expected.add(new byte[]{0, 1, 2, 3});
        expected.add(new byte[]{4, 5, 6, 7});
        expected.add(new byte[]{8, 9});

        assertEqual(PacketChunker.chunkify(data, 4), expected);
    }

    @Test
    public void emptyChunk() {
        byte[] data = new byte[0];

        assertEqual(PacketChunker.chunkify(data, 4), new ArrayList<>());
    }

    /*
    todo: Could be a matcher, but don't know how long PacketChunker will survive
    Hopefully not long, probably forever though.
    */
    private static void assertEqual(List<byte[]> actual, List<byte[]> expected) {
        if (actual.size() != expected.size()) {
            throw new AssertionError(String.format("Different amount of chunks, expected %d actual %d", expected.size(), actual.size()));
        }
        for (int i = 0; i < actual.size(); i++) {
            byte[] act = actual.get(i);
            byte[] exp = expected.get(i);
            if (act.length != exp.length) {
                throw new AssertionError(String.format("Chunks #%d differed in length, expected %d actual %d", i, exp.length, act.length));
            }
            for (int j = 0; j < act.length; j++) {
                if (act[j] != exp[j]) {
                    throw new AssertionError(String.format("Chunks #%d differed at index %d, expected %d actual %d", i, j, exp[j], act[j]));
                }
            }
        }
    }
}
