package com.nbc.quickstart.quicklaunch.management;


import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.Gravity;

import com.nbc.quickstart.R;
import com.nbc.quickstart.core.Config;

import java.io.ByteArrayOutputStream;

public class QuickLaunchEntity {
    public static final String TAG = Config.TAG + QuickLaunchEntity.class.getSimpleName();
    public static final String EXTRAS_KEY_QUICKLAUNCHKEY = "quick_launch_entity_quicklaunchkey";
    public static final String EXTRAS_KEY_PACKAGE = "quick_launch_entity_package";
    public static final String EXTRAS_KEY_ACTIVITY = "quick_launch_entity_activity";
    public static final String EXTRAS_KEY_TYPE = "quick_launch_entity_type";
    public static final String EXTRAS_KEY_ACTION = "quick_launch_entity_action";
    public static final String EXTRAS_KEY_USERID = "quick_launch_entity_userid";

    public static final int QUICK_LAUNCH_FUNCTION = 0;
    public static final int QUICK_LAUNCH_APP = 1;

    public int id;
    public String quickLaunchKey;
    public int type = QUICK_LAUNCH_FUNCTION;
    public String displayName;
    public String iconFileName;
    public Drawable displayIcon;
    public byte[] displayIconByte;
    public String packageName;
    public String startActivity;
    public boolean choosenStatus = false;
    public int sequence;

    public Context mContext;
    public ResolveInfo activityInfo;
    public int userId;
    public boolean installed = true;
    public String action;

    public Rect mRect = new Rect();

    public QuickLaunchEntity(Context context) {
        mContext = context.getApplicationContext();
    }

    public String getDisplayName() {
        if (type == QUICK_LAUNCH_FUNCTION) {
            int resId = getResId(quickLaunchKey, "string");
            if (resId != 0) {
                displayName = mContext.getString(resId);
                return displayName;
            }
        } else if (type == QUICK_LAUNCH_APP) {
            if (activityInfo != null) {
                return (String) activityInfo.loadLabel(mContext.getPackageManager());
            }
            return displayName;
        }
        return null;
    }

    public Drawable getDisplayIcon() {
        if (type == QUICK_LAUNCH_FUNCTION) {
            int resId = getResId(iconFileName, "drawable");
            if (resId != 0) {
                return mContext.getDrawable(resId);
            } else if (displayIcon != null) {
                return displayIcon;
            }
        } else if (type == QUICK_LAUNCH_APP) {
            if (displayIcon != null)
                return displayIcon;
            if (activityInfo != null) {
                return activityInfo.loadIcon(mContext.getPackageManager());
            }
        }
        return null;
    }

    public int getIconIdById() {
        if (type == QUICK_LAUNCH_FUNCTION) {
            return getResId(iconFileName, "drawable");
        }
        return 0;
    }

    private int getResId(String label, String resType) {
        if (mContext != null && label != null) {
            int resId = mContext.getResources().getIdentifier(label, resType, mContext.getPackageName());
            return resId;
        } else {
            Log.e(TAG, "getDisplayName mContext is null !!!");
            return 0;
        }
    }

    public ContentValues convertToContentValues() {
        ContentValues values = new ContentValues();
        values.put(QuickLaunchDatabaseHelper.QUICKLAUNCHKEY_COLUMN, quickLaunchKey);
        values.put(QuickLaunchDatabaseHelper.TYPE_COLUMN, type);
        values.put(QuickLaunchDatabaseHelper.DISPLAYNAME_COLUMN, displayName);
        values.put(QuickLaunchDatabaseHelper.ICONFILENAME_COLUMN, iconFileName);
        values.put(QuickLaunchDatabaseHelper.PACKAGENAME_COLUMN, packageName);
        values.put(QuickLaunchDatabaseHelper.STARTACTIVITY_COLUMN, startActivity);
        values.put(QuickLaunchDatabaseHelper.CHOOSE_COLUMN, choosenStatus ? 1:0);
        values.put(QuickLaunchDatabaseHelper.SEQUENCE_COLUMN, sequence);
        values.put(QuickLaunchDatabaseHelper.USERID_COLUMN, userId);
        values.put(QuickLaunchDatabaseHelper.INSTALLED_COLUMN, installed ? 1:0);
        values.put(QuickLaunchDatabaseHelper.ACTION_COLUMN, action);
        if (displayIconByte != null) {
            values.put(QuickLaunchDatabaseHelper.DISPLAYICON_COLUMN, displayIconByte);
        }
        return values;
    }

    public void generateFromCursor(Cursor cursor){
        try {
            id = cursor.getInt(QuickLaunchDatabaseHelper.ID_INDEX);
            quickLaunchKey = cursor.getString(QuickLaunchDatabaseHelper.QUICKLAUNCHKEY_INDEX);
            type = cursor.getInt(QuickLaunchDatabaseHelper.TYPE_INDEX);
            displayName = cursor.getString(QuickLaunchDatabaseHelper.DISPLAYNAME_INDEX);
            iconFileName = cursor.getString(QuickLaunchDatabaseHelper.ICONFILENAME_INDEX);
            packageName = cursor.getString(QuickLaunchDatabaseHelper.PACKAGENAME_INDEX);
            startActivity = cursor.getString(QuickLaunchDatabaseHelper.STARTACTIVITY_INDEX);
            choosenStatus = cursor.getInt(QuickLaunchDatabaseHelper.CHOOSE_INDEX) == 1;
            sequence = cursor.getInt(QuickLaunchDatabaseHelper.SEQUENCE_INDEX);
            userId = cursor.getInt(QuickLaunchDatabaseHelper.USERID_INDEX);
            installed = cursor.getInt(QuickLaunchDatabaseHelper.INSTALLED_INDEX) == 1;
            action = cursor.getString(QuickLaunchDatabaseHelper.ACTION__INDEX);
            byte[] bytes = cursor.getBlob(QuickLaunchDatabaseHelper.DISPLAYICON_INDEX);
            if (bytes != null) {
                Bitmap appIcon = scaleImage(bytes);
                displayIconByte = bytes;
                displayIcon = new BitmapDrawable(mContext.getResources(), appIcon);
            }
        } catch (Exception e) {
            Log.i(TAG, "generateFromCursor fail!!", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QuickLaunchEntity:{");
        sb.append("id:"+id);
        sb.append(", quickLaunchKey:" + quickLaunchKey);
        sb.append(", type:" + type);
        sb.append(", displayName:" + displayName);
        sb.append(", iconFileName:" + iconFileName);
        sb.append(", packageName:" + packageName);
        sb.append(", startActivity:" + startActivity);
        sb.append(", choosenStatus:" + choosenStatus);
        sb.append(", sequence:" + sequence);
        sb.append(", userId:" + userId);
        sb.append(", action:" + action);
        return sb.toString();
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private byte[] bitmapToByte(Drawable drawable) {
        if (drawable == null)
            return null;
        Bitmap bm  = drawableToBitmap(drawable);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public Bitmap scaleImage(byte[] bytes) {
        Bitmap appIcon = null;
        try {
            appIcon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.i(TAG, "scaleImage fail!", e);
        }

        return appIcon;
    }

}
