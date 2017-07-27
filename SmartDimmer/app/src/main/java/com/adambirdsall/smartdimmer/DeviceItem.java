package com.adambirdsall.smartdimmer;

import android.bluetooth.BluetoothDevice;
import android.net.NetworkInfo;
import android.os.ParcelUuid;

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
