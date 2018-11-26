package com.ublox.BLE.datapump;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.ublox.BLE.fragments.TestFragment;
import com.ublox.BLE.services.BluetoothLeService;
import com.ublox.BLE.utils.BLEQueue;

import java.util.Locale;

import static com.ublox.BLE.utils.GattAttributes.UUID_CHARACTERISTIC_CREDITS;
import static com.ublox.BLE.utils.GattAttributes.UUID_CHARACTERISTIC_FIFO;

/**
 * Class encapsulating data pump test feature based on bluetoothLeService communication
 */
public class DataPumpManager {

    public static final int MAX_NUMBER_OF_CREDITS = 0x20;
    private static final String TAG = DataPumpManager.class.getSimpleName();

    private BluetoothGattCharacteristic characteristicFifo;
    private BluetoothGattCharacteristic characteristicCredits;
    private boolean isTestRunning;
    private boolean continuousMode;
    private boolean bitErrorActive;
    private boolean withCredits;
    private float txCounter;
    private float rxCounter;
    private long txStartTime;
    private long rxStartTime;
    private int packetSize = 20; // default value, to be discussed when changing test fragment flow
    private int mtuSize = 23;
    private int sizeOfRemainingPacket;
    private int sizeOfDividedPacket;
    private int rxCredits;
    private int txCredits;
    private long timeOfLastUIUpdate;

    private IDataPumpListener dataPumpListener;
    private BluetoothLeService bluetoothLeService;
    private boolean noOngoingCredits;

    /**
     * Interface defining data pump test events
     */
    public interface IDataPumpListener {
        /**
         * Indicates successful update of MTU size received from BLE service
         * @param size Updated MTU size
         */
        void updateMTUSize(int size);

        /**
         * Indicates TxCounter update in String format for UI
         * @param value String representation of Tx packets count\
         * @param average String representation of average throughput value
         */
        void updateTxCounter(String value, String average);

        /**
         * Indicates RxCounter and average throughput values in String format for UI
         * @param rxValue String representation of Rx packets count
         * @param average String representation of average throughput value
         */
        void updateRxCounter(String rxValue, String average);

        /**
         * Indicates last packet has been sent
         */
        void onLastPacketSent();
    }

    //Constructors
    /**
     * Constructor
     * @param dataPumpListener Listener to receive data pump events
     */
    public DataPumpManager(IDataPumpListener dataPumpListener) {
        this.dataPumpListener = dataPumpListener;
        noOngoingCredits = true;
    }


    public boolean isTestRunning() {
        return isTestRunning;
    }

    //Setters and getters
    public void setBluetoothLeService(BluetoothLeService bluetoothLeService) {
        this.bluetoothLeService = bluetoothLeService;
    }

    public void setCharacteristicFifo(BluetoothGattCharacteristic characteristicFifo) {

        if (bluetoothLeService != null) {
            if (this.characteristicFifo == null) {
                this.characteristicFifo = characteristicFifo;
                bluetoothLeService.setCharacteristicNotification(characteristicFifo, true);
            } else if (!this.characteristicFifo.equals(characteristicFifo)) {
                this.characteristicFifo = characteristicFifo;
                bluetoothLeService.setCharacteristicNotification(characteristicFifo, true);
            }
        } else {
            Log.w(TAG, "BLE service not initialized");
        }
    }

    public void setCharacteristicCredits(BluetoothGattCharacteristic characteristicCredits) {
        if (characteristicCredits != null) {
            this.characteristicCredits = characteristicCredits;
        } else if (!this.characteristicCredits.equals(characteristicCredits)) {
            this.characteristicCredits = characteristicCredits;
        }
    }

