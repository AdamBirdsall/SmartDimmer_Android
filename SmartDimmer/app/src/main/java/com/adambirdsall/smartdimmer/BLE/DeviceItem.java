package com.adambirdsall.smartdimmer.BLE;

import android.bluetooth.BluetoothDevice;

/**
 * Created by AdamBirdsall on 7/18/17.
 */

public class DeviceItem {

    private BluetoothDevice bluetoothDevice;
    private int rssi;

    public DeviceItem(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    public String getName() {
        return bluetoothDevice.getName();
    }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    public int getRssi() {
        return rssi;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }
}
