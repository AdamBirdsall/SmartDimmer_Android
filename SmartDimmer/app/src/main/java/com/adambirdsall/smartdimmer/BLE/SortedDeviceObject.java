package com.adambirdsall.smartdimmer.BLE;


/**
 * Created by AdamBirdsall on 3/4/18.
 */

public class SortedDeviceObject {

    private String deviceName;
    private String deviceUuid;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }
}
