package com.adambirdsall.smartdimmer.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.adambirdsall.smartdimmer.BLE.BroadcastReceiver_BTState;
import com.adambirdsall.smartdimmer.BLE.DeviceItem;
import com.adambirdsall.smartdimmer.BLE.ListAdapter_BTLE_Devices;
import com.adambirdsall.smartdimmer.BLE.Scanner_BTLE;
import com.adambirdsall.smartdimmer.Fragments.DiscoveryFragment;
import com.adambirdsall.smartdimmer.Fragments.HelpFragment;
import com.adambirdsall.smartdimmer.Fragments.SetupFragment;
import com.adambirdsall.smartdimmer.R;
import com.adambirdsall.smartdimmer.Utils.DeviceDatabase;
import com.adambirdsall.smartdimmer.Utils.EventListener;
import com.adambirdsall.smartdimmer.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DiscoveryActivity extends AppCompatActivity implements EventListener, View.OnClickListener, AdapterView.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener, SwipeRefreshLayout.OnRefreshListener, net.qiujuer.genius.ui.widget.SeekBar.OnSeekBarChangeListener {

    private final static String TAG = DiscoveryActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;

    public boolean setupFlag = false;
    public boolean discoverFlag = false;

    // Database
    private DeviceDatabase deviceDb;

    // Device maps and arrays
    private HashMap<String, DeviceItem> mBTDevicesHashMap;
    private ArrayList<DeviceItem> mBTDevicesArrayList;
    private ListAdapter_BTLE_Devices adapter;
    private ArrayList<DeviceItem> selectedGroupsArrayList;

    // BottomSheetBehavior variable
    private BottomSheetBehavior bottomSheetBehavior;
    private BottomSheetBehavior setup_bottomSheet;

    // Text view variables
    public TextView connectedLabel;
    public TextView brightnessLabel;

    // List views, seekbars, and toolbar
    public net.qiujuer.genius.ui.widget.SeekBar stepSeekBar;
    public ScrollView scrollView;
    public ScrollView setupScrollView;
    public Toolbar mainToolbar;
    public ListView mainListView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SwipeRefreshLayout setupSwipeRefresh;
    private DrawerLayout mainDrawer;

    // Bluetooth variables
    private BroadcastReceiver_BTState mBTStateUpdateReceiver;
    private Scanner_BTLE mBLTLeScanner;
    private List<BluetoothGatt> groupsList;
    private BluetoothGatt mainBleGatt;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        deviceDb = new DeviceDatabase(this);

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
                builder.setTitle("Allow Location for Bluetooth?");
                builder.setMessage("Please allow this app to access location to use Bluetooth on your device");
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
        discoverFlag = true;

        if (findViewById(R.id.action_groups) != null) {
            findViewById(R.id.action_groups).setEnabled(true);
            findViewById(R.id.action_groups).setVisibility(View.VISIBLE);
            mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("Groups");
        }

        groupsList = new ArrayList<>();

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        mBLTLeScanner = new Scanner_BTLE(this, 3000, -75);

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        mainListView = new ListView(this);
        mainListView.setAdapter(adapter);

        mainListView.setOnItemClickListener(this);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.addView(mainListView);

        mainDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        findViewById(R.id.disconnect_button).setOnClickListener(this);

        connectedLabel = (TextView) findViewById(R.id.connectedLabel);
        brightnessLabel = (TextView) findViewById(R.id.brightnessLabel);
        stepSeekBar = (net.qiujuer.genius.ui.widget.SeekBar) findViewById(R.id.stepSeekBar);

        stepSeekBar.setOnSeekBarChangeListener(this);

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetLayout));
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {

                // Check Logs to see how bottom sheets behaves
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mainToolbar.getMenu().findItem(R.id.action_groups).setEnabled(true);
                        findViewById(R.id.nav_view).setEnabled(true);
                        findViewById(R.id.swipe_refresh_layout).setEnabled(true);
                        scrollView.setEnabled(true);
                        mainListView.setEnabled(true);
                        mainToolbar.setEnabled(true);
                        mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                        mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("Groups");

                        disconnectFromDevices();
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        mainToolbar.getMenu().findItem(R.id.action_groups).setEnabled(false);
                        findViewById(R.id.nav_view).setEnabled(false);
                        findViewById(R.id.swipe_refresh_layout).setEnabled(false);
                        scrollView.setEnabled(false);
                        mainListView.setEnabled(false);
                        mainToolbar.setEnabled(false);
                        mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                        findViewById(R.id.drawer_layout).setEnabled(false);
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
        discoverFlag = false;

        if (findViewById(R.id.action_groups) != null) {
            findViewById(R.id.action_groups).setEnabled(false);
            findViewById(R.id.action_groups).setVisibility(View.INVISIBLE);
        }
        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        mBLTLeScanner = new Scanner_BTLE(this, 3000, -75);

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        mainListView = new ListView(this);
        mainListView.setAdapter(adapter);
        mainListView.setOnItemClickListener(this);
        setupSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.setup_swipe_refresh_layout);
        setupSwipeRefresh.setOnRefreshListener(this);

        setupScrollView = (ScrollView) findViewById(R.id.setupScrollView);
        setupScrollView.addView(mainListView);

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
                        findViewById(R.id.action_groups).setEnabled(false);
                        findViewById(R.id.action_groups).setVisibility(View.INVISIBLE);
                        mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("");

                        findViewById(R.id.nav_view).setEnabled(true);
                        findViewById(R.id.setup_swipe_refresh_layout).setEnabled(true);
                        setupScrollView.setEnabled(true);
                        mainListView.setEnabled(true);
                        mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

                        disconnectFromDevices();
                        break;

                    case BottomSheetBehavior.STATE_EXPANDED:
                        findViewById(R.id.action_groups).setEnabled(false);
                        findViewById(R.id.action_groups).setVisibility(View.INVISIBLE);
                        mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("");

                        findViewById(R.id.nav_view).setEnabled(false);
                        findViewById(R.id.setup_swipe_refresh_layout).setEnabled(false);
                        setupScrollView.setEnabled(false);
                        mainListView.setEnabled(false);
                        mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                        findViewById(R.id.drawer_layout).setEnabled(false);
                        break;

                    case BottomSheetBehavior.STATE_DRAGGING:
                        setup_bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
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
    public void onRefresh() {

        mBTDevicesHashMap.clear();
        mBTDevicesArrayList.clear();

        startScan();
    }

    @Override
    public void helpVariables() {

    }

    @Override
    public void addToGroupsList(BluetoothGatt newDevice) {
        groupsList.add(newDevice);
    }

    @Override
    public void deleteFromGroupsList(BluetoothGatt removeDevice) {
        groupsList.remove(removeDevice);
    }

    @Override
    public void disconnectFromDevices() {
        if (mainBleGatt == null && groupsList == null) {
            return;
        } else if(mainBleGatt == null && groupsList.size() == 0) {
            return;
        } else {

            if (groupsList.size() > 0) {
                for (BluetoothGatt disconnectDevice : groupsList) {
                    mBLTLeScanner.disconnectFromDevice(disconnectDevice);
                }
                groupsList.clear();
            }

            if (mainBleGatt != null) {
                mBLTLeScanner.disconnectFromDevice(mainBleGatt);
                mainBleGatt = null;
            }
        }
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
        String buttonTitle = mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString();

        switch (v.getId()) {
            case R.id.disconnect_button:
                Utils.toast(getApplicationContext(), "Disconnecting..");

                if (buttonTitle.equals("Groups")) {

                    mBLTLeScanner.disconnectFromDevice(mainBleGatt);

                    mainBleGatt = null;

                } else {

                    for (BluetoothGatt disconnectDevice : groupsList) {
                        mBLTLeScanner.disconnectFromDevice(disconnectDevice);
                    }
                    groupsList.clear();
                }

                mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("Groups");
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                int childCount = mainListView.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    mainListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }

                break;
            case R.id.setup_disconnect_button:
                Utils.toast(getApplicationContext(), "Disconnecting..");
                didDisconnect = mBLTLeScanner.disconnectFromDevice(mainBleGatt);
                if (didDisconnect) {
                    setup_bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);

                    // TODO: Groups button rename
                    mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("");

                    mainBleGatt = null;

                } else {
                    // Failed to disconnect
                    Utils.toast(getApplicationContext(), "Failed to disconnect");
                }
                break;
            case R.id.lowest_button:
                mBLTLeScanner.writeCustomCharacteristic(202, mainBleGatt);
                break;
            case R.id.highest_button:
                mBLTLeScanner.writeCustomCharacteristic(201, mainBleGatt);
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

        // If you want to select a single device
        if (buttonTitle.equals("Groups") || buttonTitle.equals("")) {
            mainBleGatt = mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), getApplicationContext());
            if (mainBleGatt != null) {
                if (setupFlag) {
                    setup_bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            } else {
                // TODO: Failed to connect
                Utils.toast(getApplicationContext(), "Failed to connect");
            }
        }
        // If you want to select multiple devices
        else if (buttonTitle.equals("Connect")) {
            // Add device to an arraylist

            ColorDrawable listItemColor = (ColorDrawable) parent.getChildAt(position).getBackground();

            if (listItemColor == null || listItemColor.getColor() == Color.TRANSPARENT) {

                // connect and change color
                BluetoothGatt newGatt = mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), getApplicationContext());

                if (newGatt != null) {
                    groupsList.add(newGatt);

                    // TODO: set with nice color and checkmark picture
                    parent.getChildAt(position).setBackgroundColor(Color.parseColor("#bee2f3"));
                }


            } else {
                // disconnect and put transparent color

                boolean didDisconnect = mBLTLeScanner.disconnectFromDevice(groupsList.get(position));
                if (didDisconnect) {
                    parent.getChildAt(position).setBackgroundColor(Color.TRANSPARENT);
                    groupsList.remove(position);
                } else {
                    // Failed to disconnect
                    Utils.toast(getApplicationContext(), "Failed to disconnect");
                }
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
            if (item.getTitle().equals("Groups")) {

                item.setTitle("Connect");

            } else { // Else if the title is 'Connect'

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

        displayFragment(item.getItemId());

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
    public void onProgressChanged(net.qiujuer.genius.ui.widget.SeekBar seekBar, int progress, boolean fromUser) {
        String buttonTitle = mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString();

        try {
            brightnessLabel.setText(String.valueOf(progress * 10));
            //TODO: groups button rename
            if (buttonTitle.equals("Groups")) {
                mBLTLeScanner.writeCustomCharacteristic(progress * 10, mainBleGatt);
            } else {

                for (BluetoothGatt writeGatt : groupsList) {
                    mBLTLeScanner.writeCustomCharacteristic(progress * 10, writeGatt);
                }

            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onStartTrackingTouch(net.qiujuer.genius.ui.widget.SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(net.qiujuer.genius.ui.widget.SeekBar seekBar) {

    }

    /************************************************************************************************/
    /**
     *
     * @param device
     * @param new_rssi
     */
    public void addDevice(BluetoothDevice device, int new_rssi) {
//        String address = device.getAddress();

        if (device.getName() == null || device.getName().length() == 0) {
            return;
        }
        if (!mBTDevicesHashMap.containsKey(device.getAddress())) {
            DeviceItem newDevice = new DeviceItem(device);

            System.out.println(newDevice);

            if (newDevice.getName() != null) {
                mBTDevicesHashMap.put(newDevice.getAddress(), newDevice);
                mBTDevicesArrayList.add(newDevice);
            }

            newDevice = null;
        } else {

        }

        adapter.notifyDataSetChanged();
    }

    public void startScan() {
        mainListView.setEnabled(false);

        if (setupFlag) {
            setupSwipeRefresh.setRefreshing(true);
        } else {
            swipeRefreshLayout.setRefreshing(true);
        }

        mBTDevicesArrayList.clear();
        mBTDevicesHashMap.clear();

        adapter.notifyDataSetChanged();

        mBLTLeScanner.start();
    }

    public void stopScan() {
        mainListView.setEnabled(true);

        mBLTLeScanner.stop();

        if (setupFlag) {
            setupSwipeRefresh.setRefreshing(false);
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
