package com.ublox.BLE.utils;

public class WrongParameterException extends Exception {
    public WrongParameterException() {
        super("Some of the parameters are invalid!");
    }
}
