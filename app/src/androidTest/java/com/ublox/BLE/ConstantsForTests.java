package com.ublox.BLE;

public class ConstantsForTests {

    /*Test Environment Constants*/

    public static final String DEVICE_ADDRESS = "E8:47:5E:19:6A:06";
    public static final long TIMEOUT = 10000;

    /*UI String Constants*/

    public static final String TAB_SERVICES = "SERVICES";
    public static final String TAB_CHAT = "CHAT";
    public static final String TAB_TEST = "TEST";
    public static final String TOAST_MTU_EMPTY = "You must enter a number";
    public static final String TOAST_PACKET_EMPTY = "The packet length number too big or not valid!";
    public static final String TOAST_CHAT_DISCONNECTED = "You need to be connected to a device in order to do this";

    public static final String MODE_PACKET = "packet"; //This radio button has no ID
    public static final String MAP_KEY_NAME = "NAME";

    /*Test Values and Expected Results*/

    public static final String WEBSITE = "http://u-blox.com";
    public static final String MESSAGE = "Hello World!";

    public static final String MTU_TOO_SMALL = "0";
    public static final String MTU_MIN = "23";
    public static final String MTU_MAX = "247";
    public static final String MTU_TOO_LARGE = "300";

    public static final String RX_CLEAR = "0.00 kbps";

    public static final String ZERO_BYTES = "0 B";
    public static final String TWENTY_BYTES = "20 B";

}
