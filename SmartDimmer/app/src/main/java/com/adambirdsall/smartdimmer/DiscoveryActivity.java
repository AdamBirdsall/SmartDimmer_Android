package com.adambirdsall.smartdimmer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class DiscoveryActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;

    TextView connectedLabel, brightnessLabel;
    SeekBar mainSlider;
    Button minusButton, plusButton;
    Switch onOffSwitch;
    ListView mainList;

    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID DISCOVER_UUID = UUID.fromString("00001523-1212-EFDE-1523-785FEABCD123");
    static final UUID WRITE_CHARACTERISTIC = UUID.fromString("00001525-1212-EFDE-1523-785FEABCD123");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_discovery);

        mainList = (ListView) findViewById(R.id.mainList);
        connectedLabel = (TextView) findViewById(R.id.connectedLabel);
        brightnessLabel = (TextView) findViewById(R.id.brightnessLabel);
        mainSlider = (SeekBar) findViewById(R.id.mainSlider);
        minusButton = (Button) findViewById(R.id.minusButton);
        plusButton = (Button) findViewById(R.id.plusButton);
        onOffSwitch = (Switch) findViewById(R.id.onOffSwitch);

        mainSlider.setMax(100);
        mainSlider.setMin(0);
        mainSlider.setProgress(10);

        mainSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                writeBLEData(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        setBluetoothSettings();
    }

    private void setBluetoothSettings() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(mBluetoothAdapter == null) {
            //Show a mensag. that thedevice has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            //finish apk
            finish();
        } else {
            if (mBluetoothAdapter.isEnabled()) {

            } else {
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon,1);
            }
        }

        pairedDevicesList();
    }

    private void pairedDevicesList() {

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size() > 0) {

            for(BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }

        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        mainList.setAdapter(adapter);
        mainList.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick (AdapterView av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            // Make an intent to start next activity.
//            Intent i = new Intent(mainList.this, ledControl.class);
            //Change the activity.
//            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
//            startActivity(i);
        }
    };

    /**
     * Write the new data to the BLE device to change light brightness
     * @param value
     */
    private void writeBLEData(int value) {

    }
}
