package com.adambirdsall.smartdimmer.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.adambirdsall.smartdimmer.BLE.BroadcastReceiver_BTState;
import com.adambirdsall.smartdimmer.BLE.DeviceItem;
import com.adambirdsall.smartdimmer.BLE.ListAdapter_BTLE_Devices;
import com.adambirdsall.smartdimmer.BLE.Scanner_BTLE;
import com.adambirdsall.smartdimmer.Fragments.DiscoveryFragment;
import com.adambirdsall.smartdimmer.Fragments.HelpFragment;
import com.adambirdsall.smartdimmer.Fragments.SetupFragment;
import com.adambirdsall.smartdimmer.R;
import com.adambirdsall.smartdimmer.Utils.EventListener;
import com.adambirdsall.smartdimmer.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class DiscoveryActivity extends AppCompatActivity implements EventListener, View.OnClickListener, AdapterView.OnItemClickListener, SeekBar.OnSeekBarChangeListener, NavigationView.OnNavigationItemSelectedListener {

    private final static String TAG = DiscoveryActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;

    public boolean setupFlag = false;

    private HashMap<String, DeviceItem> mBTDevicesHashMap;
    private ArrayList<DeviceItem> mBTDevicesArrayList;
    private ListAdapter_BTLE_Devices adapter;
    private ArrayList<DeviceItem> selectedGroupsArrayList;

    // BottomSheetBehavior variable
    private BottomSheetBehavior bottomSheetBehavior;
    private BottomSheetBehavior setup_bottomSheet;

    public TextView connectedLabel;
    public TextView brightnessLabel;

    public SeekBar brightnessSeekBar;
    public ScrollView scrollView;
    public Toolbar mainToolbar;

    public ListView mainListView;

    private BroadcastReceiver_BTState mBTStateUpdateReceiver;

    private Scanner_BTLE mBLTLeScanner;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        mainToolbar = (Toolbar) findViewById(R.id.toolbar);
        mainToolbar.setTitleTextColor(Color.parseColor("#289dd8"));
        setSupportActionBar(mainToolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mainToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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

        displayFragment(R.id.nav_discover);
    }

    @Override
    public void discoveryVariables() {

        setupFlag = false;

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        mBLTLeScanner = new Scanner_BTLE(this, 4000, -75);

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        mainListView = new ListView(this);
        mainListView.setAdapter(adapter);
//        mainListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mainListView.setOnItemClickListener(this);

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.addView(mainListView);

        findViewById(R.id.disconnect_button).setOnClickListener(this);

        connectedLabel = (TextView) findViewById(R.id.connectedLabel);
        brightnessLabel = (TextView) findViewById(R.id.brightnessLabel);
        brightnessSeekBar = (SeekBar) findViewById(R.id.brightnessSeekBar);

        brightnessSeekBar.setOnSeekBarChangeListener(this);

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetLayout));
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {

                // Check Logs to see how bottom sheets behaves
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mainToolbar.getMenu().findItem(R.id.action_groups).setEnabled(true);
                        findViewById(R.id.nav_view).setEnabled(true);
                        mBLTLeScanner.disconnectFromDevice();
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        mainToolbar.getMenu().findItem(R.id.action_groups).setEnabled(false);
                        findViewById(R.id.nav_view).setEnabled(false);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                findViewById(R.id.bg).setVisibility(View.VISIBLE);
                findViewById(R.id.bg).setAlpha(slideOffset);
            }
        });

        startScan();
    }

    @Override
    public void setupVariables() {

        setupFlag = true;

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        mBLTLeScanner = new Scanner_BTLE(this, 4000, -75);

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        mainListView = new ListView(this);
        mainListView.setAdapter(adapter);
//        mainListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mainListView.setOnItemClickListener(this);

        scrollView = (ScrollView) findViewById(R.id.setupScrollView);
        scrollView.addView(mainListView);

        findViewById(R.id.setup_disconnect_button).setOnClickListener(this);
        findViewById(R.id.lowest_button).setOnClickListener(this);
        findViewById(R.id.highest_button).setOnClickListener(this);

        connectedLabel = (TextView) findViewById(R.id.setup_connectedLabel);

        setup_bottomSheet = BottomSheetBehavior.from(findViewById(R.id.setupSheetLayout));
        setup_bottomSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Check Logs to see how bottom sheets behaves
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mainToolbar.getMenu().findItem(R.id.action_groups).setEnabled(true);
                        findViewById(R.id.nav_view).setEnabled(true);
                        mBLTLeScanner.disconnectFromDevice();
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        mainToolbar.getMenu().findItem(R.id.action_groups).setEnabled(false);
                        findViewById(R.id.nav_view).setEnabled(false);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                findViewById(R.id.bg).setVisibility(View.VISIBLE);
                findViewById(R.id.bg).setAlpha(slideOffset);
            }
        });

        startScan();
    }

    @Override
    public void helpVariables() {

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
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

    /**
     * On Button Click
     *
     * @param v
     */
    @Override
    public void onClick(View v) {

        boolean didDisconnect = false;

        switch (v.getId()) {
            case R.id.disconnect_button:
                Utils.toast(getApplicationContext(), "Disconnecting..");
                didDisconnect = mBLTLeScanner.disconnectFromDevice();
                if (didDisconnect) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                    // TODO: Groups button rename
                    mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("Scan");
                    mainListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

                } else {
                    // Failed to disconnect
                }
                break;
            case R.id.setup_disconnect_button:
                Utils.toast(getApplicationContext(), "Disconnecting..");
                didDisconnect = mBLTLeScanner.disconnectFromDevice();
                if (didDisconnect) {
                    setup_bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);

                    // TODO: Groups button rename
                    mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("Scan");
                    mainListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

                } else {
                    // Failed to disconnect
                }
                break;
            case R.id.lowest_button:
                System.out.print("");
                break;
            case R.id.highest_button:
                System.out.print("");
                break;
            default:
                break;
        }
    }

    /**
     * On List View Item Click
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        String buttonTitle = mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString();
        DeviceItem deviceItem = (DeviceItem) parent.getItemAtPosition(position);
//        CheckBox checkBox = (CheckBox) parent.getItemAtP

        // If you want to select a single device
        if (buttonTitle.equals("Groups")) {
            boolean didConnect = mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), getApplicationContext());
            if (didConnect) {
                if (setupFlag) {
                    setup_bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            } else {
                // Failed to connect
            }
        }
        // If you want to select multiple devices
        else if (buttonTitle.equals("Connect")) {
            // Add device to an arraylist

//            CheckBox checkBox = (CheckBox) parent.findViewById(R.id.checkBox);
//            checkBox.setChecked(!checkBox.isChecked());
        } else {
            boolean didConnect = mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), getApplicationContext());
            if (didConnect) {
                if (setupFlag) {
                    setup_bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            } else {
                // Failed to connect
            }
        }
    }

    /**
     * On Topbar Menu Item Click
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_groups) {
            // If the title of the button is 'Groups'
            // TODO: groups button rename
            if (item.getTitle().equals("Scan")) {
                startScan();
//                item.setTitle("Connect");
//                mainListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

            } else { // Else if the title is 'Connect'

                SparseBooleanArray checked = mainListView.getCheckedItemPositions();
                if (checked.size() == 0) {
                    Utils.toast(getApplicationContext(), "Please select a device");
                    return true;
                }
                ArrayList<DeviceItem> selectedItems = new ArrayList<>();
                for (int i = 0; i < checked.size(); i++) {
                    // Item position in adapter
                    int position = checked.keyAt(i);
                    if (checked.valueAt(i))
                        selectedItems.add(adapter.getItem(position));
                }

                for (DeviceItem deviceItem : selectedItems) {
                    boolean didConnect = mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), getApplicationContext());
                    if (didConnect) {

                    } else {

                    }
                }

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * On SideMenu item selected
     *
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        //calling the method displayselectedscreen and passing the id of selected menu
        displayFragment(item.getItemId());
        //make this method blank
        return true;
    }

    private void displayFragment(int id) {

        Fragment fragment = null;

        switch (id) {
            case R.id.nav_discover:
                fragment = new DiscoveryFragment();
                break;
            case R.id.nav_help:
                fragment = new HelpFragment();
                break;
            case R.id.nav_setup:
                fragment = new SetupFragment();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
            ft.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    /**
     * Go back button pressed in the SideMenu
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Functions for the seekbar and changing brightness value
     *
     * @param seekBar
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        try {
            brightnessLabel.setText(String.valueOf(progress));
            //TODO: groups button rename
            if (mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString().equals("Scan")) {
                mBLTLeScanner.writeCustomCharacteristic(progress);
            } else {
//                for
            }
        } catch (Exception e) {

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

            if (newDevice.getName() != null && newDevice.getName().startsWith("SmartDimmer")) {
                mBTDevicesHashMap.put(address, newDevice);
                mBTDevicesArrayList.add(newDevice);
            }

        } else {
            mBTDevicesHashMap.get(address).setRSSI(new_rssi);
        }

        adapter.notifyDataSetChanged();
    }

    public void startScan() {
        mainListView.setEnabled(false);

        mBTDevicesArrayList.clear();
        mBTDevicesHashMap.clear();

        adapter.notifyDataSetChanged();

        mBLTLeScanner.start();
    }

    public void stopScan() {
        mainListView.setEnabled(true);

        mBLTLeScanner.stop();
    }
}
