package com.adambirdsall.smartdimmer.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
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
import com.adambirdsall.smartdimmer.Utils.DeviceObject;
import com.adambirdsall.smartdimmer.Utils.EventListener;
import com.adambirdsall.smartdimmer.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DiscoveryActivity extends AppCompatActivity implements EventListener, View.OnClickListener, AdapterView.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener, SwipeRefreshLayout.OnRefreshListener, net.qiujuer.genius.ui.widget.SeekBar.OnSeekBarChangeListener, Switch.OnCheckedChangeListener {

    private final static String TAG = DiscoveryActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;

    public boolean setupFlag = false;
    public boolean discoverFlag = false;

    // Database
    private DeviceDatabase deviceDb;
    private List<DeviceObject> listOfDevices;

    // Device maps and arrays
    private HashMap<String, DeviceItem> mBTDevicesHashMap;
    private ArrayList<DeviceItem> mBTDevicesArrayList;
    private ListAdapter_BTLE_Devices adapter;

    // BottomSheetBehavior variable
    public BottomSheetBehavior bottomSheetBehavior;
    public BottomSheetBehavior setup_bottomSheet;

    // Text view variables
    public TextView connectedLabel;
    public TextView brightnessLabel;

    // List views, seekbars, and toolbar
    public net.qiujuer.genius.ui.widget.EditText renameTextEdit;
    public net.qiujuer.genius.ui.widget.SeekBar stepSeekBar;
    public ScrollView scrollView;
    public ScrollView setupScrollView;
    public Toolbar mainToolbar;
    public ListView mainListView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SwipeRefreshLayout setupSwipeRefresh;
    private DrawerLayout mainDrawer;
    private Switch onOffSwitch;
    private FloatingActionButton fab;
    public ImageView titleImageView;

    // Bluetooth variables
    private BroadcastReceiver_BTState mBTStateUpdateReceiver;
    private Scanner_BTLE mBLTLeScanner;
    private BluetoothGatt mainBleGatt;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        titleImageView = new ImageView(getApplicationContext());
        titleImageView.setImageResource(R.drawable.top_bar_title);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                cancelGroupsView();
            }
        });

        fab.setVisibility(View.INVISIBLE);

        deviceDb = new DeviceDatabase(this);
        listOfDevices = deviceDb.getAllDevices();

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

        displayFragment(R.id.nav_discover);
    }

    @Override
    public void discoveryVariables() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            } else {
                setDiscoverVariables();
            }
        }
    }

    public void setDiscoverVariables() {

        setupFlag = false;
        discoverFlag = true;

        if (findViewById(R.id.action_groups) != null) {
            findViewById(R.id.action_groups).setEnabled(true);
            findViewById(R.id.action_groups).setVisibility(View.VISIBLE);
            mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("Groups");
        }

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

        onOffSwitch = (Switch) findViewById(R.id.switch_on_off);
        onOffSwitch.setOnCheckedChangeListener(this);

        mainToolbar.addView(titleImageView);

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
        renameTextEdit = (net.qiujuer.genius.ui.widget.EditText) findViewById(R.id.renameText);

        mainToolbar.removeView(titleImageView);
        mainToolbar.setTitle("Setup");

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

                        findViewById(R.id.bg).setAlpha((float)0.0);

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

        mainToolbar.removeView(titleImageView);
        mainToolbar.setTitle("Help");

    }


    @Override
    public void disconnectFromDevices() {

        if (mainBleGatt == null) {
            return;
        } else {

            String buttonTitle = mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString();

            if (buttonTitle.equals("Groups")) {

                mBLTLeScanner.disconnectFromDevice(false, null, false);

                mainBleGatt = null;

            } else {

                mBLTLeScanner.disconnectFromDevice(true, null, true);

                int childCount = mainListView.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    mainListView.getChildAt(i).setBackgroundColor(Color.WHITE);
                }

                mainBleGatt = null;
            }

            if (setupFlag) {
                setup_bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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
                    setDiscoverVariables();
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

    /**
     * Life Cycle functions
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectFromDevices();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromDevices();
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

    public void cancelGroupsView() {
        mBLTLeScanner.disconnectFromDevice(true, null, false);
        mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("Groups");
        fab.setVisibility(View.INVISIBLE);

        int childCount = mainListView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            mainListView.getChildAt(i).setBackgroundColor(Color.WHITE);
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

        String buttonTitle = mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString();

        switch (v.getId()) {
            case R.id.disconnect_button:
                Utils.toast(getApplicationContext(), "Disconnecting..");

                if (buttonTitle.equals("Groups")) {

                    mBLTLeScanner.disconnectFromDevice(false, null, false);

                    mainBleGatt = null;

                } else {

                    mBLTLeScanner.disconnectFromDevice(true, null, false);
                }

                mainToolbar.getMenu().findItem(R.id.action_groups).setTitle("Groups");

                stepSeekBar.setOnSeekBarChangeListener(null);
                onOffSwitch.setOnCheckedChangeListener(null);

                stepSeekBar.setProgress(0);
                onOffSwitch.setChecked(false);
                brightnessLabel.setText("0");

                stepSeekBar.setOnSeekBarChangeListener(this);
                onOffSwitch.setOnCheckedChangeListener(this);

                int childCount = mainListView.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    mainListView.getChildAt(i).setBackgroundColor(Color.WHITE);
                }

                break;
            case R.id.setup_disconnect_button:
                Utils.toast(getApplicationContext(), "Disconnecting..");

                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(renameTextEdit.getWindowToken(), 0);

                // Update the name of the device
                mBLTLeScanner.updateDeviceName(renameTextEdit.getText().toString(), deviceDb);

                mBLTLeScanner.disconnectFromDevice(false, null, true);
                mainBleGatt = null;

                break;
            case R.id.lowest_button:
                mBLTLeScanner.writeCustomCharacteristic(202, false, deviceDb);
                break;
            case R.id.highest_button:
                mBLTLeScanner.writeCustomCharacteristic(201, false, deviceDb);
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
            if (setupFlag) {
                mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), false, deviceDb, true);
            } else {
                mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), false, deviceDb, false);
            }
        }
        // If you want to select multiple devices
        else if (buttonTitle.equals("Connect")) {
            // Add device to an arraylist

            ColorDrawable listItemColor = (ColorDrawable) parent.getChildAt(position).getBackground();

            if (listItemColor == null || listItemColor.getColor() == Color.WHITE) {

                // connect and change color
                mBLTLeScanner.connectToDevice(deviceItem.getBluetoothDevice(), true, deviceDb, false);

                parent.getChildAt(position).setBackgroundColor(Color.parseColor("#bee2f3"));

            } else {
                // disconnect and put transparent color

                mBLTLeScanner.disconnectFromDevice(true, deviceItem.getAddress(), false);
                parent.getChildAt(position).setBackgroundColor(Color.WHITE);
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
                fab.setVisibility(View.VISIBLE);

            } else { // Else if the title is 'Connect'

                if (mBLTLeScanner.groupOfDevices.size() > 0) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    fab.setVisibility(View.INVISIBLE);
                } else {

                    Utils.toast(getApplicationContext(), "Please select devices before connecting.");
                }
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
     * Also function for switch
     *
     * @param seekBar
     */
    @Override
    public void onProgressChanged(net.qiujuer.genius.ui.widget.SeekBar seekBar, int progress, boolean fromUser) {
        String buttonTitle = mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString();

        onOffSwitch.setOnCheckedChangeListener(null);
        if (progress > 0) {
            onOffSwitch.setChecked(true);
        } else {
            onOffSwitch.setChecked(false);
        }
        onOffSwitch.setOnCheckedChangeListener(this);

        try {

            //TODO: groups button rename
            if (buttonTitle.equals("Groups")) {
                mBLTLeScanner.writeCustomCharacteristic(progress * 10, false, deviceDb);
            } else {
                mBLTLeScanner.writeCustomCharacteristic(progress * 10, true, deviceDb);
            }
        } catch (Exception e) {
            Log.d("Exception: ", e.getLocalizedMessage());
        }
    }

    @Override
    public void onStartTrackingTouch(net.qiujuer.genius.ui.widget.SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(net.qiujuer.genius.ui.widget.SeekBar seekBar) {

    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
        String buttonTitle = mainToolbar.getMenu().findItem(R.id.action_groups).getTitle().toString();

        try {

            stepSeekBar.setOnSeekBarChangeListener(null);

            if (buttonTitle.equals("Groups")) {
                mBLTLeScanner.updateSwitchBrightness(false, deviceDb, isOn);
            } else {
                mBLTLeScanner.updateSwitchBrightness(true, deviceDb, isOn);
            }

            stepSeekBar.setOnSeekBarChangeListener(this);

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

        if (device.getName() == null || device.getName().length() == 0) {
            return;
        }
        if (!mBTDevicesHashMap.containsKey(device.getAddress())) {
            DeviceItem newDevice = new DeviceItem(device);

            System.out.println(newDevice);

            if (newDevice.getName() != null && newDevice.getName().contains("SmartDimmer")) {
                mBTDevicesHashMap.put(newDevice.getAddress(), newDevice);
                mBTDevicesArrayList.add(newDevice);

                boolean addNewFlag = true;
                for (DeviceObject existingObject : listOfDevices) {
                    if (existingObject.getMacAddress().equals(newDevice.getAddress())) {
                        addNewFlag = false;
                    }
                }

                if (addNewFlag) {
                    DeviceObject deviceObject = new DeviceObject(newDevice.getAddress(), newDevice.getName(), "0", "0");
                    listOfDevices.add(deviceObject);
                    deviceDb.addDevice(deviceObject);
                }
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

        if (mainListView.getChildCount() == 0) {
            Utils.toast(getApplicationContext(), "No devices found. Be sure you are in range or turn Bluetooth off, then on again.");
        }

        if (setupFlag) {
            setupSwipeRefresh.setRefreshing(false);
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
