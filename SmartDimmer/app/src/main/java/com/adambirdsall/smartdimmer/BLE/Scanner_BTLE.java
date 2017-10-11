package com.adambirdsall.smartdimmer.BLE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import com.adambirdsall.smartdimmer.Activities.DiscoveryActivity;
import com.adambirdsall.smartdimmer.R;
import com.adambirdsall.smartdimmer.Utils.Utils;

import java.util.List;
import java.util.UUID;

/**
 * Created by AdamBirdsall on 7/25/17.
 */

public class Scanner_BTLE {

    private DiscoveryActivity ma;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;

    private boolean mScanning;
    private Handler mHandler;

    private long scanPeriod;
    private int signalStrength;

    private final UUID serviceUUID = UUID.fromString("00001523-1212-EFDE-1523-785FEABCD123");
    private final UUID writeUUID = UUID.fromString("00001525-1212-EFDE-1523-785FEABCD123");
    private final UUID readUUID = UUID.fromString("00001524-1212-EFDE-1523-785FEABCD123");

    public Scanner_BTLE(DiscoveryActivity mainActivity, long scanPeriod, int signalStrength) {
        ma = mainActivity;
        mHandler = new Handler();
        this.scanPeriod = scanPeriod;
        this.signalStrength = signalStrength;

        final BluetoothManager bluetoothManager = (BluetoothManager) ma.getSystemService(Context.BLUETOOTH_SERVICE);

        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void start() {
        if (!Utils.checkBluetooth(bluetoothAdapter)) {
            Utils.requestUserBluetooth(ma);
            ma.stopScan();
        } else {
            scanLeDevice(true);
        }
    }

    public void stop() {
        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enabled) {
        if (enabled && !mScanning) {
            Utils.toast(ma.getApplication(), "Starting BLE Scan...");

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Utils.toast(ma.getApplicationContext(), "Stopping BLE Scan...");
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(mLeScanCallback);

                    ma.stopScan();
                }
            }, scanPeriod);

            mScanning = true;

            bluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            final int new_rssi = i;
            if (new_rssi > signalStrength) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ma.addDevice(bluetoothDevice, new_rssi);
                    }
                });
            }
        }
    };

    public boolean disconnectFromDevice(BluetoothGatt disconnectDevice) {
        try {
            disconnectDevice.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public BluetoothGatt connectToDevice(BluetoothDevice bluetoothDevice, Context context) {
        try {
            String bluetoothName = bluetoothDevice.getName();
            return bluetoothDevice.connectGatt(context, false, btleGattCallback);
        } catch (Exception e) {
            return null;
        }
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            byte[] data = characteristic.getValue();
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            System.out.println(newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {

                ma.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String buttonTitle = ma.mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString();
                        ma.connectedLabel.setText(R.string.connectedTo);

                        if (buttonTitle.equals("Groups")) {
                            ma.mainListView.setClickable(false);
                            ma.mainListView.setEnabled(false);
                        } else {
                            ma.mainListView.setClickable(true);
                            ma.mainListView.setEnabled(true);
                        }
                    }
                });

                mBluetoothGatt = gatt;
                gatt.discoverServices();
            }
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {

                ma.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ma.connectedLabel.setText(R.string.notConnected);
                        ma.mainListView.setClickable(true);
                        ma.mainListView.setEnabled(true);
                    }
                });

                mBluetoothGatt = null;
                readCharacteristic = null;
                writeCharacteristic = null;
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {

            BluetoothGattService service = gatt.getService(serviceUUID);
            final BluetoothGattCharacteristic characteristic = service.getCharacteristic(writeUUID);
            readCharacteristic = service.getCharacteristic(readUUID);
            writeCharacteristic = characteristic;
        }
    };


    public void readCustomCharacteristic() {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(serviceUUID);
        if(mCustomService == null){
            return;
        }

        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(readUUID);
        if(!mBluetoothGatt.readCharacteristic(mReadCharacteristic)){
        }
    }

    public void writeCustomCharacteristic(int value, BluetoothGatt newDevice) {
        if (bluetoothAdapter == null || mBluetoothGatt == null || newDevice == null) {
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = newDevice.getService(serviceUUID);
        if(mCustomService == null){
            return;
        }

        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(writeUUID);
        mWriteCharacteristic.setValue(value,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);


        if(!newDevice.writeCharacteristic(mWriteCharacteristic)){

        } else {
            System.out.println("SUCCESSFULLY WROTE TO CHARACTERISTIC");
        }
    }

}
