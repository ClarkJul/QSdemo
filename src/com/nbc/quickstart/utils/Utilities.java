package com.nbc.quickstart.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.nbc.quickstart.core.Config;

public class Utilities {

    private static final String TAG = Config.TAG + Utilities.class.getSimpleName();

    private static final String SHARED_PREFERENCES_KEY = "smartcard_prefs";
    private static final String IS_SMARTCARD_LAUNCHED = "is_smartcard_launched";
    private static final String IS_QUICKSTART_LAUNCHED = "is_quickstart_launched";


    public static SharedPreferences getPrefs(Context context) {
        if (context != null)
            return context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        else
            return null;
    }

    public static boolean isSmartCardLaunched(Context context) {
        SharedPreferences prefs = getPrefs(context);
        boolean launched = false;
        if (prefs != null) {
            launched = prefs.getBoolean(IS_SMARTCARD_LAUNCHED, false);
            if (!launched)
                prefs.edit().putBoolean(IS_SMARTCARD_LAUNCHED, true).apply();
        }
        Log.i(TAG, "isSmartCardLaunched " + launched);
        return launched;
    }

    public static boolean isQuickStartLaunched(Context context) {
        SharedPreferences prefs = getPrefs(context);
        boolean launched = false;
        if (prefs != null) {
            launched = prefs.getBoolean(IS_QUICKSTART_LAUNCHED, false);
            if (!launched)
                prefs.edit().putBoolean(IS_QUICKSTART_LAUNCHED, true).apply();
        }
        Log.i(TAG, "isQuickStartLaunched " + launched);
        return launched;
    }

}
