package com.ublox.BLE.utils;

public enum PhyMode {
    PHY_UNDEFINED,
    PHY_1M,
    PHY_2M,
    PHY_CODED;

    public static PhyMode fromInteger(int mode) {
        switch (mode) {
            case 1:
                return PHY_1M;
            case 2:
                return PHY_2M;
            case 3:
                return PHY_CODED;
            default:
                return PHY_UNDEFINED;
        }
    }

    public static boolean is2MPhyEnabled(PhyMode phyMode) {
        return phyMode == PHY_2M;
    }

    @Override
    public String toString() {
        switch (this) {
            case PHY_1M: return "1M Phy";
            case PHY_2M: return "2M Phy";
            case PHY_CODED: return "Coded Phy";
            default: return "undefined";
        }
    }
}
