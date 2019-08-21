package com.nbc.quickstart.widget;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Keep;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.nbc.quickstart.R;
import com.nbc.quickstart.core.Config;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchEntity;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchManagementActivity;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchManager;
import com.nbc.quickstart.quicklaunch.management.RefreshMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class WidgetHelper {
    private static final String TAG = "WidgetHelper";
    private static WidgetHelper helper;

    public synchronized static WidgetHelper getInstance() {
        if (helper == null) {
            helper = new WidgetHelper();
            if (!EventBus.getDefault().isRegistered(helper)) {
                EventBus.getDefault().register(helper);
            }
        }
        return helper;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Keep
    public void onMessageEvent(RefreshMessage event) {
        if (event != null) {
            notifyAppWidgetViewDataChanged(event.mContext);
        }
    }

    public void notifyAppWidgetViewDataChanged(Context context) {
        Log.i(TAG, "messageEvent notifyAppWidgetViewDataChanged!!!");
        if (context == null) {
            Log.i(TAG, "notifyAppWidgetViewDataChanged  context = null");
            return;
        }
        int[] mAppWidgetIds = null;
        if (mAppWidgetIds == null) {
            ComponentName componentName = new ComponentName(context.getPackageName(), SmartCardWidgetProvider.class.getCanonicalName());
            mAppWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(componentName);
            Log.i(TAG, "notifyAppWidgetViewDataChanged  mAppWidgetIds is null");
        }
        for (int widgetId : mAppWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.card_view_layout_quick_launch_listview);
//            updateMoreButtonStatus(false, widgetId, context, remoteViews, false, false);
            updateWidget(context, widgetId, remoteViews);
        }
        List<QuickLaunchEntity> mQuickLaunchList = QuickLaunchManager.getInstance(context).getQuickLaunchChooseList();
//        if(mQuickLaunchList != null && mQuickLaunchList.size() !=0){
        if (mQuickLaunchList != null) {
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(mAppWidgetIds, R.id.recy_card_view_quick_launch_list);
        }
//        }
    }

    public static void updateWidget(Context context, int widgetId, RemoteViews remoteViews) {
        Log.i(TAG, "updateWidget");

        remoteViews.setTextViewText(R.id.card_view_header_title, context.getString(R.string.mop_str_title_quick_start));

        Intent mIntService = new Intent(context, SmartCardWidgetService.class);
        mIntService.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        remoteViews.setRemoteAdapter(R.id.recy_card_view_quick_launch_list, mIntService);
        remoteViews.setEmptyView(R.id.recy_card_view_quick_launch_list, R.id.quick_launch_empty_text_list);

        //item的点击事件
        Intent intent = new Intent(context, SmartCardActionActivity.class);

        PendingIntent clickIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.recy_card_view_quick_launch_list, clickIntent);

        //icon">"的点击事件
        Intent intent_more_button = new Intent(context, SmartCardWidgetProvider.class);
        intent_more_button.setAction("quick_launch_more_button_expend_action");
        PendingIntent clickIntent_more = PendingIntent.getBroadcast(context, 0, intent_more_button, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.card_view_header_more_button, clickIntent_more);


        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }

    public static void updateMoreButtonStatus(boolean update, int widgetId, Context context, RemoteViews remoteViews, boolean expand, boolean flash) {
        Log.d(TAG,"entry updateMoreButtonStatus");
        if (remoteViews == null) {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.card_view_layout_quick_launch_listview);
        }
        Intent intent_more_button = new Intent(context, SmartCardWidgetProvider.class);
        intent_more_button.setAction("quick_launch_more_button_expend_action");
        if (update) {
            if (expand) {
                remoteViews.setImageViewResource(R.id.card_view_header_more_button, R.drawable.ic_smartcard_retract);
                Log.i(Config.TAG, "updateMoreButtonStatus 0 show expand button=true");
                intent_more_button.putExtra("quick_launch_more_button_expend", false);
                QuickLaunchManager.getInstance(context).setCardMoreButtonExpandStatus(Config.CARD_KEY_QUICK_LAUNCH, true);
            } else {
                remoteViews.setImageViewResource(R.id.card_view_header_more_button, R.drawable.ic_smartcard_expand);
                Log.i(Config.TAG, "updateMoreButtonStatus 0 show expand button=false");
                intent_more_button.putExtra("quick_launch_more_button_expend", true);
                QuickLaunchManager.getInstance(context).setCardMoreButtonExpandStatus(Config.CARD_KEY_QUICK_LAUNCH, false);
            }
            PendingIntent clickIntent_more = PendingIntent.getBroadcast(context, 0, intent_more_button, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.card_view_header_more_button, clickIntent_more);
        } else {
            if (QuickLaunchManager.getInstance(context).showMoreInQuickLaunchCardView()) {
                remoteViews.setViewVisibility(R.id.card_view_header_more_button, View.VISIBLE);

                if (QuickLaunchManager.getInstance(context).getCardMoreButtonExpandStatus(Config.CARD_KEY_QUICK_LAUNCH)) {
                    Log.i(Config.TAG, "updateMoreButtonStatus show expand button=true");
                    remoteViews.setImageViewResource(R.id.card_view_header_more_button, R.drawable.ic_smartcard_retract);
                    intent_more_button.putExtra("quick_launch_more_button_expend", false);
                } else {
                    Log.i(Config.TAG, "updateMoreButtonStatus show expand button=false");
                    remoteViews.setImageViewResource(R.id.card_view_header_more_button, R.drawable.ic_smartcard_expand);
                    intent_more_button.putExtra("quick_launch_more_button_expend", true);
                }

                PendingIntent clickIntent_more = PendingIntent.getBroadcast(context, 0, intent_more_button, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.card_view_header_more_button, clickIntent_more);
            } else {
                remoteViews.setViewVisibility(R.id.card_view_header_more_button, View.GONE);
            }
        }

        if (QuickLaunchManager.getInstance(context).getQuickLaunchChooseList().size() == 0) {
            Log.i(Config.TAG, "updateMoreButtonStatus empty_text show widgetId:" + widgetId);
            remoteViews.setTextViewText(R.id.quick_launch_empty_text_list, context.getString(R.string.mop_str_text_no_qsa));
            Intent intent = new Intent(context, QuickLaunchManagementActivity.class);
            intent.putExtra(Config.EXTRA_CARD_VIEW_ENABLED, true);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.quick_launch_empty_text_list, pendingIntent);
        } else {
            Log.i(Config.TAG, "updateMoreButtonStatus empty_text gone widgetId:" + widgetId);
        }

        if (flash) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
