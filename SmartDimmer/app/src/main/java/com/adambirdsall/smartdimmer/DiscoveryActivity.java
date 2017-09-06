package com.adambirdsall.smartdimmer;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class DiscoveryActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, SeekBar.OnSeekBarChangeListener, NavigationView.OnNavigationItemSelectedListener {

    private final static String TAG = DiscoveryActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;

    private HashMap<String, DeviceItem> mBTDevicesHashMap;
    private ArrayList<DeviceItem> mBTDevicesArrayList;
    private ListAdapter_BTLE_Devices adapter;

    public Button btn_Scan;

    // BottomSheetBehavior variable
    private BottomSheetBehavior bottomSheetBehavior;
    private Button expandBottomSheetButton;
    private Button collapseBottomSheetButton;

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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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
//        brightnessLabel = (TextView) findViewById(R.id.brightnessLabel);
        brightnessSeekBar = (SeekBar) findViewById(R.id.brightnessSeekBar);

        brightnessSeekBar.setOnSeekBarChangeListener(this);

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetLayout));
// Capturing the callbacks for bottom sheet
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {

                // Check Logs to see how bottom sheets behaves
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        Log.e("Bottom Sheet Behaviour", "STATE_COLLAPSED");
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.e("Bottom Sheet Behaviour", "STATE_DRAGGING");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Log.e("Bottom Sheet Behaviour", "STATE_EXPANDED");
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.e("Bottom Sheet Behaviour", "STATE_HIDDEN");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.e("Bottom Sheet Behaviour", "STATE_SETTLING");
                        break;
                }
            }


            @Override
            public void onSlide(View bottomSheet, float slideOffset) {

            }
        });

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
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_groups) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//                if (btn_Scan.getText().equals("Scan Again")) {
//
//                    if (!mBLTLeScanner.isScanning()) {
//                        startScan();
//                    } else {
//                        stopScan();
//                    }
//                }
//                if (btn_Scan.getText().equals("Disconnect")) {
//                    Utils.toast(getApplicationContext(), "Disconnecting..");
//                    mBLTLeScanner.disconnectFromDevice();
//                }

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