    public void setBitErrorActive(boolean bitErrorActive) {
        this.bitErrorActive = bitErrorActive;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public void setWithCredits(boolean withCredits) {
        this.withCredits = withCredits;
    }

    //Public methods

    /**
     * Use bluetoothLeService to set data for given BluetoothCharacteristic
     * @param type Characteristic type for data pump (can be FIFO or CREDITS)
     * @param value Data to be set
     */
    public void writeCharacteristic(TestFragment.CharacteristicType type, byte[] value) {
        if (bluetoothLeService != null) {
            if (type == TestFragment.CharacteristicType.CREDITS && characteristicCredits != null) {
                bluetoothLeService.writeCharacteristic(characteristicCredits, value);
            } else if (type == TestFragment.CharacteristicType.FIFO && characteristicFifo != null) {
                bluetoothLeService.writeCharacteristic(characteristicFifo, value);
            }
        } else {
            Log.w(TAG, "BLE service not initialized");
        }
    }

    /**
     * Use bluetoothLeService to toggle notifications for given BluetoothCharacteristic
     * @param type Characteristic type for data pump (can be FIFO or CREDITS)
     * @param enabled Flag to toggle the value
     */
    public void setCharacteristicNotification(TestFragment.CharacteristicType type,
                                              boolean enabled) {
        if (bluetoothLeService != null) {
            if (type == TestFragment.CharacteristicType.CREDITS && characteristicCredits != null) {
                bluetoothLeService.setCharacteristicNotification(characteristicCredits, enabled);
            } else if (type == TestFragment.CharacteristicType.FIFO && characteristicFifo != null) {
                bluetoothLeService.setCharacteristicNotification(characteristicFifo, enabled);
            }
        } else {
            Log.w(TAG, "BLE service not initialized");
        }
    }

    /**
     * Use bluetoothLeService to set priority of BLE connection
     * @param connectionParameter Connection priority defined in BluetoothGatt class
     */
    public void connectionPrioRequest(int connectionParameter) {
        if (bluetoothLeService != null) {
            bluetoothLeService.connectionPrioRequest(connectionParameter);
        } else {
            Log.w(TAG, "BLE service not initialized");
        }
    }

    /**
     * Use bluetoothLeService to request MTU size
     * @param size Size of the newly requested MTU
     */
    public void setMtuSize(int size) {
        if (bluetoothLeService != null) {
            bluetoothLeService.mtuRequest(size);
        } else {
            Log.w(TAG, "BLE service not initialized");
        }
    }

    /**
     * Writes data to FIFO characteristic
     */
    public void sendData() {
        if (bluetoothLeService != null && characteristicFifo != null) {
            if (isTestRunning) {
                if (withCredits) {
                    if (txCredits > 0) {
                        bluetoothLeService.writeCharacteristic(characteristicFifo, createPacket());
                    } else {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sendData();
                            }
                        }, 1);
                    }
                } else {
                     bluetoothLeService.writeCharacteristic(characteristicFifo, createPacket());
                }
            }
        } else {
            Log.w(TAG, "BLE service not initialized");
        }
    }

    private byte[] createPacket() {
        if(sizeOfRemainingPacket == 0) {
            sizeOfRemainingPacket = packetSize;
        }

        int from = packetSize - sizeOfRemainingPacket;
        sizeOfDividedPacket = mtuSize - 3 > sizeOfRemainingPacket ?
                sizeOfRemainingPacket : mtuSize - 3;
        sizeOfRemainingPacket -= sizeOfDividedPacket;

        if(sizeOfRemainingPacket == 0 && !continuousMode) {
            timeOfLastUIUpdate = 0; // update the UI after the last package sent
        }
        return fillPacketWithBytes(from);
    }

    private byte[] fillPacketWithBytes(int from) {
        int data = from;
        byte[] packet = new byte[sizeOfDividedPacket];
        for (int i = 0; i < sizeOfDividedPacket; i++, data++) {
            packet[i] = (bitErrorActive && i == 5) ? (byte) 0 : (byte) data;
        }
        return packet;
    }

    /**
     * Set properties before starting data pump test
     */
    public void startDataPump() {
        txCounter = 0;
        isTestRunning = true;
        txStartTime = SystemClock.elapsedRealtime();
        sizeOfRemainingPacket = 0;
        sendData();
    }

    /**
     * Set flag to indicate data pump test stop
     */
    public void stopDataPump() {
        isTestRunning = false;
        timeOfLastUIUpdate = 0;
    }

    /**
     * Update TxCredits value
     * @param credits Credits data to be added
     */
    public void updateTxCredits(byte[] credits) {
        txCredits += credits[0];
    }

    /**
     * Update RxCredits value
     * @param credits Credits data to be added
     */
    public void updateRxCredits(byte[] credits) {
        rxCredits += credits[0];
        noOngoingCredits = true;
    }

    /**
     * Decreas number of TxCredits
     */
    public void decreaseTxCredits() {
        if (withCredits) {
            txCredits--;
        }
    }

    /**
     * Reset credits values
     */
    public void resetCredits() {
        txCredits = 0;
        rxCredits = 0;
    }

    /**
     * Reset data pump state
     */
    public void resetDataPump() {
        rxCounter = 0;
        txCounter = 0;
        txStartTime = SystemClock.elapsedRealtime();
        rxStartTime = SystemClock.elapsedRealtime();
    }

    public void reset() {
        resetDataPump();
        packetSize = 20;
        mtuSize = 23;
        bitErrorActive = false;
    }

    /**
     * Broadcast MTU size change to listener
     * @param size Updated MTU size
     */
    public void updateMtuSizeChanged(int size) {
        dataPumpListener.updateMTUSize(size);
        mtuSize = size;
    }

    /**
     * Broadcast TxCounter value change to listener
     */
    public void updateTxCounter() {
        txCounter += sizeOfDividedPacket;
        if(SystemClock.elapsedRealtime() - timeOfLastUIUpdate > 100) {
            String counter;
            if (txCounter > 1024*1024) {
                counter = String.format(Locale.US, "%.2f", (txCounter / (1024 * 1024))) + " MB";
            } else if (txCounter > 1024) {
                counter = String.format(Locale.US, "%.2f", (txCounter / 1024)) + " KB";
            } else {
                counter = String.format(Locale.US, "%.0f", txCounter) + " B";
            }
            long elapsedTime = SystemClock.elapsedRealtime() - txStartTime;
            float avgValue = (8 * txCounter) / elapsedTime;
            String avgText = (String.format(Locale.US, "%.2f", avgValue) + " kbps");
            dataPumpListener.updateTxCounter(counter, avgText);
            timeOfLastUIUpdate = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Broadcast RxCounter and average throughput values change to listener
     * @param data
     */
    public void updateRxCounter(byte[] data) {
        if (bluetoothLeService != null) {
            rxCounter += data.length;
            String counter;
            if (rxCounter > 1024 * 1024) {
                counter = String.format(Locale.US, "%.2f", (rxCounter / (1024 * 1024))) + " MB";
            } else if (rxCounter > 1024) {
                counter = String.format(Locale.US, "%.2f", (rxCounter / 1024)) + " KB";
            } else {
                counter = String.format(Locale.US, "%.0f", rxCounter) + " B";
            }

            long elapsedTime = SystemClock.elapsedRealtime() - rxStartTime;
            float avgValue = (8 * rxCounter) / elapsedTime;
            String avgText = (String.format(Locale.US, "%.2f", avgValue) + " kbps");
            dataPumpListener.updateRxCounter(counter, avgText);

            if (withCredits && characteristicCredits != null) {
                rxCredits--;
                if (noOngoingCredits && rxCredits <= (MAX_NUMBER_OF_CREDITS / 2)) {
                    noOngoingCredits = false;
                    byte[] credits = new byte[]{(byte) (MAX_NUMBER_OF_CREDITS - rxCredits)};
                    bluetoothLeService.writeCharacteristic(characteristicCredits, credits);
                }
            }
        } else {
            Log.w(TAG, "BLE service not initialized");
        }
    }

    /**
     * Notify DataPumpManager of Gatt characteristic change
     * @param uuid UUID of changed characteristic
     * @param type Type of changed characteristic
     * @param data Updated characteristic data
     */
    public void notifyGattUpdate(String uuid, int type, byte[] data) {
        if (type == BLEQueue.ITEM_TYPE_WRITE && uuid.equals(UUID_CHARACTERISTIC_FIFO)) {
            decreaseTxCredits();
            updateTxCounter();
            if(sizeOfRemainingPacket == 0 && !continuousMode) {
                dataPumpListener.onLastPacketSent();
            }
            if(continuousMode || sizeOfRemainingPacket != 0) {
                sendData();
            }
        }
        if (type == BLEQueue.ITEM_TYPE_WRITE && uuid.equals(UUID_CHARACTERISTIC_CREDITS)) {
            updateRxCredits(data);
        }
        if (type == BLEQueue.ITEM_TYPE_NOTIFICATION) {
            if (uuid.equals(UUID_CHARACTERISTIC_FIFO)) {
                if(rxStartTime == 0) {
                    rxStartTime = SystemClock.elapsedRealtime();
                }
                updateRxCounter(data);
            } else if (uuid.equals(UUID_CHARACTERISTIC_CREDITS)) {
                updateTxCredits(data);
            }
        }
        
    }

    /**
     * Set or unset DataPumpManager for credits controlled connection
     * @param enabled toggles credits on and off
     */
    public void toggleCreditsConnection(boolean enabled) {
        if (enabled) {
            setCharacteristicNotification(TestFragment.CharacteristicType.FIFO, false);
            resetCredits();
            setCharacteristicNotification(TestFragment.CharacteristicType.CREDITS, true);
            setCharacteristicNotification(TestFragment.CharacteristicType.FIFO, true);
            byte[] credits = new byte[]{(byte)DataPumpManager.MAX_NUMBER_OF_CREDITS};
            writeCharacteristic(TestFragment.CharacteristicType.CREDITS, credits);
            setWithCredits(true);
        } else {
            byte[] credits = new byte[]{(byte)0xff};
            writeCharacteristic(TestFragment.CharacteristicType.CREDITS, credits);
            setCharacteristicNotification(TestFragment.CharacteristicType.FIFO, false);
            setCharacteristicNotification(TestFragment.CharacteristicType.CREDITS,false);
            setCharacteristicNotification(TestFragment.CharacteristicType.FIFO, true);
            setWithCredits(false);
        }
    }

    /**
     * Setter for sending mode
     * @param enabled indicates if in continuous mode (if not it is packet sending)
     */
    public void setContinuousMode(boolean enabled) {
        this.continuousMode = enabled;
    }
}