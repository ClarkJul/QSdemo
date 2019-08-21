package com.nbc.quickstart.quicklaunch.management;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.nbc.quickstart.utils.DatabaseHelper;

public class QuickLaunchDatabaseHelper extends DatabaseHelper{

    public static final int ID_INDEX = 0;
    public static final int QUICKLAUNCHKEY_INDEX = 1;
    public static final int TYPE_INDEX = 2;
    public static final int DISPLAYNAME_INDEX = 3;
    public static final int ICONFILENAME_INDEX = 4;
    public static final int PACKAGENAME_INDEX = 5;
    public static final int STARTACTIVITY_INDEX = 6;
    public static final int CHOOSE_INDEX = 7;
    public static final int SEQUENCE_INDEX = 8;
    public static final int USERID_INDEX = 9;
    public static final int INSTALLED_INDEX = 10;
    public static final int DISPLAYICON_INDEX = 11;
    public static final int ACTION__INDEX = 12;

    public static final String ID_COLUMN = "_id";
    public static final String QUICKLAUNCHKEY_COLUMN = "quicklaunchkey";
    public static final String TYPE_COLUMN = "type";
    public static final String DISPLAYNAME_COLUMN = "displayname";
    public static final String ICONFILENAME_COLUMN = "iconfilename";
    public static final String PACKAGENAME_COLUMN = "packagename";
    public static final String STARTACTIVITY_COLUMN = "startactivity";
    public static final String CHOOSE_COLUMN = "choose";
    public static final String SEQUENCE_COLUMN = "sequence";
    public static final String USERID_COLUMN = "userid";
    public static final String INSTALLED_COLUMN = "installed";
    public static final String DISPLAYICON_COLUMN = "displayicon";
    public static final String ACTION_COLUMN = "action";


    public QuickLaunchDatabaseHelper(Context context) {
        super(context);
    }

    public Cursor getQuickLaunch(){
        String sql = "select * from " + QUICK_LAUNCH_TABLE_NAME + " order by sequence ASC;";
        return rawQuery(sql);
    }

    public Cursor getDefaultQuickLaunchByKey(String key) {
        if (TextUtils.isEmpty(key))
            return null;
        String sql = "select * from " + QUICK_LAUNCH_TABLE_NAME + " where " + QUICKLAUNCHKEY_COLUMN + "='" + key + "';";
        return rawQuery(sql);
    }

    public boolean clearDataInDb() {
        String sql = "delete from " + QUICK_LAUNCH_TABLE_NAME + ";";
        return execSQL(sql);
    }

    public long updateSingleQuickLaunch(ContentValues values, String whereClause, String[] whereArgs) {
        return update(DatabaseHelper.QUICK_LAUNCH_TABLE_NAME, values, whereClause, whereArgs);
    }
    public void updateQuickLaunchBatch(ContentValues[] values) {
        beginTransaction();
        try {
            for (ContentValues val : values) {
                if (val == null)
                    continue;
                String where = "_id = ?";
                String id = val.getAsString(ID_COLUMN);
                String[] arg = {id};
                update(DatabaseHelper.QUICK_LAUNCH_TABLE_NAME, val, where, arg);
            }
            setTransactionSuccessful();
        }finally {
            endTransaction();
        }
    }

    public long insertQuickLaunchToDb(ContentValues values) {
        return insert(DatabaseHelper.QUICK_LAUNCH_TABLE_NAME, values);
    }

    public boolean deleteAppNotChooseInDB(String ids) {
        if (TextUtils.isEmpty(ids))
            return false;
        String sql = "delete from " + QUICK_LAUNCH_TABLE_NAME
                + " where " + TYPE_COLUMN + "=" + QuickLaunchEntity.QUICK_LAUNCH_APP
                + " and _id in (" + ids + ");";
        return execSQL(sql);
    }

    public boolean deleteAppNotChooseInDB() {
        String sql = "delete from " + QUICK_LAUNCH_TABLE_NAME
                + " where " + TYPE_COLUMN + "=" + QuickLaunchEntity.QUICK_LAUNCH_APP
                + " and choose=0;";
        return execSQL(sql);
    }

    public Cursor getQuickLaunchFunctions(String packageName, int userId) {
        if (packageName == null)
            return null;
        String sql = "select * from " + QUICK_LAUNCH_TABLE_NAME
                + " where packagename='" + packageName + "' "
                + " and userid=" + userId
                + " and type=0 "
                + ";";
        return rawQuery(sql);
    }

}
