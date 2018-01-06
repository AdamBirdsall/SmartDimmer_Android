package com.adambirdsall.smartdimmer.BLE;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.BottomSheetBehavior;
import android.util.Log;

import com.adambirdsall.smartdimmer.Activities.DiscoveryActivity;
import com.adambirdsall.smartdimmer.R;
import com.adambirdsall.smartdimmer.Utils.DeviceDatabase;
import com.adambirdsall.smartdimmer.Utils.DeviceObject;
import com.adambirdsall.smartdimmer.Utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * Created by AdamBirdsall on 7/25/17.
 * @author AdamBirdsall
 */

public class Scanner_BTLE extends DiscoveryActivity {

    private DiscoveryActivity ma;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothLeScanner bluetoothLeScanner;

    public List<BluetoothGatt> groupOfDevices;

    private ScanSettings settings;
    private List<ScanFilter> filters;

    private boolean mScanning;
    private boolean isSetupView;
    private boolean isGroupsView;
    private Handler mHandler;

    private long scanPeriod;
    private int signalStrength;

    private final UUID serviceUUID = UUID.fromString("00001523-1212-EFDE-1523-785FEABCD123");
    private final UUID writeUUID = UUID.fromString("00001525-1212-EFDE-1523-785FEABCD123");

    private boolean switchClicked = false;

