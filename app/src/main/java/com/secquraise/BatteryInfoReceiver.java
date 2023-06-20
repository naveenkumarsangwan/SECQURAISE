package com.secquraise;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class BatteryInfoReceiver extends BroadcastReceiver {
    private BatteryInfoListener listener;

    public BatteryInfoReceiver(BatteryInfoListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int batteryLevel = (level * 100) / scale;

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String chargingStatus;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            chargingStatus = "ON";
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            chargingStatus = "OFF";
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            chargingStatus = "OFF";
        } else {
            chargingStatus = "OFF";
        }

        if (listener != null) {
            // Format the battery percentage without decimal point and "%"
            String batteryPercentageString = String.valueOf(batteryLevel) + "%";

            listener.onBatteryInfoReceived(batteryPercentageString,chargingStatus);
        }
    }

    public interface BatteryInfoListener {
        void onBatteryInfoReceived(String batteryPercentage, String chargingStatus);
    }
}
