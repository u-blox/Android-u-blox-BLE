package com.ublox.BLE.utils;

import java.util.LinkedList;

public class BLEQueue {

    public interface QueueAction {
        boolean execute();
    }

    private LinkedList<QueueAction> queueItems = new LinkedList<>();

    public synchronized void addAction(QueueAction action) {
        queueItems.addLast(action);
    }

    public synchronized QueueAction getNextItem() {
        return queueItems.pollFirst();
    }

    public int hasItems() {
        return queueItems.size();
    }
}