    public Scanner_BTLE(DiscoveryActivity mainActivity, long scanPeriod, int signalStrength) {
        ma = mainActivity;
        mHandler = new Handler();
        this.scanPeriod = scanPeriod;
        this.signalStrength = signalStrength;
        this.groupOfDevices = new ArrayList<>();

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

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {

        final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(bleScanCallBack);
                    ma.stopScan();
                }
            }, scanPeriod);

            bluetoothLeScanner.startScan(bleScanCallBack);
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(bleScanCallBack);
            ma.stopScan();
        }
    }

    private ScanCallback bleScanCallBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ma.addDevice(result.getDevice(), result.getRssi());
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };


    public void disconnectFromDevice(boolean isGroups, String deviceAddress, boolean isSetup) {

        try {

            if (isSetup) {

                mBluetoothGatt.close();
                mBluetoothGatt.disconnect();
                mBluetoothGatt = null;

                ma.setup_bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                ma.renameTextEdit.setText("");
                ma.mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("");

            } else {
                if (isGroups) {

                    // Clear all with null
                    if (deviceAddress == null) {
                        for (BluetoothGatt disconnectGatt : groupOfDevices) {
                            disconnectGatt.close();
                            disconnectGatt.disconnect();
                        }

                        groupOfDevices.clear();
                    } else {

                        for (BluetoothGatt disconnectGatt : groupOfDevices) {

                            if (disconnectGatt.getDevice().getAddress().equals(deviceAddress)) {
                                disconnectGatt.close();
                                disconnectGatt.disconnect();
                                groupOfDevices.remove(disconnectGatt);
                            }
                        }
                    }

                } else {
                    mBluetoothGatt.close();
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt = null;
                }

                ma.bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } catch (Exception e) {
            Utils.toast(getApplicationContext(), "Failed to disconnect");
        }
    }

    public void connectToDevice(BluetoothDevice bluetoothDevice, boolean isGroups, DeviceDatabase deviceDb, boolean isSetup) {

        this.isSetupView = isSetup;
        String deviceName = "";
        List<DeviceObject> allDevices = deviceDb.getAllDevices();
        for (DeviceObject deviceObject : allDevices) {
            if (deviceObject.getMacAddress().equals(bluetoothDevice.getAddress())) {
                deviceName = deviceObject.getDeviceName();
            }
        }

        if (isGroups) {
            this.isGroupsView = true;
            BluetoothGatt groupsGatt = bluetoothDevice.connectGatt(this, false, btleGattCallback);
            groupOfDevices.add(groupsGatt);
            scanLeDevice(false);

        } else {
            this.isGroupsView = false;
            if (mBluetoothGatt == null) {

                if (isSetup) {
                    if (deviceName.equals("")) {
                        ma.renameTextEdit.setText(bluetoothDevice.getName());
                    } else {
                        ma.renameTextEdit.setText(deviceName);
                    }
                }

                mBluetoothGatt = bluetoothDevice.connectGatt(this, false, btleGattCallback);
                scanLeDevice(false);// will stop after first device detection
            }
        }
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");

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

                            if (isSetupView) {
                                ma.setup_bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                            } else {
                                if (!isGroupsView) {
                                    ma.bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                                }
                            }
                        }
                    });

                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:

                    Log.e("gattCallback", "STATE_DISCONNECTED");

                    ma.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ma.connectedLabel.setText(R.string.notConnected);
                            ma.mainListView.setClickable(true);
                            ma.mainListView.setEnabled(true);
                        }
                    });

                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            BluetoothGattService service = gatt.getService(serviceUUID);
            final BluetoothGattCharacteristic characteristic = service.getCharacteristic(writeUUID);
            writeCharacteristic = characteristic;
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };


    public void writeCustomCharacteristic(final int value, boolean isGroups, DeviceDatabase deviceDb) {

        if (bluetoothAdapter == null) {
            return;
        }

        if (isGroups) {

            if (groupOfDevices.size() == 0) {
                return;
            }

            for (BluetoothGatt writeToGatt : groupOfDevices) {

                /*check if the service is available on the device*/
                BluetoothGattService mCustomService = writeToGatt.getService(serviceUUID);
                if(mCustomService == null){
                    return;
                }

                /*get the read characteristic from the service*/
                BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(writeUUID);
                mWriteCharacteristic.setValue(value,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);

                updateDevice(writeToGatt.getDevice(), value, deviceDb);

                if(!writeToGatt.writeCharacteristic(mWriteCharacteristic)) {

                } else {

                    ma.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ma.brightnessLabel.setText(String.valueOf(value));

                            if (switchClicked) {
                                ma.stepSeekBar.setProgress(value/10);
                            }
                        }
                    });

                    System.out.println("SUCCESSFULLY WROTE TO CHARACTERISTIC");
                }
            }

        } else {

            if (mBluetoothGatt == null) {
                return;
            }

            /*check if the service is available on the device*/
            BluetoothGattService mCustomService = mBluetoothGatt.getService(serviceUUID);
            if(mCustomService == null){
                return;
            }

            /*get the read characteristic from the service*/
            BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(writeUUID);
            mWriteCharacteristic.setValue(value,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);

            updateDevice(mBluetoothGatt.getDevice(), value, deviceDb);

            if(!mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)) {

            } else {
                ma.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ma.brightnessLabel.setText(String.valueOf(value));

                        if (switchClicked) {
                            ma.stepSeekBar.setProgress(value/10);
                        }
                    }
                });

                System.out.println("SUCCESSFULLY WROTE TO CHARACTERISTIC");
            }
        }

        switchClicked = false;
    }

    /**
     * SQL functions to update values
     */

    public void updateDevice(BluetoothDevice updateDevice, int brightnessValue, DeviceDatabase deviceDb) {

        DeviceObject getDevice = deviceDb.getDevice(updateDevice.getAddress());

        getDevice.setDeviceName(getDevice.getDeviceName());
        getDevice.setMacAddress(getDevice.getMacAddress());
        getDevice.setPreviousValue(getDevice.getBrightnessValue());
        getDevice.setBrightnessValue(String.valueOf(brightnessValue));

        deviceDb.updateDeviceBrightness(getDevice);
    }


    public void updateDeviceName(String nameText, DeviceDatabase deviceDb) {

        DeviceObject updateDevice = new DeviceObject();

        // If the user deletes the text in the text box, set name to the original device name
        if (nameText.equals("")) {
            updateDevice.setDeviceName(mBluetoothGatt.getDevice().getName());
        } else {
            updateDevice.setDeviceName(nameText);
        }

        updateDevice.setMacAddress(mBluetoothGatt.getDevice().getAddress());
        updateDevice.setBrightnessValue("0");
        updateDevice.setPreviousValue("0");

        deviceDb.updateDevice(updateDevice);
    }


    public void updateSwitchBrightness(boolean isGroups, DeviceDatabase deviceDb, boolean isOn) {

        switchClicked = true;

        if (isGroups) {

            if (isOn) {
                DeviceObject updateDevice = deviceDb.getDevice(groupOfDevices.get(0).getDevice().getAddress());

                writeCustomCharacteristic(Integer.parseInt(updateDevice.getPreviousValue()), true, deviceDb);
            } else {
                writeCustomCharacteristic(0, true, deviceDb);
            }

        } else {
            DeviceObject updateDevice = deviceDb.getDevice(mBluetoothGatt.getDevice().getAddress());

            // Device turns on, sets brightness of previous value
            if (isOn) {

                writeCustomCharacteristic(Integer.parseInt(updateDevice.getPreviousValue()), false, deviceDb);

            } else {

                writeCustomCharacteristic(0, false, deviceDb);
            }
        }
    }
}
