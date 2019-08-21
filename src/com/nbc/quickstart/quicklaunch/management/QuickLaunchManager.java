package com.nbc.quickstart.quicklaunch.management;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.evenwell.DataCollect.DataCollect;
import com.evenwell.DataCollect.DataConfig;
import com.evenwell.DataCollect.DataConst;
import com.nbc.quickstart.R;
import com.nbc.quickstart.core.AbstractExecutor;
import com.nbc.quickstart.core.Config;
import com.nbc.quickstart.utils.AppInfoConst;
import com.nbc.quickstart.utils.CustomSortUtil;
import com.nbc.quickstart.utils.NamedTask;
import com.nbc.quickstart.utils.Utilities;

import org.greenrobot.eventbus.EventBus;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuickLaunchManager extends AbstractExecutor {
    private static final String TAG = Config.TAG + QuickLaunchManager.class.getSimpleName();
    private static int QUICK_LAUNCH_CHOOSE_MAX_SIZE = 8;
    private static int QUICK_LAUNCH_CHOOSE_LINE_COUNT = 4;
    private static int QUICK_LAUNCH_CHOOSE_MAX_SIZE_EDGE = 8;
    private static int QUICK_LAUNCH_CHOOSE_LINE_COUNT_EDGE = 4;

    private static final String XML_VERSION = "smart_card_quick_functions_xml_version";
    private static final String CARD_MORE_BUTTON_STATUS = "_more_button_expanded";
    private static final String APP_AUTHORITY = "content://com.android.launcher3.frequence";
    private static final String APP_URI = APP_AUTHORITY + "/search_suggest_query";
    private static final String COMPONENT_SEPARATOR = "/";
    private static final int FLASH_UI = 2;
    private static final int RELOAD_QUICK_START_VIEW_DATA = 3;

    private static QuickLaunchManager mQuickLaunchManager;
    private List<QuickLaunchEntity> mQuickLaunchChooseList = new ArrayList<>();
    private ArrayMap<String, QuickLaunchEntity> mQuickLaunchDBMap = new ArrayMap<>();   //如果选中，并且type为1，则加入该集合.
    private ArrayMap<String, QuickLaunchEntity> mLauncherAppsMap = new ArrayMap<>();
    private List<QuickLaunchEntity> mMoreFunctionList = new ArrayList<>();
    private List<QuickLaunchEntity> mMoreAppList = new ArrayList<>();
    private Context mContext;
    private QuickLaunchDatabaseHelper mDataHelper;
    private Handler mQuickHandler;
    private HandlerThread mQuickThread;
    private LauncherApps mLauncherApps;
    private LauncherApps.Callback mLauncherAppsCallback;
    private QuickLaunchManagementActivity mActivity;
    private Runnable mSaveDBRunnable;
    private boolean mHasNewVersion;
    private int mXmlId = R.xml.smart_card_quick_functions;
    private Runnable mStartWechat;
    private Intent mCurrentIntent;
    private ContentObserver mLauncherIconChangeObserver;

    private Toast mOpenQuickStartToast;
    private final Object mLock = new Object();
    private List<RecyclerView.Adapter> moreAppFunctionListObserver = new ArrayList<>();

    public int mTapArea;

    private QuickLaunchManager(Context context) {
        super(QuickLaunchManager.class.getSimpleName());
        mContext = context.getApplicationContext();
        mDataHelper = new QuickLaunchDatabaseHelper(context.getApplicationContext());

        int maxSize = mContext.getResources().getInteger(R.integer.quick_launch_choose_max);
        QUICK_LAUNCH_CHOOSE_MAX_SIZE = maxSize == 0 ? QUICK_LAUNCH_CHOOSE_MAX_SIZE : maxSize;
        Log.i(TAG, "The max size of quick launch is " + QUICK_LAUNCH_CHOOSE_MAX_SIZE);
        int lineCount = mContext.getResources().getInteger(R.integer.quick_launch_choose_line_count);
        QUICK_LAUNCH_CHOOSE_LINE_COUNT = lineCount == 0 ? QUICK_LAUNCH_CHOOSE_LINE_COUNT : lineCount;
        Log.i(TAG, "The line size of quick launch is " + QUICK_LAUNCH_CHOOSE_LINE_COUNT);

        if (mTapArea == 0) {
            mTapArea = mContext.getResources().getDimensionPixelSize(R.dimen.quick_launch_badge_icon_height);
        }

        mQuickThread = new HandlerThread("Quick-Launch-Thread");
        mQuickThread.start();
        mQuickHandler = new Handler(mQuickThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case FLASH_UI:
                        //刷新负一屏的widget.
                        Log.i(TAG, "flash minun one widget");
                        RefreshMessage refreshmsg = new RefreshMessage(mContext);
                        EventBus.getDefault().post(refreshmsg);
                        break;
                    case RELOAD_QUICK_START_VIEW_DATA:
                        initFunctionsInBackground();
                        break;
                }
            }
        };
        registerLauncherAppChangedListener();
        // SIUI@2018/8/8 zhengruixin add this for [start nokia mall small program]
