package com.nbc.quickstart.utils;


import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

public class Utils {

    public static String getSceneId(String sceneId) {
        String sId = null;
        if (!TextUtils.isEmpty(sceneId) && sceneId.length() >= 5) {
            sId = sceneId.substring(0, 5);
        }
        return sId;
    }

    public static String formatYearMonthDate(Context context, long when) {
        if (when == 0) {
            return "";
        }
        int format_flags = DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_YEAR
                | DateUtils.FORMAT_SHOW_WEEKDAY;
        return DateUtils.formatDateTime(context, when, format_flags);
    }
    public static String formatDate(Context context, long when) {
        if (when == 0) {
            return "";
        }
        int format_flags = DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_WEEKDAY;
        return DateUtils.formatDateTime(context, when, format_flags);
    }

    public static String formatDateTime(Context context, long when) {
        if (when == 0) {
            return "";
        }
        int format_flags = DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_WEEKDAY;
        return DateUtils.formatDateTime(context, when, format_flags);
    }

    public static String formatTime(Context context, long when) {
        if (when == 0) {
            return "";
        }
        int format_flags = DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_TIME;
        return DateUtils.formatDateTime(context, when, format_flags);
    }

    public static float getActionBarHeight(Context context) {
        TypedArray actionbarSizeTypedArray = context.obtainStyledAttributes(new int[] {
                android.R.attr.actionBarSize
        });

        float actionBarHeight = actionbarSizeTypedArray.getDimension(0, 0);
        actionbarSizeTypedArray.recycle();
        return actionBarHeight;
    }

    /**
     * check the tips is agree
     * 20180424 tom
     * */
    public static boolean getHasLauncherAgree(Context context) {
        int agree = 0;
        try {
            if (null != context) {
                ContentResolver resolver = context.getContentResolver();
                agree = Settings.System.getInt(resolver, "fih_launcher_permission_confirm");
                Log.i("---", "getHasLauncherAgree : " + agree);
            }
        } catch (Settings.SettingNotFoundException e) {
            //e.printStackTrace();
            Log.i("---", "getHasLauncherAgree SettingNotFoundException: ");
            agree = 3;//Prevent Launcher version is too low to use smartlife tom 20180424
        } catch (RuntimeException e) {
            //e.toString();
            Log.i("---", "getHasLauncherAgree RuntimeException");
        }
        if (agree == 1 || agree == 3) {
            return true;
        } else {
            return false;
        }
    }
}
