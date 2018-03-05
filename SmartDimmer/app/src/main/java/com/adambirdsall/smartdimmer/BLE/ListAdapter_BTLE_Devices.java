package com.adambirdsall.smartdimmer.BLE;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.adambirdsall.smartdimmer.R;
import com.adambirdsall.smartdimmer.Utils.DeviceDatabase;
import com.adambirdsall.smartdimmer.Utils.DeviceObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AdamBirdsall on 7/25/17.
 * @author AdamBirdsall
 */

public class ListAdapter_BTLE_Devices extends ArrayAdapter<SortedDeviceObject> {

    Activity activity;
    int layoutResourceID;
    ArrayList<SortedDeviceObject> devices;
    private boolean isSetupView;

    private DeviceDatabase deviceDb;

    public ListAdapter_BTLE_Devices(Activity activity, int resource, ArrayList<SortedDeviceObject> objects, boolean isSetupView) {
        super(activity.getApplicationContext(), resource, objects);

        this.activity = activity;
        this.isSetupView = isSetupView;
        layoutResourceID = resource;
        devices = objects;
        this.deviceDb = new DeviceDatabase(activity.getApplicationContext());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layoutResourceID, parent, false);
        }

        SortedDeviceObject deviceItem = devices.get(position);
        String name = deviceItem.getDeviceName();
        String address = deviceItem.getDeviceUuid();

        List<DeviceObject> deviceObjectList = deviceDb.getAllDevices();

        TextView tv_name = (TextView) convertView.findViewById(R.id.tv_name);
        if (name != null && name.length() > 0) {

            boolean nameExistsFlag = false;
            for (DeviceObject existingObject : deviceObjectList) {
                if (existingObject.getMacAddress().equals(deviceItem.getDeviceUuid())) {
                    tv_name.setText(existingObject.getDeviceName());
                    nameExistsFlag = true;
                }
            }

            if (!nameExistsFlag) {
                tv_name.setText(deviceItem.getDeviceName());
            }
        } else {
            tv_name.setText("No Name");
        }

        TextView tv_macAddress = (TextView) convertView.findViewById(R.id.tv_macaddr);
        if (isSetupView) {
            if (address != null && address.length() > 0) {
                tv_macAddress.setText(deviceItem.getDeviceUuid());
            } else {
                tv_macAddress.setText("No Address");
            }
        }

        return convertView;
    }
}
