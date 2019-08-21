package com.nbc.quickstart.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;

import com.nbc.quickstart.R;
import com.nbc.quickstart.core.Config;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchEntity;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchManagementActivity;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchManager;
import com.nbc.quickstart.utils.AppInfoConst;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


public class SmartCardWidgetProvider extends AppWidgetProvider {
    public static final String TAG = Config.TAG + SmartCardWidgetProvider.class.getSimpleName();

    int[] mAppWidgetIds;
    private Context mContext;
    private HandlerThread mLocalThread;
    private Handler mHandler;

    public SmartCardWidgetProvider() {
        super();
        Log.i(TAG, "SmartCardWidgetProvider()");
    }

    @Override
    public void onEnabled(Context context) {
        Log.i(TAG, "onEnabled");
        mContext = context.getApplicationContext();

        if (mLocalThread == null) {
            mLocalThread = new HandlerThread("SmartCardWidgetProvider");
            mLocalThread.start();
        }
        if (mHandler == null) {
            mHandler = new Handler(mLocalThread.getLooper());
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "onEnabled initForCardDisplay");
                if (QuickLaunchManager.getInstance(mContext).getQuickLaunchChooseList().isEmpty()) {
                    QuickLaunchManager.getInstance(mContext).initForCardDisplay();
                }
            }
        });

        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context.getApplicationContext();
        Log.i(TAG, "onReceive  action=" + intent.getAction());
        super.onReceive(context, intent);

        boolean more_button_expend = intent.getBooleanExtra("quick_launch_more_button_expend", false);

        Log.i(TAG, "onReceive more_button_expend=" + more_button_expend);

        if ("quick_launch_click_action".equals(intent.getAction())) {
            QuickLaunchEntity entity = new QuickLaunchEntity(context);
            entity.packageName = intent.getStringExtra(QuickLaunchEntity.EXTRAS_KEY_PACKAGE);
            entity.startActivity = intent.getStringExtra(QuickLaunchEntity.EXTRAS_KEY_ACTIVITY);
            entity.type = intent.getIntExtra(QuickLaunchEntity.EXTRAS_KEY_TYPE, QuickLaunchEntity.QUICK_LAUNCH_FUNCTION);
            entity.action = intent.getStringExtra(QuickLaunchEntity.EXTRAS_KEY_ACTION);
            entity.userId = intent.getIntExtra(QuickLaunchEntity.EXTRAS_KEY_USERID, 0);

            QuickLaunchManager.getInstance(context).startActivitySafely(entity);
            return;
        }

        if ("quick_launch_more_button_expend_action".equals(intent.getAction())) {
//            if (mAppWidgetIds == null) {
//                ComponentName componentName = new ComponentName(context.getPackageName(), SmartCardWidgetProvider.class.getCanonicalName());
//                mAppWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(componentName);
//            }
//            for (int widgetId : mAppWidgetIds) {
//                WidgetHelper.getInstance().updateMoreButtonStatus(true, widgetId, context, null, more_button_expend, true);
//                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetId, R.id.recy_card_view_quick_launch_list);
//            }
            Log.d(TAG, "点击“>" + "跳转到quickStart");
            Intent quickLauncherActivity = new Intent(context, QuickLaunchManagementActivity.class);
            quickLauncherActivity.setFlags(FLAG_ACTIVITY_NEW_TASK);
//            quickLauncherActivity.putExtra("From", "MINUS");  //表示从负一屏调转到设置页面的.
            quickLauncherActivity.putExtra(Config.EXTRA_CARD_VIEW_ENABLED, true);
            context.startActivity(quickLauncherActivity);
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        mAppWidgetIds = appWidgetIds;
        Log.i(TAG, "onUpdate");
        //切换中英文的时候，会回调该方法，重新初始化下数据.再会回调onAppWidgetOptionsChanged刷新下负一屏widget的界面.
        QuickLaunchManager.getInstance(mContext).initAppsFromLauncher();
        QuickLaunchManager.getInstance(mContext).init();
        for (int widgetId : appWidgetIds) {
            Log.i(TAG, "onUpdate widgetId=" + widgetId);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.card_view_layout_quick_launch_listview);
//            WidgetHelper.getInstance().updateMoreButtonStatus(false, widgetId, context, remoteViews, false, false);
            WidgetHelper.updateWidget(context, widgetId, remoteViews);
        }
        context.startService(new Intent(context, EmptyService.class));
    }

    public static class EmptyService extends Service {
        public EmptyService() {
            super();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }


    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Log.i(TAG, "onAppWidgetOptionsChanged");
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        WidgetHelper.getInstance().notifyAppWidgetViewDataChanged(context);
        QuickLaunchManager.getInstance(mContext).sendDataCollection(AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_DISPLAY);
    }

}
