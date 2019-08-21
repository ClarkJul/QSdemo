package com.nbc.quickstart.widgetedge;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Keep;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.nbc.quickstart.R;
import com.nbc.quickstart.core.Config;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchEntity;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchManager;
import com.nbc.quickstart.quicklaunch.management.RefreshMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class SmartCardWidgetProviderEdge extends AppWidgetProvider {
    public static final String TAG = Config.TAG + SmartCardWidgetProviderEdge.class.getSimpleName();

    int[] mAppWidgetIds;
    private static Context mContext;
    private static SmartCardWidgetProviderEdge mInstance = new SmartCardWidgetProviderEdge();
    private static HandlerThread mLocalThread;
    private static Handler mHandler;

    public SmartCardWidgetProviderEdge() {
        super();
        Log.i(TAG, "SmartCardWidgetProviderEdge()");
    }

    @Override
    public void onEnabled(Context context) {
        Log.i(TAG, "onEnabled");
        mContext = context.getApplicationContext();
        if (mLocalThread == null) {
            mLocalThread = new HandlerThread("SmartCardWidgetProviderEdge");
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
        if (!EventBus.getDefault().isRegistered(mInstance)) {
            EventBus.getDefault().register(mInstance);
        }
        super.onEnabled(context);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Keep
    public void onMessageEvent(RefreshMessage event) {
        if (event != null) {
            notifyAppWidgetViewDataChanged();
        }
    }

    private void notifyAppWidgetViewDataChanged() {
        Log.i(TAG, "messageEvent notifyAppWidgetViewDataChanged!!!");
        if (mContext == null) {
            Log.i(TAG, "notifyAppWidgetViewDataChanged  context = null");
            return;
        }
        ComponentName componentName = new ComponentName(mContext.getPackageName(), SmartCardWidgetProviderEdge.class.getCanonicalName());
        mAppWidgetIds = AppWidgetManager.getInstance(mContext).getAppWidgetIds(componentName);
        if (mAppWidgetIds == null) {
            Log.i(TAG, "notifyAppWidgetViewDataChanged  mAppWidgetIds = null");
            return;
        }
        for (int widgetId : mAppWidgetIds) {
//            updateMoreButtonStatus(false, widgetId, mContext, null, false, true);
            AppWidgetManager.getInstance(mContext).notifyAppWidgetViewDataChanged(widgetId, R.id.recy_card_view_quick_launch_list);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context.getApplicationContext();
        Log.i(TAG, "onReceive  action=" + intent.getAction());
        super.onReceive(context, intent);

        if (!EventBus.getDefault().isRegistered(mInstance)) {
            EventBus.getDefault().register(mInstance);
        }

//        boolean click_event = intent.getBooleanExtra("quick_launch_click", false);
        boolean more_button_expend = intent.getBooleanExtra("quick_launch_more_button_expend", false);

//        Log.i(TAG, "onReceive click_event=" + click_event);
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
            ComponentName componentName = new ComponentName(context.getPackageName(), SmartCardWidgetProviderEdge.class.getCanonicalName());
            mAppWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(componentName);
            for (int widgetId : mAppWidgetIds) {
//                updateMoreButtonStatus(true, widgetId, context, null, more_button_expend, true);
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetId, R.id.recy_card_view_quick_launch_list);
            }
            return;
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        mAppWidgetIds = appWidgetIds;
        Log.i(TAG, "onUpdate");
        for (int widgetId : appWidgetIds) {
            Log.i(TAG, "onUpdate widgetId=" + widgetId);
            updateWidget(context, widgetId);
        }

    }

    private void updateWidget(Context context, int widgetId) {
        Log.i(TAG, "updateWidget");

        RemoteViews mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.card_view_layout_quick_launch_listview_edge);
        mRemoteViews.setTextViewText(R.id.card_view_header_title, context.getString(R.string.mop_str_title_quick_start));
        mRemoteViews.setViewVisibility(R.id.quick_launch_empty_text_list, View.GONE);
        mRemoteViews.setViewVisibility(R.id.card_view_header_more_button, View.GONE);
//        updateMoreButtonStatus(false, widgetId, context, mRemoteViews, false, false);

        Intent mIntService = new Intent(context, SmartCardWidgetServiceEdge.class);
        mIntService.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        mRemoteViews.setRemoteAdapter(R.id.recy_card_view_quick_launch_list, mIntService);

        Intent intent = new Intent(context, SmartCardWidgetProviderEdge.class);
        intent.setAction("quick_launch_click_action");

        PendingIntent clickIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setPendingIntentTemplate(R.id.recy_card_view_quick_launch_list, clickIntent);


        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(widgetId, mRemoteViews);
    }

    private void updateMoreButtonStatus(boolean update, int widgetId, Context context, RemoteViews remoteViews, boolean expand, boolean flash) {
        if (remoteViews == null) {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.card_view_layout_quick_launch_listview);
        }
        Intent intent_more_button = new Intent(context, SmartCardWidgetProviderEdge.class);
        intent_more_button.setAction("quick_launch_more_button_expend_action");
        if (update) {
            if (expand) {
                remoteViews.setImageViewResource(R.id.card_view_header_more_button, R.drawable.ic_smartcard_retract);
                intent_more_button.putExtra("quick_launch_more_button_expend", false);
                QuickLaunchManager.getInstance(context).setCardMoreButtonExpandStatus(Config.CARD_KEY_QUICK_LAUNCH, true);
            } else {
                remoteViews.setImageViewResource(R.id.card_view_header_more_button, R.drawable.ic_smartcard_expand);
                intent_more_button.putExtra("quick_launch_more_button_expend", true);
                QuickLaunchManager.getInstance(context).setCardMoreButtonExpandStatus(Config.CARD_KEY_QUICK_LAUNCH, false);
            }
            PendingIntent clickIntent_more = PendingIntent.getBroadcast(context, 0, intent_more_button, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.card_view_header_more_button, clickIntent_more);
        } else {
            if (QuickLaunchManager.getInstance(context).showMoreInQuickLaunchCardView()) {
                remoteViews.setViewVisibility(R.id.card_view_header_more_button, View.VISIBLE);

                if (QuickLaunchManager.getInstance(context).getCardMoreButtonExpandStatus(Config.CARD_KEY_QUICK_LAUNCH)) {
                    Log.i(TAG, "updateMoreButtonStatus show expand button=true");
                    remoteViews.setImageViewResource(R.id.card_view_header_more_button, R.drawable.ic_smartcard_retract);
                    intent_more_button.putExtra("quick_launch_more_button_expend", false);
                } else {
                    Log.i(TAG, "updateMoreButtonStatus show expand button=false");
                    remoteViews.setImageViewResource(R.id.card_view_header_more_button, R.drawable.ic_smartcard_expand);
                    intent_more_button.putExtra("quick_launch_more_button_expend", true);
                }

                PendingIntent clickIntent_more = PendingIntent.getBroadcast(context, 0, intent_more_button, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.card_view_header_more_button, clickIntent_more);
            } else {
                remoteViews.setViewVisibility(R.id.card_view_header_more_button, View.GONE);
            }
        }
        if (flash) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Log.i(TAG, "onAppWidgetOptionsChanged");
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

//        QuickLaunchManager.getInstance(mContext).initForCardDisplay();

        updateWidget(context, appWidgetId);

        notifyAppWidgetViewDataChanged();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (EventBus.getDefault().isRegistered(mInstance)) {
            EventBus.getDefault().unregister(mInstance);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        if (EventBus.getDefault().isRegistered(mInstance)) {
            EventBus.getDefault().unregister(mInstance);
        }
    }

}
