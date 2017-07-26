package com.adambirdsall.smartdimmer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by AdamBirdsall on 7/25/17.
 */

public class ListAdapter_BTLE_Devices extends ArrayAdapter<DeviceItem> {

    Activity activity;
    int layoutResourceID;
    ArrayList<DeviceItem> devices;

    public ListAdapter_BTLE_Devices(Activity activity, int resource, ArrayList<DeviceItem> objects) {
        super(activity.getApplicationContext(), resource, objects);

        this.activity = activity;
        layoutResourceID = resource;
        devices = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layoutResourceID, parent, false);
        }

        DeviceItem deviceItem = devices.get(position);
        String name = deviceItem.getName();
        String address = deviceItem.getAddress();
        int rssi = deviceItem.getRssi();

        TextView tv_name = (TextView) convertView.findViewById(R.id.tv_name);
        if (name != null && name.length() > 0) {
            tv_name.setText(deviceItem.getName());
        } else {
            tv_name.setText("No Name");
        }

        TextView tv_rssi = (TextView) convertView.findViewById(R.id.tv_rssi);
        tv_rssi.setText("RSSI: " + Integer.toString(rssi));

        TextView tv_macAddress = (TextView) convertView.findViewById(R.id.tv_macaddr);
        if (address != null && address.length() > 0) {
            tv_macAddress.setText(deviceItem.getAddress());
        } else {
            tv_macAddress.setText("No Address");
        }

        return convertView;
    }
}
