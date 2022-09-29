# Android-u-blox-Bluetooth-Low-Energy
The u-blox Bluetooth low energy Android app allows developers to evaluate the stand-alone Bluetooth low energy modules from u-blox.

https://www.u-blox.com/

[Download APK](https://github.com/u-blox/Android-u-blox-BLE/releases/latest)

# 

These files are all working with Android Studio 4.2.2 and with SDK version 29 (Android 10).

Needed files to get Bluetooth low energy working in your app:  
```
\services\BluetoothLeService.java
\utils\BLEQueue.java
\utils\GattAttributes.java
\utils\QueueItem.java
```

In the app manifest you need at least the following:  
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />  
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />  
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

For a simple and stripped down Activity see BasicBLEActivity.java, here we have the simplest way of connecting to the service to be able to connect, read, write and to get notifications from a Bluetooth low energy device like NINA-B1. This Activity is well documented to get you started in no time.

## Disclaimer
Copyright (C) u-blox

u-blox reserves all rights in this deliverable (documentation, software, etc., hereafter “Deliverable”).

u-blox grants you the right to use, copy, modify and distribute the Deliverable provided hereunder for any purpose without fee.

THIS DELIVERABLE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED WARRANTY. IN PARTICULAR, NEITHER THE AUTHOR NOR U-BLOX MAKES ANY REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY OF THIS DELIVERABLE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.

In case you provide us a feedback or make a contribution in the form of a further development of the Deliverable (“Contribution”), u-blox will have the same rights as granted to you, namely to use, copy, modify and distribute the Contribution provided to us for any purpose without fee.
