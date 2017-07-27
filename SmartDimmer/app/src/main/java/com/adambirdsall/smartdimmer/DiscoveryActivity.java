package com.adambirdsall.smartdimmer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DiscoveryActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, SeekBar.OnSeekBarChangeListener {

    private final static String TAG = DiscoveryActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;

    private HashMap<String, DeviceItem> mBTDevicesHashMap;
    private ArrayList<DeviceItem> mBTDevicesArrayList;
    private ListAdapter_BTLE_Devices adapter;

    public Button btn_Scan;
    public TextView connectedLabel;
    public TextView brightnessLabel;

    public SeekBar brightnessSeekBar;
    public ScrollView scrollView;

    public ListView mainListView;

    private BroadcastReceiver_BTState mBTStateUpdateReceiver;

    private Scanner_BTLE mBLTLeScanner;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.toast(getApplicationContext(), "BLE not supported");
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("");
                builder.setMessage("");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        mBLTLeScanner = new Scanner_BTLE(this, 4000, -75);

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        mainListView = new ListView(this);
        mainListView.setAdapter(adapter);
        mainListView.setOnItemClickListener(this);

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.addView(mainListView);

        btn_Scan = (Button) findViewById(R.id.btn_scan);
        findViewById(R.id.btn_scan).setOnClickListener(this);

        connectedLabel = (TextView) findViewById(R.id.connectedLabel);
        brightnessLabel = (TextView) findViewById(R.id.brightnessLabel);
        brightnessSeekBar = (SeekBar) findViewById(R.id.brightnessSeekBar);

        brightnessSeekBar.setOnSeekBarChangeListener(this);

        startScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(mBTStateUpdateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopScan();
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mBTStateUpdateReceiver);
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {

            } else if (resultCode == RESULT_CANCELED) {
                Utils.toast(getApplicationContext(), "Please turn on Bluetooth");
            }
        }
    }


    /************************************************************************************************/

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DeviceItem deviceItem = (DeviceItem) parent.getItemAtPosition(position);

        mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), getApplicationContext());
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        brightnessLabel.setText(String.valueOf(progress));
        mBLTLeScanner.writeCustomCharacteristic(progress);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_scan:

                if (btn_Scan.getText().equals("Scan Again")) {

                    if (!mBLTLeScanner.isScanning()) {
                        startScan();
                    } else {
                        stopScan();
                    }
                }
                if (btn_Scan.getText().equals("Disconnect")) {
                    Utils.toast(getApplicationContext(), "Disconnecting..");
                    mBLTLeScanner.disconnectFromDevice();
                }

                break;
            default:
                break;
        }
    }



/************************************************************************************************/
    /**
     *
     * @param device
     * @param new_rssi
     */
    public void addDevice(BluetoothDevice device, int new_rssi) {
        String address = device.getAddress();

        if (!mBTDevicesHashMap.containsKey(address)) {
            DeviceItem newDevice = new DeviceItem(device);
            newDevice.setRSSI(new_rssi);

            System.out.println(newDevice);

            mBTDevicesHashMap.put(address, newDevice);
            mBTDevicesArrayList.add(newDevice);
        } else {
            mBTDevicesHashMap.get(address).setRSSI(new_rssi);
        }

        adapter.notifyDataSetChanged();
    }

    public void startScan() {
        btn_Scan.setText("Scanning...");

        mainListView.setEnabled(false);

        mBTDevicesArrayList.clear();
        mBTDevicesHashMap.clear();

        adapter.notifyDataSetChanged();

        mBLTLeScanner.start();
    }

    public void stopScan() {
        btn_Scan.setText("Scan Again");

        mainListView.setEnabled(true);

        mBLTLeScanner.stop();
    }
}
