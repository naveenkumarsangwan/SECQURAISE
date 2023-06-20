package com.secquraise;
import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;

public class MobileDataHelper {

    private static final String TAG = "MobileDataHelper";

    public static void setMobileDataEnabled(Context context, boolean enabled) {
        try {
            // Get the connectivity manager
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            // Get the class of the underlying connectivity manager
            Class<?> connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());

            // Get the method to set mobile data state
            Method setMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", boolean.class);

            // Make the method accessible
            setMobileDataEnabledMethod.setAccessible(true);

            // Invoke the method to set mobile data state
            setMobileDataEnabledMethod.invoke(connectivityManager, enabled);
        } catch (Exception e) {
            Log.e(TAG, "Error toggling mobile data: " + e.getMessage());
        }
    }

    public static boolean isMobileDataEnabled(Context context) {
        try {
            // Get the telephony manager
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the method to check mobile data state
            Method getDataEnabledMethod = telephonyManager.getClass().getDeclaredMethod("getDataEnabled");

            // Invoke the method to check mobile data state
            return (boolean) getDataEnabledMethod.invoke(telephonyManager);
        } catch (Exception e) {
            Log.e(TAG, "Error checking mobile data state: " + e.getMessage());
        }

        return false;
    }
}
