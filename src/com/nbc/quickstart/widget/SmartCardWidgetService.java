package com.nbc.quickstart.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.nbc.quickstart.R;
import com.nbc.quickstart.core.Config;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchEntity;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchManager;

import java.util.List;

public class SmartCardWidgetService extends RemoteViewsService {
    public static final String TAG = Config.TAG + SmartCardWidgetService.class.getSimpleName();
    public static final int ADD = 1;   //+号

    public SmartCardWidgetService() {
        super();
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetFactory(getApplicationContext(), intent);
    }

    public class WidgetFactory implements RemoteViewsService.RemoteViewsFactory {

        private Context mContext;
        private QuickLaunchManager manager;
        private List<QuickLaunchEntity> mQuickLaunchList;
        private int mWidgetId;

        public WidgetFactory(Context context, Intent intent) {
            mContext = context;
            manager = QuickLaunchManager.getInstance(mContext);
            mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        }

        @Override
        public void onCreate() {
            initListViewData();
        }

        private void initListViewData() {
            Log.i(TAG, "WidgetFactory initListViewData");
            mQuickLaunchList = manager.getQuickLaunchChooseList();
            if (mQuickLaunchList.isEmpty()) {
                manager.initForCardDisplay();
            }
        }

        @Override
        public void onDataSetChanged() {
            Log.i(TAG, "WidgetFactory onDataSetChanged id=" + mWidgetId);
            mQuickLaunchList = manager.getQuickLaunchChooseList();
            Log.d(TAG,"onDataSetChange,mQuickLaunchList:" + mQuickLaunchList.size());
        }

        @Override
        public void onDestroy() {
//            Log.i(TAG, "WidgetFactory onDestroy");
        }

        @Override
        public int getCount() {
//            if (!QuickLaunchManager.getInstance(mContext).getCardMoreButtonExpandStatus(Config.CARD_KEY_QUICK_LAUNCH)) {
//                if (manager.showMoreInQuickLaunchCardView()) {
//                    return manager.getQuickLaunchChooseLineCount();
//                }
//            }
            Log.i(TAG, "WidgetFactory getCount=" + mQuickLaunchList.size());
            if(mQuickLaunchList.size() == 10){
                return mQuickLaunchList.size();
            }else{
                return mQuickLaunchList.size() + ADD;
            }
        }

        @Override
        public RemoteViews getViewAt(int i) {
            Log.d(TAG,"quickstart widget service, entry getViewAt");
            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.card_view_layout_quick_launch_listview_item);//获取item的视图
            Intent fillInIntent = new Intent();
            if (i == mQuickLaunchList.size()) {
//                remoteViews.setImageViewResource(R.id.quick_launch_icon, R.drawable.ic_add);
//                remoteViews.setTextViewText(R.id.quick_launch_name, getString(R.string.edit));
                remoteViews.setImageViewResource(R.id.quick_launch_icon, R.drawable.ic_smartcard_occupied);
                remoteViews.setTextViewText(R.id.quick_launch_name, "");
                fillInIntent.putExtra(QuickLaunchEntity.EXTRAS_KEY_ACTION, "quick_start");//添加按钮的点击效果
            } else if (mQuickLaunchList.size() > 0 && i < mQuickLaunchList.size()) {
                QuickLaunchEntity entity = mQuickLaunchList.get(i);
                remoteViews.setImageViewBitmap(R.id.quick_launch_icon, drawableToBitmap(entity.getDisplayIcon()));
                remoteViews.setTextViewText(R.id.quick_launch_name, entity.getDisplayName());

                fillInIntent.putExtra(QuickLaunchEntity.EXTRAS_KEY_QUICKLAUNCHKEY, entity.quickLaunchKey);
                fillInIntent.putExtra(QuickLaunchEntity.EXTRAS_KEY_PACKAGE, entity.packageName);
                fillInIntent.putExtra(QuickLaunchEntity.EXTRAS_KEY_ACTIVITY, entity.startActivity);
                fillInIntent.putExtra(QuickLaunchEntity.EXTRAS_KEY_TYPE, entity.type);
                fillInIntent.putExtra(QuickLaunchEntity.EXTRAS_KEY_ACTION, entity.action);
                fillInIntent.putExtra(QuickLaunchEntity.EXTRAS_KEY_USERID, entity.userId);

            }
            remoteViews.setOnClickFillInIntent(R.id.quick_launch_item_id, fillInIntent);
            return remoteViews;
        }

        public Bitmap drawableToBitmap(Drawable drawable) {
            if (drawable == null) {
                return null;
            }
            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            Bitmap.Config config =
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                            : Bitmap.Config.RGB_565;
            Bitmap bitmap = Bitmap.createBitmap(w, h, config);
            //注意，下面三行代码要用到，否则在View或者SurfaceView里的canvas.drawBitmap会看不到图
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, w, h);
            drawable.draw(canvas);

            return bitmap;
        }

        @Override
        public RemoteViews getLoadingView() {
//            Log.i(TAG, "WidgetFactory getLoadingView");
            return null;
        }

        @Override
        public int getViewTypeCount() {
//            Log.i(TAG, "WidgetFactory getViewTypeCount");
            return 1;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
//            Log.i(TAG, "WidgetFactory hasStableIds");
            return true;
        }
    }

}
