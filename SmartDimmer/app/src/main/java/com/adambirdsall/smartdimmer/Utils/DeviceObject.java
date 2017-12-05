package com.adambirdsall.smartdimmer.Utils;

/**
 * Created by AdamBirdsall on 12/4/17.
 */

public class DeviceObject {

    private String macAddress;
    private String deviceName;
    private String brightnessValue;
    private String previousValue;

    public DeviceObject() {

    }

    public DeviceObject(String macAddress, String deviceName, String brightnessValue, String previousValue) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.brightnessValue = brightnessValue;
        this.previousValue = previousValue;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBrightnessValue() {
        return brightnessValue;
    }

    public void setBrightnessValue(String brightnessValue) {
        this.brightnessValue = brightnessValue;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }
}
