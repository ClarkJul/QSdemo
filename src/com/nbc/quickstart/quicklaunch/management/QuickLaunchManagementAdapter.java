package com.nbc.quickstart.quicklaunch.management;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.nbc.quickstart.R;
import com.nbc.quickstart.cardmanagement.OnItemMoveListener;
import com.nbc.quickstart.utils.CustomSortUtil;
import com.nbc.quickstart.widget.WidgetHelper;
import java.util.List;


public class QuickLaunchManagementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnItemMoveListener {

    private static final String TAG = "QuickLaunchManagementAd";
    public static final int MORE_FUNCTIONS = 3;
    public static final int TYPE_MORE_APPS_HEADER = 4;
    public static final int MORE_APPS = 5;
    private static final int COUNT_PRE_MORE_FUNCTIONS_HEADER = 1;

    private LayoutInflater mInflater;
    private QuickLaunchManagementActivity mActivity;
    private QuickLaunchManager mQuickLaunchManager;

    private List<QuickLaunchEntity> mDatas;

//    public QuickLaunchManagementAdapter(Context context, List<QuickLaunchEntity> moreFunctionsItems, List<QuickLaunchEntity> moreAppItems) {
//        mActivity = (QuickLaunchManagementActivity) context;
//        mInflater = LayoutInflater.from(context);
//        mMoreFunctionsItems = moreFunctionsItems;
//        mMoreAppsItems = moreAppItems;
//        mQuickLaunchManager = QuickLaunchManager.getInstance(context);
//    }

    public QuickLaunchManagementAdapter(Context context, List<QuickLaunchEntity> mDatas) {
        mActivity = (QuickLaunchManagementActivity) context;
        mInflater = LayoutInflater.from(context);
        this.mDatas = mDatas;
        mQuickLaunchManager = QuickLaunchManager.getInstance(context);
    }

    public void onMessageEvent(QuickLaunchChooseToMoreMessage event) {
        if (event.isQuickLaunchChanged && event.toMoreEntity != null) {
            if (!mDatas.contains(event.toMoreEntity)) {
                mDatas.add(event.toMoreEntity);
                CustomSortUtil.sort(mDatas);
                notifyDataSetChanged();
            }
        }
    }

    public void setmoreAppItems(List<QuickLaunchEntity> moreAppItems) {
        Log.d(TAG,"setMoreAppItem :" + moreAppItems.size());
        mDatas = moreAppItems;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_MORE_APPS_HEADER;
        } else {
            return MORE_APPS;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View view;
        switch (viewType) {
            case TYPE_MORE_APPS_HEADER:
                view = mInflater.inflate(R.layout.item_quick_launch_management_more_app_header, parent, false);
                TextView more_apps_header = view.findViewById(R.id.item_quick_launch_management_header_title);
                if (more_apps_header != null) {
                    more_apps_header.setText(mActivity.getResources().getString(R.string.mop_str_list_quick_start));
                }
                View divider = view.findViewById(R.id.more_function_app_divider);
                if (divider != null) {
                    divider.setVisibility(View.GONE);
                }
                return new RecyclerView.ViewHolder(view) {
                };
            case MORE_APPS:
                view = mInflater.inflate(R.layout.item_quick_launch_management, parent, false);
                final MoreAppViewHolder holder = new MoreAppViewHolder(view);
                view.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (MotionEvent.ACTION_UP == event.getAction()) {
//                            if (mQuickLaunchManager.canClick(v, event)) {
                                moveMoreAppToChoose(holder);
//                            }
                        }
                        return true;
                    }
                });
                return holder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MoreAppViewHolder) {
            MoreAppViewHolder appHodler = (MoreAppViewHolder) holder;
            int startPosition = position - COUNT_PRE_MORE_FUNCTIONS_HEADER;
            if (mDatas.isEmpty() || startPosition < 0)
                return;
            if(startPosition < mDatas.size()){
                QuickLaunchEntity entity = mDatas.get(startPosition);
                appHodler.textView.setText(entity.getDisplayName());
                entity.choosenStatus = false;
                appHodler.imageView.setImageDrawable(entity.getDisplayIcon());
            }
        }
    }

    @Override
    public int getItemCount() {
        Log.d(TAG,"getCount:" + mDatas.size());
        return mDatas.size() + 1;
    }

    private void moveMoreAppToChoose(MoreAppViewHolder moreAppHolder) {
        int position = processItemRemoveAdd(moreAppHolder);
        if (position == -1) {
            return;
        }
        notifyItemRemoved(position);

        //更新负一屏的widget
        WidgetHelper.getInstance().notifyAppWidgetViewDataChanged(mActivity);
    }

    private int processItemRemoveAdd(RecyclerView.ViewHolder otherHolder) {
        if (!mQuickLaunchManager.isQuickLaunchListFull()) {
            Toast.makeText(mActivity, R.string.mop_str_full_notification, Toast.LENGTH_SHORT).show();
            return -1;
        }

        int position = otherHolder.getAdapterPosition();
        QuickLaunchEntity item = null;
        if (otherHolder instanceof MoreAppViewHolder) {
            int startPosition = position - COUNT_PRE_MORE_FUNCTIONS_HEADER;
            if (startPosition > mDatas.size() - 1 || startPosition < 0) {
                return -1;
            }
            item = mDatas.get(startPosition);
            mDatas.remove(startPosition);
        }
        if (item != null) {
            postEventBusMessage(item);
        }
        return position;
    }

    private void postEventBusMessage(QuickLaunchEntity entity) {
        QuickLaunchMoreToChooseMessage msg = new QuickLaunchMoreToChooseMessage();
        msg.isQuickLaunchChanged = true;
        entity.choosenStatus = true;
        if (entity.type == QuickLaunchEntity.QUICK_LAUNCH_APP) {
            entity.id = 0;
        }
        msg.toChooseEntity = entity;
        mActivity.updateChange(msg);
    }


    @Override
    public void onItemMove(int fromPosition, int toPosition) {
//        notifyItemMoved(fromPosition, toPosition);
    }

    class MoreFunctionViewHolder extends QuickLuanchViewHolder {

        public MoreFunctionViewHolder(View itemView) {
            super(itemView);
        }
    }

    class MoreAppViewHolder extends QuickLuanchViewHolder {

        public MoreAppViewHolder(View itemView) {
            super(itemView);
        }
    }

}
