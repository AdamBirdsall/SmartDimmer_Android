package com.adambirdsall.smartdimmer.Utils;

import android.bluetooth.BluetoothGatt;

/**
 * Created by AdamBirdsall on 10/10/17.
 */

public interface EventListener {

    void setupVariables();

    void discoveryVariables();

    void helpVariables();

    void disconnectFromDevices();

    void addToGroupsList(BluetoothGatt newDevice);

    void deleteFromGroupsList(BluetoothGatt removeDevice);

}