//        WeChatUtils.registerApp(mContext);
    }

    public static QuickLaunchManager getInstance(Context context) {
        if (mQuickLaunchManager == null) {
            synchronized (QuickLaunchManager.class) {
                if (mQuickLaunchManager == null) {
                    mQuickLaunchManager = new QuickLaunchManager(context);
                }
            }
        }
        return mQuickLaunchManager;
    }

    public void init() {
        initFunctionsInBackground();
//        initMoreAppsInBackground();
    }

    public void initForCardDisplay() {
        initAppsFromLauncherInBackground();
        mHasNewVersion = isNewXmlVersion();
        if (!Utilities.isQuickStartLaunched(mContext) || mHasNewVersion) {
            initQuickLaunchFromXml();
        }

        Cursor cursor = mDataHelper.getQuickLaunch();
        initQuickLaunchDb(cursor);

        initMoreAppsInBackground();
    }

    public void initFunctionsInBackground() {
        NamedTask task = new NamedTask() {
            @Override
            public String getName() {
                return "initFunctionsInBackground";
            }
            @Override
            public void run() {
                mHasNewVersion = isNewXmlVersion();
                if (!Utilities.isQuickStartLaunched(mContext) || mHasNewVersion) {
                    initQuickLaunchFromXml();
                }
                Cursor cursor = mDataHelper.getQuickLaunch();
                if ((cursor != null && cursor.getCount() > 0)) {
                    initQuickLaunchDb(cursor);
                } else {
                    Log.e(TAG, "initFunctionsInBackground  cursor is empty!!!");
                }
            }
        };
        execute(task);
    }

    public void initMoreAppsInBackground() {
        NamedTask task = new NamedTask() {
            @Override
            public String getName() {
                return "initFunctionsInBackground";
            }
            @Override
            public void run() {
                initMoreAppList();
                flashUIByHandler();
            }
        };
        execute(task);
    }

    private void initQuickLaunchFromXml() {
        synchronized (mLock) {
            Log.i(TAG, "initQuickLaunchFromXml begin");
            if (moveUserdataFromLauncher()) {
                Log.i(TAG, "initQuickLaunchFromXml move userdata from launcher db success!!");
                return;
            }
            List<QuickLaunchEntity> quickLaunch = new ArrayList<>();
            try {
                XmlPullParser xpp = mContext.getResources().getXml(mXmlId);

                int eventType = xpp.getEventType();
                QuickLaunchEntity entity = null;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_DOCUMENT:

                            break;
                        case XmlPullParser.START_TAG:
                            String name = xpp.getName();
                            if (name.equals("entry")) {
                                entity = new QuickLaunchEntity(mContext);
                                entity.userId = android.os.Process.myUserHandle().getIdentifier();
                            } else if (name.equals("key") && entity != null) {
                                entity.quickLaunchKey = xpp.nextText();
                            } else if (name.equals("type") && entity != null) {
                                entity.type = (QuickLaunchEntity.QUICK_LAUNCH_FUNCTION + "").equals(xpp.nextText()) ? QuickLaunchEntity.QUICK_LAUNCH_FUNCTION : QuickLaunchEntity.QUICK_LAUNCH_APP;
                            } else if (name.equals("icon") && entity != null) {
                                entity.iconFileName = xpp.nextText();
                                entity.iconFileName = entity.iconFileName.replace(".png", "");
                            } else if (name.equals("package") && entity != null) {
                                entity.packageName = xpp.nextText();
                            } else if (name.equals("activity") && entity != null) {
                                entity.startActivity = xpp.nextText();
                            } else if (name.equals("choose") && entity != null) {
                                String boolValue = xpp.nextText();
                                entity.choosenStatus = Boolean.parseBoolean(boolValue != null ? boolValue : "false");
                            } else if (name.equals("action") && entity != null) {
                                entity.action = xpp.nextText();
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            String name_end = xpp.getName();
                            if (name_end.equals("entry") && entity != null) {
                                Log.d(TAG, "QuickLaunchEntity=" + entity);
                                if (!isAppInstalled(entity)) {
                                    entity.installed = false;
                                    entity.choosenStatus = false;
                                }
                                if (entity.type == QuickLaunchEntity.QUICK_LAUNCH_FUNCTION && TextUtils.isEmpty(entity.iconFileName)) {
                                    try {
                                        ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(entity.packageName, PackageManager.GET_META_DATA);
                                        entity.displayIcon = appInfo.loadIcon(mContext.getPackageManager());
                                    } catch (Exception e) {
                                        Log.e(TAG, "initQuickLaunchFromXml  get display icon failed!!", e);
                                    }
                                }
                                quickLaunch.add(entity);
                                entity = null;
                            }
                            break;
                    }
                    eventType = xpp.next();
                }

            } catch (Exception t) {
                Log.e(TAG, "custom xml-Parse R.xml.smart_card_quick_functions error!", t);
            }

//            List<QuickLaunchEntity> quickLaunchSort=quickLaunchSort(quickLaunch);
            Log.i(TAG, "initQuickLaunchFromXml mHasNewVersion=" + mHasNewVersion);
            if (mHasNewVersion) {
                insertOrUpdateDataToDB(quickLaunch);
//                insertOrUpdateDataToDB(quickLaunchSort);
            }
            Log.i(TAG, "initQuickLaunchFromXml end");
        }
    }

    /**
     * 按照需求指定的顺序给quicklaunch排序
     * @param quickLaunch
     * @return
     */
    private ArrayList<QuickLaunchEntity> quickLaunchSort(List<QuickLaunchEntity> quickLaunch) {
        ArrayList<QuickLaunchEntity> list = new ArrayList<>();
        QuickLaunchEntity[] qle = new QuickLaunchEntity[5];//需求数量更改时，改变数组长度
        if (!quickLaunch.isEmpty()) {
            int size=quickLaunch.size();
            for (int i = 0; i < size; i++) {
                if (quickLaunch.get(i).quickLaunchKey.equals("mop_sc_scanner")) {//全能扫
                    qle[0] = quickLaunch.get(i);
                    quickLaunch.remove(i);
                    i--;
                    size--;
                    continue;
                }
                if (quickLaunch.get(i).quickLaunchKey.equals("mop_sc_shot")) {//拍一拍
                    qle[1] = quickLaunch.get(i);
                    quickLaunch.remove(i);
                    i--;
                    size--;
                    continue;
                }
                if (quickLaunch.get(i).quickLaunchKey.equals("mop_sc_memo_new_note")) {//新增备忘录
                    qle[2] = quickLaunch.get(i);
                    quickLaunch.remove(i);
                    i--;
                    size--;
                    continue;
                }
                if (quickLaunch.get(i).quickLaunchKey.equals("mop_sc_global_data")) {//国际流量
                    qle[3] = quickLaunch.get(i);
                    quickLaunch.remove(i);
                    size--;
                    continue;
                }
                if (quickLaunch.get(i).quickLaunchKey.equals("mop_sc_calculator")) {//计算器
                    qle[4] = quickLaunch.get(i);
                    quickLaunch.remove(i);
                    i--;
                    size--;
                }
            }
            for (int j=0;j<qle.length;j++){
                if (qle[j]!=null){
                    list.add(qle[j]);
                }
            }
            list.addAll(quickLaunch);
            return list;
        }
        return null;
    }

    private void insertOrUpdateDataToDB(List<QuickLaunchEntity> quickLaunch) {
        Cursor cursor = mDataHelper.getQuickLaunch();
        try {
            if (cursor != null && cursor.getCount() > 0) {
                Log.i(TAG, "initQuickLaunchFromXml begin to update the data from XML!");
                Cursor cursor_temp = null;
                for (QuickLaunchEntity entity1 : quickLaunch) {
                    try {
                        cursor_temp = mDataHelper.getDefaultQuickLaunchByKey(entity1.quickLaunchKey);
                        if (cursor_temp == null || cursor_temp.getCount() == 0) {
                            entity1.choosenStatus = false;
                            ContentValues values = entity1.convertToContentValues();
                            values.put(QuickLaunchDatabaseHelper.SEQUENCE_COLUMN, quickLaunch.size() + 8);
                            mDataHelper.insertQuickLaunchToDb(values);
                        } else if (cursor_temp != null && cursor_temp.getCount() > 0){
                            ContentValues values = entity1.convertToContentValues();
                            cursor_temp.moveToFirst();
                            values.put(QuickLaunchDatabaseHelper.SEQUENCE_COLUMN, cursor_temp.getInt(QuickLaunchDatabaseHelper.SEQUENCE_INDEX));
                            values.put(QuickLaunchDatabaseHelper.CHOOSE_COLUMN, cursor_temp.getInt(QuickLaunchDatabaseHelper.CHOOSE_INDEX));
                            String where = QuickLaunchDatabaseHelper.QUICKLAUNCHKEY_COLUMN + "=?";
                            String[] whereArgs = {entity1.quickLaunchKey};
                            mDataHelper.updateSingleQuickLaunch(values, where, whereArgs);
                        }
                    }finally {
                        if (cursor_temp != null)
                            cursor_temp.close();
                    }
                }
            } else {
                Log.i(TAG, "initQuickLaunchFromXml there is no data in DB!!! Begin to update the data from XML!");
                ContentValues values;
                int i = 0;
                for (QuickLaunchEntity entity1 : quickLaunch) {
                    values = entity1.convertToContentValues();
                    values.put(QuickLaunchDatabaseHelper.SEQUENCE_COLUMN, i);
                    mDataHelper.insertQuickLaunchToDb(values);

                    i++;
                }
            }
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean moveUserdataFromLauncher() {
        boolean success = false;
        Cursor cursor = mDataHelper.getQuickLaunch();
        if (cursor != null && cursor.getCount() > 0) {
            Log.i(TAG, "There is data in quick start db");
            //There is data in SmartCard DB, don't need to move
            return success;
        }
        try {
            Uri uri = Uri.parse(Config.QUICK_LAUNCH_MINUS_URI);
            cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                Log.i(TAG, "origin db size:" + cursor.getCount());
                QuickLaunchEntity entity = null;
                List<QuickLaunchEntity> list = new ArrayList<>();
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    entity = new QuickLaunchEntity(mContext);
                    entity.generateFromCursor(cursor);
                    list.add(entity);

                    cursor.moveToNext();
                }

                ContentValues values = null;
                for (QuickLaunchEntity entity1 : list) {
                    values = entity1.convertToContentValues();
                    mDataHelper.insertQuickLaunchToDb(values);
                }
                success = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "moveUserdataFromLauncher error " + e);
        }

        if (cursor != null) {
            cursor.close();
        }
        return success;
    }

    private boolean isAppInstalled(QuickLaunchEntity entity) {
        if (entity == null || entity.packageName == null)
            return false;
        PackageManager pm = mContext.getPackageManager();
        ApplicationInfo info = null;
        try {
            info = pm.getApplicationInfoAsUser(entity.packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, entity.userId);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, entity.packageName + " is not installed!!   " + e);
        }
        return info != null;
    }

    private synchronized void initQuickLaunchDb(Cursor cursor) {
        if ((cursor != null && cursor.getCount() > 0)) {
            mQuickLaunchChooseList.clear();
            mMoreFunctionList.clear();
            mQuickLaunchDBMap.clear();

        } else {
            Log.e(TAG, "loadQuickLaunchDb  cursor is empty!!!");
            return;
        }
        loadQuickLaunchDb(cursor);
    }

    private synchronized void loadQuickLaunchDb(Cursor cursor) {
        try {
            QuickLaunchEntity entity;
            Log.i(TAG, "loadQuickLaunchDb cursor.count=" + cursor.getCount());

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                entity = new QuickLaunchEntity(mContext);
                entity.generateFromCursor(cursor);
                if (!isAppInstalled(entity)) {
                    cursor.moveToNext();
                    continue;
                } else {
                    entity.installed = true;
                }

                if (entity.choosenStatus) {
                    if (entity.type == QuickLaunchEntity.QUICK_LAUNCH_APP) {
                        mQuickLaunchDBMap.put(generateUniqueKey(entity), entity);

                        try {
                            String activityName = generateUniqueKey(entity);
                            QuickLaunchEntity temp = mLauncherAppsMap.get(activityName);
                            if (temp != null) {
                                entity.displayName = temp.displayName;
                                entity.displayIconByte = temp.displayIconByte;
                                entity.displayIcon = temp.displayIcon;
                            } else {
                                PackageManager pm = mContext.getPackageManager();
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName(entity.packageName, entity.startActivity));
                                ResolveInfo info = pm.resolveActivityAsUser(intent, PackageManager.GET_META_DATA, entity.userId);
                                if (info != null) {
                                    entity.displayName = (String) info.loadLabel(pm);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "loadQuickLaunchDb load app label failed!! packageName=" + entity.packageName, e);
                        }
                    }
                    mQuickLaunchChooseList.add(entity);
                    quickLaunchChooseListChanged();
                } else {
                    if (entity.type == QuickLaunchEntity.QUICK_LAUNCH_FUNCTION) {
                        mMoreFunctionList.add(entity);
//                        moreFunctionAndAppsListChanged();
                    }
                }

                cursor.moveToNext();
            }
            moreFunctionAndAppsListChanged();
        }catch (Exception e) {
            Log.e(TAG, "loadQuickLaunchDb error!!!", e);
        }finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public void initAppsFromLauncherInBackground() {
        NamedTask task = new NamedTask (){
            @Override
            public String getName() {
                return "initFunctionsInBackground";
            }
            @Override
            public void run() {
                initAppsFromLauncher();
                QuickLaunchEntity temp;
                for (QuickLaunchEntity entity : mQuickLaunchChooseList) {
                    if (entity == null)
                        continue;
                    String activityName = generateUniqueKey(entity);
                    temp = mLauncherAppsMap.get(activityName);
                    if (entity.type == QuickLaunchEntity.QUICK_LAUNCH_APP) {
                        if (temp != null) {
                            entity.displayIcon = temp.displayIcon;
                            entity.displayIconByte = temp.displayIconByte;
                            entity.displayName = temp.displayName;
                        } else {
                            for (QuickLaunchEntity app : mLauncherAppsMap.values()) {
                                if (entity.packageName.equals(app.packageName) && entity.userId == app.userId) {
                                    entity.displayIcon = app.displayIcon;
                                    entity.displayIconByte = app.displayIconByte;
                                    entity.displayName = app.displayName;
                                    entity.startActivity = app.startActivity;
                                    break;
                                }
                            }
                        }
                    }
                    flashUIByHandler();
                }
            }
        };
        execute(task);
    }

    public void initAppsFromLauncher() {
        Uri app_uri = Uri.parse(APP_URI);
        Cursor cursor_result = null;
        try{
            //从Launcher return的list已经是按照当前语言排好序
            cursor_result = mContext.getContentResolver().query(app_uri, null, "", null, null);
            if (cursor_result != null) {
                int count = cursor_result.getCount();
                Log.i(TAG, "initAppsFromLauncher  data size=" + count);
                synchronized (mLauncherAppsMap) {
                    if (count != 0) {
                        mLauncherAppsMap.clear();
                        QuickLaunchEntity entity = null;
                        cursor_result.moveToFirst();
                        while (!cursor_result.isAfterLast()) {
                            ComponentName componentName = parseComponentNameFromStr(cursor_result.getString(cursor_result.getColumnIndex("component")));
                            int userId = cursor_result.getInt(cursor_result.getColumnIndex("user"));

                            entity = new QuickLaunchEntity(mContext);
                            entity.displayName = cursor_result.getString(cursor_result.getColumnIndex("title"));
                            byte[] bytes = cursor_result.getBlob(cursor_result.getColumnIndex("icon"));
                            Bitmap appIcon;
                            if (bytes != null) {
                                appIcon = entity.scaleImage(bytes);
                                entity.displayIconByte = bytes;
                                entity.displayIcon = new BitmapDrawable(mContext.getResources(), appIcon);
                            }
                            if (componentName != null) {
                                entity.packageName = componentName.getPackageName();
                                entity.startActivity = componentName.getClassName();
                            }
                            entity.userId = userId;
                            entity.type = QuickLaunchEntity.QUICK_LAUNCH_APP;

                            String activityName = "";
                            if (componentName != null) {
                                activityName = generateUniqueKey(entity);
                            }
                            if (addLauncherAppMap(entity)) {
                                mLauncherAppsMap.put(activityName, entity);
                                Log.d("huangxiaofeng", "displayName:" + entity.displayName);
                            }
                            cursor_result.moveToNext();
                        }
                    }
                }
            } else {
                Log.e(TAG, "initAppsFromLauncher Load data from Launcher failed!!!!!!");
            }
        } catch (Exception e) {
            Log.e(TAG, "initAppsFromLauncher get App List from Launcher failed!!", e);
        } finally {
            if (cursor_result != null)
                cursor_result.close();
        }
        if (mLauncherIconChangeObserver == null) {
            mLauncherIconChangeObserver = new ContentObserver(mQuickHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    initAppsFromLauncher();
                    updateQuickStartIcons();
                }
            };
            mContext.getContentResolver().registerContentObserver(Uri.parse(APP_AUTHORITY), false, mLauncherIconChangeObserver);
        }
    }

    private boolean addLauncherAppMap(QuickLaunchEntity entity) {
        if (null != entity) {
            if (entity.packageName.equals("com.nbc.smartshot") || entity.packageName.equals("com.android.calculator2")||entity.packageName.equals("com.redteamobile.global.roaming")) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private synchronized void initMoreAppList() {
        if (mLauncherAppsMap.size() > 0) {
            mMoreAppList.clear();
//            moreFunctionAndAppsListChanged();
        }
        QuickLaunchEntity temp = null;
        for (QuickLaunchEntity entity : mLauncherAppsMap.values()) {
            if (entity == null)
                continue;
            String activityName = generateUniqueKey(entity);
            if (mQuickLaunchDBMap.size() == 0) {
                for (QuickLaunchEntity choose : getQuickLaunchChooseList()) {
                    String chooseActName = generateUniqueKey(choose);
                    if (choose.type == QuickLaunchEntity.QUICK_LAUNCH_APP && activityName.equals(chooseActName)) {
                        temp = choose;
                        break;
                    }
                }
            } else {
                temp = mQuickLaunchDBMap.get(activityName);
            }
            if (temp == null) {
                mMoreAppList.add(entity);
//                moreFunctionAndAppsListChanged();
            }
            temp = null;
        }
        if (mMoreAppList.size() > 1) {
            CustomSortUtil.sort(mMoreAppList);
        }
        moreFunctionAndAppsListChanged();
    }

    private void updateQuickStartIcons() {
        if (mLauncherAppsMap.isEmpty()) {
            return;
        }
        QuickLaunchEntity temp;
        boolean changed = false;
        for (QuickLaunchEntity entity : mQuickLaunchChooseList) {
            if (entity.type != QuickLaunchEntity.QUICK_LAUNCH_APP) {
                continue;
            }
            String activityName = generateUniqueKey(entity);
            temp = mLauncherAppsMap.get(activityName);
            if (temp != null) {
                if (!entity.displayName.equals(temp.displayName) ||
                        !Arrays.equals(entity.displayIconByte, temp.displayIconByte) ||
                        !entity.startActivity.equals(temp.startActivity)) {
                    entity.displayName = temp.displayName;
                    entity.displayIconByte = temp.displayIconByte;
                    entity.displayIcon = temp.displayIcon;
                    entity.startActivity = temp.startActivity;
                    updateQuickLaunchStatusToDB(entity);
                    changed = true;
                }
            }
        }
        if (changed) {
            flashUIByHandler();
        }
    }

    public boolean canClick(View v, MotionEvent event) {
        boolean result = false;
        float clickX = event.getRawX();
        float clickY = event.getRawY();
        int width = v.getWidth();
        int[] position = new int[2];
        v.getLocationInWindow(position);
        int left = position[0];
        int top = position[1];
        if (clickX > (left + width - mTapArea) && clickX < (left + width) && clickY > top && clickY < top + mTapArea) {
            result = true;
        }
        return result;
    }

    public void saveQuickLaunchListToDB() {
        if (mSaveDBRunnable == null) {
            mSaveDBRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "saveQuickLaunchListToDB start.....");
                    synchronized (mQuickLaunchChooseList) {
                        QuickLaunchEntity entity = null;
                        List<QuickLaunchEntity> saveList = new ArrayList<>();
                        saveList.addAll(mQuickLaunchChooseList);
                        ContentValues[] quick_content_values = new ContentValues[saveList.size()];
                        for (int i = 0; i < saveList.size(); i++) {
                            entity = saveList.get(i);
                            entity.choosenStatus = true;
                            ContentValues values = entity.convertToContentValues();
                            values.put(QuickLaunchDatabaseHelper.SEQUENCE_COLUMN, i);
                            if (entity.id == 0) {
                                values.remove(QuickLaunchDatabaseHelper.ID_COLUMN);
                                long id = mDataHelper.insertQuickLaunchToDb(values);
                                if (id != -1) {
                                    entity.id = (int) id;
                                }
                            } else {
                                values.put(QuickLaunchDatabaseHelper.ID_COLUMN, entity.id);
                                quick_content_values[i] = values;
                            }
                        }
                        mDataHelper.updateQuickLaunchBatch(quick_content_values);
                        Log.i(TAG, "saveQuickLaunchListToDB start..... save ChooseList completed!!!");
                        //如果没有数据QuickStart卡片就消失，有数据且卡片式enabled，QuickStart卡片就显示
//                        if (getWidgetProviderShowStatus()) {
//                            if (saveList.isEmpty()) {
//                                updateWidgetProviderStatus(false);
//                            }
//                        } else {
//                            if (!saveList.isEmpty()) {
//                                updateWidgetProviderStatus(true);
//                            }
//                        }

                        saveList.clear();
                        saveList.addAll(mMoreFunctionList);
                        quick_content_values = new ContentValues[saveList.size()];
                        for (int i = 0; i < saveList.size(); i++) {
                            entity = saveList.get(i);
                            entity.choosenStatus = false;
                            ContentValues values = entity.convertToContentValues();
                            values.put(QuickLaunchDatabaseHelper.SEQUENCE_COLUMN, i);

                            quick_content_values[i] = values;
                        }
                        mDataHelper.updateQuickLaunchBatch(quick_content_values);
                        mDataHelper.deleteAppNotChooseInDB();
                    }
                    Log.i(TAG, "saveQuickLaunchListToDB end.....");
                    flashUIByHandler();
                }
            };
        }
        mQuickHandler.removeCallbacks(mSaveDBRunnable);
        mQuickHandler.postDelayed(mSaveDBRunnable, 50);
    }

    public void updateQuickLaunchStatusToDB(final QuickLaunchEntity entity) {
        if (entity == null)
            return;
        mQuickHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "updateQuickLaunchStatusToDB entity=" + entity);
                if (entity.id != 0) {
                    ContentValues values = entity.convertToContentValues();
                    String where = "_id = ?";
                    String id = entity.id + "";
                    String[] arg = {id};
                    mDataHelper.updateSingleQuickLaunch(values, where, arg);
                }
            }
        });
    }

    private boolean isNewXmlVersion() {
        boolean isNew = false;
        String version = null;
        try {
            XmlPullParser xpp = mContext.getResources().getXml(mXmlId);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String name = xpp.getName();
                        if (name.equals("version")) {
                            version = xpp.nextText();
                        }
                        break;
                }
                if (version != null) {
                    break;
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "isNewXmlVersion error!", e);
        }

        SharedPreferences prefs = Utilities.getPrefs(mContext);

        if (prefs != null) {
            float oldVersion = prefs.getFloat(XML_VERSION, 0f);
            Log.i(TAG, "The version of R.xml.smart_card_quick_functions is " + version + " , the version in prefs is " + oldVersion);
            float newVersion = 0f;
            try {
                newVersion = Float.parseFloat(version);
            } catch (NumberFormatException e) {
                Log.e(TAG, "parseFloat error: version=" + version, e);
            }
            if (oldVersion == 0f) {
                isNew = true;
                prefs.edit().putFloat(XML_VERSION, 1.0f).apply();
            } else {
                if (newVersion != 0f && newVersion > oldVersion) {
                    isNew = true;
                    prefs.edit().putFloat(XML_VERSION, newVersion).apply();
                }
            }
            Log.i(TAG, "isNew=" + isNew);
        }
        return isNew;
    }

    private void registerLauncherAppChangedListener() {
        mLauncherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (mLauncherAppsCallback == null) {
            mLauncherAppsCallback = new LauncherApps.Callback() {
                @Override
                public void onPackageRemoved(String packageName, UserHandle user) {
                    Log.i(TAG, "onPackageRemoved " + packageName + " User=" + user.getIdentifier());
                    synchronized (mQuickLaunchChooseList) {
                        List<QuickLaunchEntity> temp = new ArrayList<>();
                        for (QuickLaunchEntity entity : mQuickLaunchChooseList) {
                            if (packageName.equals(entity.packageName) && entity.userId == user.getIdentifier()) {
                                if (QuickLaunchEntity.QUICK_LAUNCH_FUNCTION == entity.type) {
                                    entity.choosenStatus = false;
                                    entity.installed = false;
                                    ContentValues values = entity.convertToContentValues();
                                    String where = "_id = ?";
                                    String id = entity.id + "";
                                    String[] arg = {id};
                                    mDataHelper.updateSingleQuickLaunch(values, where, arg);
                                } else if (QuickLaunchEntity.QUICK_LAUNCH_APP == entity.type) {
                                    mDataHelper.deleteAppNotChooseInDB(entity.id + "");
                                }
                                temp.add(entity);
                            }
                        }
                        if (temp.size() > 0) {
                            mQuickLaunchChooseList.removeAll(temp);
//                            mMoreFunctionList.removeAll(temp);
                            temp.clear();
                        } else {
                            initMoreAppList();
                        }
                    }
                    //删除一个应用时，同时更新mMoreFunctionList集合.
                    synchronized (mMoreFunctionList) {
                        int size = mMoreFunctionList.size();
                        for (int i = 0; i < size; i++) {
                            if (mMoreFunctionList.get(i).packageName.equals(packageName)) {
                                QuickLaunchEntity entity = mMoreFunctionList.get(i);
                                Log.d("huangxiaofeng", "delete entity:" + entity.displayName);
                                mMoreFunctionList.remove(entity);
                                size -= 1;
                            }
                        }
                    }
                    //刷新负一屏的quickStart widget
                    flashUIByHandler();
                    //刷新列表
                    quickLaunchChooseListChanged();
                    moreFunctionAndAppsListChanged();
                }

                @Override
                public void onPackageAdded(String packageName, UserHandle user) {
                    Log.i(TAG, "onPackageAdded " + packageName + " User=" + user.getIdentifier());
                    Cursor cursor = mDataHelper.getQuickLaunchFunctions(packageName, user.getIdentifier());
//                    if (cursor != null && cursor.getCount() > 0) {
//                        initQuickLaunchDb(cursor);
//                    } else {
//                        initMoreAppList();
//                    }
                    //安装一个应用时，同时更新mMoreFunctionList集合.
                    updateMoreFunctionsList(cursor);
                    initMoreAppList();
                    flashUIByHandler();
                }

                @Override
                public void onPackageChanged(String packageName, UserHandle user) {
                }

                @Override
                public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
                }

                @Override
                public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
                }
            };
        }
        try {
            mLauncherApps.registerCallback(mLauncherAppsCallback, mQuickHandler);
        } catch (Exception e) {
            Log.e(TAG, "registerLauncherAppChangedListener failed", e);
        }
    }


    private synchronized void updateMoreFunctionsList(Cursor cursor) {
        try {
            QuickLaunchEntity entity;
            Log.i(TAG, "loadQuickLaunchDb cursor.count=" + cursor.getCount());

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                entity = new QuickLaunchEntity(mContext);
                entity.generateFromCursor(cursor);
                if (!isAppInstalled(entity)) {
                    cursor.moveToNext();
                    continue;
                } else {
                    entity.installed = true;
                }

                if (entity.type == QuickLaunchEntity.QUICK_LAUNCH_FUNCTION) {
                    mMoreFunctionList.add(entity);
//                    moreFunctionAndAppsListChanged();
                }
                cursor.moveToNext();
            }
            moreFunctionAndAppsListChanged();
        } catch (Exception e) {
            Log.e(TAG, "loadQuickLaunchDb error!!!", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private void unregisterLauncherAppChangedListener() {
        if (mLauncherApps != null && mLauncherAppsCallback != null) {
            try {
                mLauncherApps.unregisterCallback(mLauncherAppsCallback);
            } catch (Exception e) {
                Log.e(TAG, "unregisterLauncherAppChangedListener failed", e);
            }
        }
    }

    private void quickLaunchChooseListChanged() {
        RefreshManageUIMessage manageUIMessage = new RefreshManageUIMessage();
        manageUIMessage.chooseBool = true;
        EventBus.getDefault().post(manageUIMessage);
    }

    private void moreFunctionAndAppsListChanged() {
        if (!moreAppFunctionListObserver.isEmpty()) {
            for (RecyclerView.Adapter adapter : moreAppFunctionListObserver) {
                List<QuickLaunchEntity> list = new ArrayList<>();
                list.addAll(mMoreAppList);
                list.addAll(mMoreFunctionList);
                ((QuickLaunchManagementAdapter) adapter).setmoreAppItems(CustomSortUtil.sort(list));
                RefreshManageUIMessage manageUIMessage = new RefreshManageUIMessage();
                manageUIMessage.moreBool = true;
                EventBus.getDefault().post(manageUIMessage);
            }
        }
    }

    public void addMoreFunctionAndAppsListObserver(RecyclerView.Adapter adapter) {
        moreAppFunctionListObserver.add(adapter);
    }

    public void removeMoreFunctionAndAppsListObserver() {
        moreAppFunctionListObserver.clear();
    }

    public List<QuickLaunchEntity> getQuickLaunchChooseList() {
        return mQuickLaunchChooseList;
    }

    public List<QuickLaunchEntity> getQuickStartAppList() {
        List<QuickLaunchEntity> list = new ArrayList<>();
        list.addAll(mMoreAppList);
        list.addAll(mMoreFunctionList);
        return list;
    }

    public int getQuickLaunchChooseListSize() {
        return mQuickLaunchChooseList.size();
    }

    public int getQuickLaunchChooseMaxSize() {
        return QUICK_LAUNCH_CHOOSE_MAX_SIZE;
    }

    public int getQuickLaunchChooseLineCount() {
        return QUICK_LAUNCH_CHOOSE_LINE_COUNT;
    }

    public boolean isQuickLaunchListFull() {
        return getQuickLaunchChooseListSize() < QUICK_LAUNCH_CHOOSE_MAX_SIZE;
    }

    public void setQuickLaunchManagementActivity(QuickLaunchManagementActivity activity) {
        mActivity = activity;
    }

    public boolean showMoreInQuickLaunchCardView() {
        return getQuickLaunchChooseListSize() > QUICK_LAUNCH_CHOOSE_LINE_COUNT;
    }

    /**
     * @param value
     * @return
     * @description Read ComponentName from a string
     */
    public static ComponentName parseComponentNameFromStr(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }

        if (!value.contains(COMPONENT_SEPARATOR)) {
            return null;
        }

        String[] subs = value.split(COMPONENT_SEPARATOR);
        if (subs == null || subs.length < 2) {
            return null;
        }

        return new ComponentName(subs[0], subs[1]);
    }

    private String generateUniqueKey(QuickLaunchEntity entity) {
        return entity.packageName + entity.startActivity + entity.userId;
    }

    public void startActivitySafely(final QuickLaunchEntity entity) {

        if (entity == null || TextUtils.isEmpty(entity.packageName)) {
            return;
        }

        AsyncTask task = new AsyncTask<Object, Void, Intent>() {
            @Override
            protected Intent doInBackground(Object... args) {
                sendDataCollection(entity);
                ComponentName componentName = new ComponentName(entity.packageName, entity.startActivity);
                // Only launch using the new animation if the shortcut has not opted out (this is a
                // private contract between launcher and may be ignored in the future).
                Intent intent = new Intent().setComponent(componentName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (entity.type == QuickLaunchEntity.QUICK_LAUNCH_APP) {
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                } else if (entity.type == QuickLaunchEntity.QUICK_LAUNCH_FUNCTION) {
                    if (TextUtils.isEmpty(entity.action)) {
                        intent.setAction(Intent.ACTION_VIEW);
                    } else {
                        intent.setAction(entity.action);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    }
                }
                if (componentName.getClassName().equals("com.alipay.mobile.scan.as.main.MainCaptureActivity")) {
                    //use Uri to start Alipay Scan
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.setComponent(null);
                    Uri uri = Uri.parse("alipays://platformapi/startapp?appId=10000007&sourceId=shortcut&source=shortcut");
                    intent.setData(uri);
                }
                if (entity.packageName.equals("com.nbc.quickstart")) {
                    intent.putExtra(Config.EXTRA_CARD_VIEW_ENABLED, true);
                }
                intent.putExtra("profile", entity.userId);
                intent.putExtra("com.fihnjdc.appcloner.had_choose", true);
                intent.putExtra("appcloner_from_package", mContext.getClass().getPackage().getName());
                intent.putExtra("appcloner_from_user", android.os.Process.myUserHandle().getIdentifier());
                intent.putExtra("appcloner_to_user", entity.userId);

                try {
                    //If application is not installed, will launch fail
                    ApplicationInfo info = mContext.getPackageManager().getApplicationInfoAsUser(entity.packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, entity.userId);
                    if (info == null) {
                        runInUIThreadHandler(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, R.string.mop_sc_fail_notification, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return null;
                    }
                    return intent;
                } catch (Exception e) {
                    Log.e(TAG, "Start quick launch activity failed doInBackground(): entity.startActivity" + componentName + " userId=" + entity.userId, e);
                    runInUIThreadHandler(new Runnable() {
                        @Override
                        public void run() {
                            if (mOpenQuickStartToast == null || mContext == null) {
                                mOpenQuickStartToast = Toast.makeText(mContext, R.string.mop_sc_fail_notification, Toast.LENGTH_SHORT);
                            }
                            mOpenQuickStartToast.setText(R.string.mop_sc_fail_notification);
                            mOpenQuickStartToast.show();
                        }
                    });
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Intent intent) {
                if (intent != null) {
                    try {
                        if (entity.packageName.equals("com.tencent.mm") && entity.type == QuickLaunchEntity.QUICK_LAUNCH_FUNCTION) {
                            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo("com.tencent.mm", 0);
                            int versionCode = packageInfo.versionCode;
                            //WeChat versionName=6.6.0 versionCode=1200 changed the start activity name
                            if (versionCode >= 1200 && "mop_sc_wechat_moments".equals(entity.quickLaunchKey)) {
                                ComponentName comp = intent.getComponent();
                                if (comp != null) {
                                    comp = new ComponentName(comp.getPackageName(), Config.WECHAT_6_6_MOMENT_START_ACTIVITY);
                                    intent.setComponent(comp);
                                }
                            }
                            mCurrentIntent = intent;
                            startWechatLauncherActivity(intent);

                            return;
                        }
                        mContext.startActivityAsUser(intent, null, new UserHandle(entity.userId));
                    } catch (Exception e) {
                        Log.e(TAG, "Start quick launch activity failed: entity.startActivity" + entity.startActivity + " userId=" + entity.userId, e);
                        Toast.makeText(mContext, R.string.mop_sc_fail_notification, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void startWechatLauncherActivity(Intent intent) {
        Log.i(TAG, "startWechatLauncherActivity begin");
        //1. Start the Wechat launcher activity first
        Intent temp_intent = (Intent) intent.clone();
        temp_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        temp_intent.setAction(Intent.ACTION_MAIN);
        temp_intent.addCategory(Intent.CATEGORY_LAUNCHER);
        temp_intent.setClassName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
        try {
            mContext.startActivity(temp_intent);
        } catch (Exception e) {
            Log.e(TAG, "Could not start activity!!!", e);
            return;
        }

        //2. Then check if Wechat is started success or not, if yes, start the quick starts for Wechat
        startWeChatQuickStarts();
    }

    private void startWeChatQuickStarts() {
        Log.i(TAG, "startWeChatQuickStarts");
        //Set the timeout to 10s, then exit the Runnable
        final int MAX_ROUND = 100;
        final ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (mStartWechat == null) {
            mStartWechat = new Runnable() {
                int round = 0;

                @Override
                public void run() {
                    if (round >= MAX_ROUND) {
                        Log.e(TAG, "Time out to start wechat quick start, intent=" + mCurrentIntent);
                        mStartWechat = null;
                        return;
                    }
                    boolean isWechatActive = false;

                    List<ActivityManager.RunningServiceInfo> serviceInfos = am.getRunningServices(1000);
                    Set<String> weChatServices = new HashSet<>();
                    for (ActivityManager.RunningServiceInfo service : serviceInfos) {
                        //If Wechat is started success, start the quick starts for WeChat
                        int userId = UserHandle.getUserId(service.uid);
                        if (service.process.equals("com.tencent.mm") && userId == android.os.Process.myUserHandle().getIdentifier()) {
                            weChatServices.add(service.service.getClassName());
                            Log.i(TAG, "weChatServices.size=" + weChatServices.size() + " class=" + service.service.getClassName());
                            if (weChatServices.size() >= 2 || service.service.getClassName().equals("com.tencent.mm.booter.NotifyReceiver$NotifyService")) {
                                if (mContext != null && !isOnWeChatLoginUI()) {
                                    Log.i(TAG, "startWeChatQuickStarts >>>>runWechatQuickStart");
                                    try {
                                        if (mContext != null && mCurrentIntent != null) {
                                            Log.i(TAG, "runWechatQuickStart >>>>intent=" + mCurrentIntent);
                                            mContext.startActivity(mCurrentIntent);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Could not start activity !!!!", e);
                                    }
                                } else {
                                    Log.i(TAG, "No conditions to started WeChat quick start, intent=" + mCurrentIntent);
                                }
                                isWechatActive = true;
                                mStartWechat = null;
                                return;
                            }
                        }
                    }
                    //If WeChat is not started, re-run the check
                    if (!isWechatActive && mStartWechat != null) {
                        mQuickHandler.postDelayed(mStartWechat, 100);
                    }
                    round++;
                }
            };
        }
        mQuickHandler.removeCallbacks(mStartWechat);
        mQuickHandler.postDelayed(mStartWechat, 100);
    }

    private boolean isOnWeChatLoginUI() {
        boolean isLoginUI = false;
        if (mContext != null) {
            ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(2);
            if (runningTasks != null && runningTasks.size() > 0) {
                ActivityManager.RunningTaskInfo runningTaskInfo = runningTasks.get(0);
                ComponentName topActivity = runningTaskInfo.topActivity;
                ComponentName mWechatLogin = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.account.LoginPasswordUI");
                if (topActivity != null && topActivity.equals(mWechatLogin)) {
                    Log.i(TAG, "topActivity is WeChat's LoginPasswordUI, return....");
                    isLoginUI = true;
                } else {
                    Log.i(TAG, "topActivity is null or topActivity is not equal WeChat LoginUI,  topActivity=" + topActivity);
                }
            }
        }
        return isLoginUI;
    }

    public void clearData() {
        //Don't clear the data of mQuickLaunchChooseList, since QuickLaunchCardViewAdapter will use it to show in the Card View
        //Don't clear the data of mMoreFunctionList
        mMoreAppList.clear();
        setQuickLaunchManagementActivity(null);
        if (mLauncherIconChangeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mLauncherIconChangeObserver);
        }
        close();
    }

    public ArrayMap<String, QuickLaunchEntity> getLauncherAppsMap() {
        return mLauncherAppsMap;
    }

    private void runInUIThreadHandler(Runnable r) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.removeCallbacks(r);
        handler.postDelayed(r, 100);
    }

    private void flashUIByHandler() {
        Log.d(TAG, "entry flashUIByHandler");
        if (mActivity == null || mActivity.mDataUpdated) {
            mQuickHandler.removeMessages(FLASH_UI);
            mQuickHandler.sendEmptyMessageDelayed(FLASH_UI, 150);
        }
    }

    public boolean getCardMoreButtonExpandStatus(String cardKey) {
        if (TextUtils.isEmpty(cardKey))
            return false;
        SharedPreferences prefs = Utilities.getPrefs(mContext);
        boolean result = false;
        if (prefs != null) {
            String key = cardKey + CARD_MORE_BUTTON_STATUS;
            result = prefs.getBoolean(key, false);
        }
        return result;
    }

    public void setCardMoreButtonExpandStatus(final String cardKey, final boolean expanded) {
        final SharedPreferences prefs = Utilities.getPrefs(mContext);
        if (prefs != null) {
            new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] params) {
                    String key = cardKey + CARD_MORE_BUTTON_STATUS;
                    prefs.edit().putBoolean(key, expanded).apply();
                    return null;
                }
            }.execute();
        }
    }

    public void setResultWithData(boolean checked, String cardName) {
        int result = 0;
        try {
            Uri uri = Uri.parse(Config.CARD_VIEW_SHOWSTATUS_MINUS_URI);
            ContentValues values = new ContentValues();
            values.put(Config.ENABLED_COLUMN, checked);
            String select = Config.CARDNAME_COLUMN + "=?";
            String[] selectArgs = {cardName};
            result = mContext.getContentResolver().update(uri, values, select, selectArgs);
        } catch (Exception e) {
            Log.e(TAG, "setResultWithData error!!" + e);
        }
        Log.i(TAG, "setResultWithData result=" + result + " cardName=" + cardName);
    }

    private void sendDataCollection(QuickLaunchEntity entity) {
        if (entity == null)
            return;
        int type = entity.type;
        switch (type) {
            case QuickLaunchEntity.QUICK_LAUNCH_APP:
                sendDataCollection(AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_CLICK);
                break;
            case QuickLaunchEntity.QUICK_LAUNCH_FUNCTION: {
                int category = 0;
                if ("mop_sc_wechat_money".equals(entity.quickLaunchKey)) {
                    category = AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_WECHATPAY;
                } else if ("mop_sc_wechat_scan".equals(entity.quickLaunchKey)) {
                    category = AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_WECHATSCAN;
                } else if ("mop_sc_alipay_scan".equals(entity.quickLaunchKey)) {
                    category = AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_ALIPAYSCAN;
                } else if ("mop_sc_wechat_moments".equals(entity.quickLaunchKey)) {
                    category = AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_WECHATMOMENT;
                } else if ("mop_sc_wechat_qr_code".equals(entity.quickLaunchKey)) {
                    category = AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_WECHATCARD;
                } else if ("mop_sc_alipay_pay".equals(entity.quickLaunchKey)) {
                    category = AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_ALIPAYPAY;
                }
                if (category != 0) {
                    sendDataCollection(category);
                }
                break;
            }
            default:
                break;
        }
    }

    public void sendDataCollection(int dataCategory) {
        if (mContext.getPackageManager().hasSystemFeature("com.evenwell.DataCollect")) {
            DataConfig.setSendStrategy(DataConst.SendStrategy.PERIOD, 24);
            int iContext = AppInfoConst.APPID;
            int iEvent = dataCategory;
            String szValue = "1";
            switch (dataCategory) {
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_ON:
                    szValue = "1";
                    break;
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_OFF:
                    szValue = "0";
                    break;
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_DISPLAY:
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_CLICK:
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_WECHATSCAN:
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_WECHATPAY:
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_WECHATMOMENT:
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_WECHATCARD:
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_ALIPAYPAY:
                case AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_ALIPAYSCAN:
                    szValue = "1";
                    break;
                default:
                    break;
            }

            DataCollect.sendEvent(mContext, iContext, iEvent, szValue);
        }
    }
}
