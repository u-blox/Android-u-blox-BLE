package com.ublox.BLE.utils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.copyOfRange;

public class PacketChunker {
    public static List<byte[]> chunkify(byte[] message, int size) {
         ArrayList<byte[]> chunks = new ArrayList<>();
         int from = 0;
         for (int to = size; to < message.length; from += size, to += size) {
             chunks.add(copyOfRange(message, from, to));
         }
         if (from < message.length) {
             chunks.add(copyOfRange(message, from, message.length));
         }
         return chunks;
     }
}
