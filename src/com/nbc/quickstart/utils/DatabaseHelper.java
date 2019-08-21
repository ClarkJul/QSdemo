package com.nbc.quickstart.utils;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.nbc.quickstart.core.Config;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = Config.TAG + DatabaseHelper.class.getSimpleName();
    private SQLiteDatabase mReadDb;
    private SQLiteDatabase mWriteDb;
    private Context _context;

    private static final String DB_NAME = "quick_start.db";
    private static final int VERSION = 1;
    public static final String QUICK_LAUNCH_TABLE_NAME = "quick_launch_shorts";

    private static final String CREATE_QUICK_LAUNCH_TABLE = "CREATE TABLE IF NOT EXISTS " + QUICK_LAUNCH_TABLE_NAME + "("
            + "_id INTEGER PRIMARY KEY,"
            + "quicklaunchkey TEXT,"
            + "type INTEGER,"
            + "displayname TEXT,"
            + "iconfilename TEXT,"
            + "packagename TEXT,"
            + "startactivity TEXT,"
            + "choose INTEGER,"
            + "sequence INTEGER,"
            + "userid INTEGER,"
            + "installed INTEGER,"
            + "displayicon BLOB,"
            + "action TEXT"
            + ");";

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                          int version) {
        super(context, name, factory, version);
        _context = context;

    }

    public DatabaseHelper(Context context) {
        this(context, DB_NAME, null, VERSION);
        mReadDb = this.getReadableDatabase();
        mWriteDb = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_QUICK_LAUNCH_TABLE);
    }

    public long insert(String table, ContentValues values){
        return mWriteDb.insert(table, null, values);
    }

    public long update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return mWriteDb.update(table, values, whereClause, whereArgs);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.out.println("db upgrade");
    }

    public Cursor rawQuery(String sql) {
        return mReadDb.rawQuery(sql, null);
    }

    public boolean execSQL(String sql) {
        try {
            mWriteDb.execSQL(sql);
        } catch (SQLException e) {
            Log.e(TAG, "execSQL(1) failed! sql=" + sql, e);
            return false;
        }
        return true;
    }

    public boolean execSQL(String sql, boolean Throw) {
        try {
            mWriteDb.execSQL(sql);
        } catch (SQLException e) {
            Log.e(TAG, "execSQL(2) failed! sql=" + sql, e);
            if (Throw)
                throw e;
            return false;
        }
        return true;
    }

    public boolean execSQL(String sql, Object[] object) {
        try {
            mWriteDb.execSQL(sql, object);
        } catch (SQLException e) {
            Log.e(TAG, "execSQL(2[]) failed! sql=" + sql, e);
            return false;
        }
        return true;
    }

    public void beginTransaction() {
        mWriteDb.beginTransaction();
    }

    public void setTransactionSuccessful() {
        mWriteDb.setTransactionSuccessful();
    }

    public void endTransaction() {
        mWriteDb.endTransaction();
    }

    public void close() {
        if (mReadDb != null)
            mReadDb.close();
        if (mWriteDb != null)
            mWriteDb.close();
    }
}
