package com.adambirdsall.smartdimmer.Utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.widget.Toast;

import com.adambirdsall.smartdimmer.Activities.DiscoveryActivity;

/**
 * Created by AdamBirdsall on 7/25/17.
 */

public class Utils {

    public static void toast(Context activityContext, String message) {
        Toast toast = Toast.makeText(activityContext, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER | Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    public static boolean checkBluetooth(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        } else {
            return true;
        }
    }

    public static void requestUserBluetooth(Activity activity) {
        Intent enableBtBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtBluetooth, DiscoveryActivity.REQUEST_ENABLE_BT);
    }
}
