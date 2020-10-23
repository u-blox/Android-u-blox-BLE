package com.ublox.BLE.utils;

import java.util.HashMap;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    private static final String UUID_MASK = "0000%s-0000-1000-8000-00805f9b34fb";


    public static String UUID_SERVICE_GENERIC_ACCESS = String.format(UUID_MASK, "1800");
    public static String UUID_CHARACTERISTIC_DEVICE_NAME = String.format(UUID_MASK, "2a00");
    public static String UUID_CHARACTERISTIC_APPEARANCE = String.format(UUID_MASK, "2a01");
    public static String UUID_CHARACTERISTIC_PERIPHERAL_PRIVACY_FLAG = String.format(UUID_MASK, "2a02");
    public static String UUID_CHARACTERISTIC_RECONNECTION_ADDRESS = String.format(UUID_MASK, "2a03");
    public static String UUID_CHARACTERISTIC_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = String.format(UUID_MASK, "2a04");

    public static String UUID_SERVICE_GENERIC_ATTRIBUTE = String.format(UUID_MASK, "1801");
    public static String UUID_CHARACTERISTIC_SERVICE_CHANGED = String.format(UUID_MASK, "2a05");

    public static String UUID_SERVICE_SERIAL_PORT       = "2456e1b9-26e2-8f83-e744-f34f01e9d701";
    public static String UUID_CHARACTERISTIC_FIFO       = "2456e1b9-26e2-8f83-e744-f34f01e9d703";
    public static String UUID_CHARACTERISTIC_CREDITS    = "2456e1b9-26e2-8f83-e744-f34f01e9d704";


    public static String UUID_SERVICE_IMMEDIATE_ALERT      = String.format(UUID_MASK, "1802");
    public static String UUID_SERVICE_LINK_LOSS            = String.format(UUID_MASK, "1803");
    public static String UUID_SERVICE_TX_POWER             = String.format(UUID_MASK, "1804");
    public static String UUID_SERVICE_CURRENT_TIME         = String.format(UUID_MASK, "1805");
    public static String UUID_SERVICE_REF_TIME_UPDATE      = String.format(UUID_MASK, "1806");
    public static String UUID_SERVICE_NEXT_DST_CHANGE      = String.format(UUID_MASK, "1807");
    public static String UUID_SERVICE_GLUCOSE              = String.format(UUID_MASK, "1808");
    public static String UUID_SERVICE_HEALTH_THERM         = String.format(UUID_MASK, "1809");
    public static String UUID_SERVICE_DEVICE_ID            = String.format(UUID_MASK, "180a");
    public static String UUID_SERVICE_NETWORK_AVAIL        = String.format(UUID_MASK, "180b");
    public static String UUID_SERVICE_HEART_RATE           = String.format(UUID_MASK, "180d");
    public static String UUID_SERVICE_PHONE_ALERT_STATUS   = String.format(UUID_MASK, "180e");
    public static String UUID_SERVICE_BATTERY              = String.format(UUID_MASK, "180f");
    public static String UUID_SERVICE_BLOOD_PRESSURE       = String.format(UUID_MASK, "1810");
    public static String UUID_SERVICE_ALERT_NOTIFICATION   = String.format(UUID_MASK, "1811");
    public static String UUID_SERVICE_HUMAN_INT_DEVICE     = String.format(UUID_MASK, "1812");
    public static String UUID_SERVICE_SCAN_PARAMETERS      = String.format(UUID_MASK, "1813");
    public static String UUID_SERVICE_RUN_SPEED_CADENCE    = String.format(UUID_MASK, "1814");

    public static String UUID_SERVICE_ACC       = String.format(UUID_MASK, "ffa0");
    public static String UUID_SERVICE_TEMP      = String.format(UUID_MASK, "ffe0");
    public static String UUID_SERVICE_LED       = String.format(UUID_MASK, "ffd0");

    public static String UUID_CHARACTERISTIC_BATTERY_LEVEL       = String.format(UUID_MASK, "2a19");
    public static String UUID_CHARACTERISTIC_SYSTEM_ID           = String.format(UUID_MASK, "2a23");
    public static String UUID_CHARACTERISTIC_MODEL_NUMBER        = String.format(UUID_MASK, "2a24");
    public static String UUID_CHARACTERISTIC_SERIAL_NUMBER       = String.format(UUID_MASK, "2a25");
    public static String UUID_CHARACTERISTIC_FIRMWARE_REVISION   = String.format(UUID_MASK, "2a26");
    public static String UUID_CHARACTERISTIC_HARDWARE_REVISION   = String.format(UUID_MASK, "2a27");
    public static String UUID_CHARACTERISTIC_SW_REVISION         = String.format(UUID_MASK, "2a28");
    public static String UUID_CHARACTERISTIC_MANUFACTURE_NAME    = String.format(UUID_MASK, "2a29");
    public static String UUID_CHARACTERISTIC_REG_CERT            = String.format(UUID_MASK, "2a2a");

    public static String UUID_CHARACTERISTIC_ACC_ENABLED         = String.format(UUID_MASK, "ffa1");
    public static String UUID_CHARACTERISTIC_ACC_RANGE           = String.format(UUID_MASK, "ffa2");
    public static String UUID_CHARACTERISTIC_ACC_X               = String.format(UUID_MASK, "ffa3");
    public static String UUID_CHARACTERISTIC_ACC_Y               = String.format(UUID_MASK, "ffa4");
    public static String UUID_CHARACTERISTIC_ACC_Z               = String.format(UUID_MASK, "ffa5");

    public static String UUID_CHARACTERISTIC_TEMP_VALUE          = String.format(UUID_MASK, "ffe1");
    public static String UUID_CHARACTERISTIC_RED_LED             = String.format(UUID_MASK, "ffd1");
    public static String UUID_CHARACTERISTIC_GREEN_LED           = String.format(UUID_MASK, "ffd2");

    public static String CLIENT_CHARACTERISTIC_CONFIG = String.format(UUID_MASK, "2902");

    public static String UUID_SERVICE_MESH_PROXY = String.format(UUID_MASK, "1828");
    public static String UUID_CHARACTERISTIC_MESH_PROXY_DATA_IN = String.format(UUID_MASK, "2add");
    public static String UUID_CHARACTERISTIC_MESH_PROXY_DATA_OUT = String.format(UUID_MASK, "2ade");

    static {
        attributes.put(String.format(UUID_MASK, "180d"), "Heart Rate Service");
        attributes.put(String.format(UUID_MASK, "180a"), "Device Information Service");
        attributes.put(String.format(UUID_MASK, "2a29"), "Manufacturer Name String");

        attributes.put(UUID_SERVICE_TEMP               , "Temperature");
        attributes.put(UUID_CHARACTERISTIC_TEMP_VALUE  , "Temperature");

        attributes.put(UUID_SERVICE_ACC                 , "Accelerometer");
        attributes.put(UUID_CHARACTERISTIC_ACC_ENABLED  , "Enable");
        attributes.put(UUID_CHARACTERISTIC_ACC_RANGE    , "Range");
        attributes.put(UUID_CHARACTERISTIC_ACC_X        , "X Value");
        attributes.put(UUID_CHARACTERISTIC_ACC_Y        , "Y Value");
        attributes.put(UUID_CHARACTERISTIC_ACC_Z        , "Z Value");

        attributes.put(UUID_SERVICE_LED               , "LED");
        attributes.put(UUID_CHARACTERISTIC_GREEN_LED  , "Green LED");
        attributes.put(UUID_CHARACTERISTIC_RED_LED    , "Red LED");



        attributes.put(UUID_SERVICE_IMMEDIATE_ALERT      , "Immediate Alert");
        attributes.put(UUID_SERVICE_LINK_LOSS            , "Link Loss");
        attributes.put(UUID_SERVICE_TX_POWER             , "TX Power");
        attributes.put(UUID_SERVICE_CURRENT_TIME         , "Current Time");
        attributes.put(UUID_SERVICE_REF_TIME_UPDATE      , "Ref Time Update");
        attributes.put(UUID_SERVICE_NEXT_DST_CHANGE      , "Next DST Change");
        attributes.put(UUID_SERVICE_GLUCOSE              , "Glucose");
        attributes.put(UUID_SERVICE_HEALTH_THERM         , "Health Therm");
        attributes.put(UUID_SERVICE_DEVICE_ID            , "Device ID");
        attributes.put(UUID_SERVICE_NETWORK_AVAIL        , "Network Available");
        attributes.put(UUID_SERVICE_HEART_RATE           , "Heart Rate");
        attributes.put(UUID_SERVICE_PHONE_ALERT_STATUS   , "Phone Alert Status");
        attributes.put(UUID_SERVICE_BATTERY              , "Battery");
        attributes.put(UUID_SERVICE_BLOOD_PRESSURE       , "Blood Pressure");
        attributes.put(UUID_SERVICE_ALERT_NOTIFICATION   , "Alert Notification");
        attributes.put(UUID_SERVICE_HUMAN_INT_DEVICE     , "Human Int Device");
        attributes.put(UUID_SERVICE_SCAN_PARAMETERS      , "Scan Parameters");
        attributes.put(UUID_SERVICE_RUN_SPEED_CADENCE    , "Run Speed Cadence");

        attributes.put(UUID_CHARACTERISTIC_BATTERY_LEVEL       , "Battery Level");
        attributes.put(UUID_CHARACTERISTIC_SYSTEM_ID           , "System ID");
        attributes.put(UUID_CHARACTERISTIC_MODEL_NUMBER        , "Model Number");
        attributes.put(UUID_CHARACTERISTIC_SERIAL_NUMBER       , "Serial Number");
        attributes.put(UUID_CHARACTERISTIC_FIRMWARE_REVISION   , "Firmware Revision");
        attributes.put(UUID_CHARACTERISTIC_HARDWARE_REVISION   , "Hardware Revision");
        attributes.put(UUID_CHARACTERISTIC_SW_REVISION         , "Software Revision");
        attributes.put(UUID_CHARACTERISTIC_MANUFACTURE_NAME    , "Manufacture Name");
        attributes.put(UUID_CHARACTERISTIC_REG_CERT            , "Reg Cert");

        attributes.put(UUID_SERVICE_SERIAL_PORT     , "Serial Port");
        attributes.put(UUID_CHARACTERISTIC_FIFO     , "Fifo");
        attributes.put(UUID_CHARACTERISTIC_CREDITS  , "Credits");

        attributes.put(UUID_SERVICE_GENERIC_ACCESS                                     , "Generic Access");
        attributes.put(UUID_CHARACTERISTIC_DEVICE_NAME                                 , "Device Name");
        attributes.put(UUID_CHARACTERISTIC_APPEARANCE                                  , "Appearance");
        attributes.put(UUID_CHARACTERISTIC_PERIPHERAL_PRIVACY_FLAG                     , "Peripheral Privacy Flag");
        attributes.put(UUID_CHARACTERISTIC_RECONNECTION_ADDRESS                        , "Reconnection Address");
        attributes.put(UUID_CHARACTERISTIC_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS  , "Peripheral Preferred Connection Parameters");

        attributes.put(UUID_SERVICE_GENERIC_ATTRIBUTE       , "Generic Attribute");
        attributes.put(UUID_CHARACTERISTIC_SERVICE_CHANGED  , "Service Changed");

        attributes.put(UUID_SERVICE_MESH_PROXY, "Mesh Proxy");
        attributes.put(UUID_CHARACTERISTIC_MESH_PROXY_DATA_IN, "Mesh Proxy Data In");
        attributes.put(UUID_CHARACTERISTIC_MESH_PROXY_DATA_OUT, "Mesh Proxy Data Out");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}