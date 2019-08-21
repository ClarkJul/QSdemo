package com.nbc.quickstart.quicklaunch.management;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nbc.quickstart.R;
import com.nbc.quickstart.cardmanagement.OnDragVHListener;
import com.nbc.quickstart.cardmanagement.OnItemMoveListener;
import com.nbc.quickstart.widget.WidgetHelper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class QuickLaunchChooseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnItemMoveListener {

    public static final int INVALID_VALUE = -1;
    public static final int TYPE_CHOOSE_QUICK_LAUNCH_HEADER = 0;
    public static final int CHOOSE_QUICK_LAUNCH = 1;

    private static final int COUNT_PRE_CHOOSE_QUICK_LAUNCH_HEADER = 1;

    // touch 点击开始时间
    protected long startTime;
    protected long startTimeImage;
    // touch 间隔时间  用于分辨是否是 "点击"
    protected static final long SPACE_TIME = 100;

    private LayoutInflater mInflater;
    private ItemTouchHelper mItemTouchHelper;
    private QuickLaunchManagementActivity mActivity;
    private int mChooseListMaxSize;
    private QuickLaunchManager mQuickLaunchManager;

    private List<QuickLaunchEntity> mChooseQuickLaunchItems;


    public QuickLaunchChooseAdapter(Context context, ItemTouchHelper helper, List<QuickLaunchEntity> chooseQuickLaunchItems) {
        mActivity = (QuickLaunchManagementActivity) context;
        mInflater = LayoutInflater.from(context);
        mItemTouchHelper = helper;
        mChooseQuickLaunchItems = chooseQuickLaunchItems;
        mQuickLaunchManager = QuickLaunchManager.getInstance(context);
        mChooseListMaxSize = mQuickLaunchManager.getQuickLaunchChooseMaxSize();

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Keep
    public void onMessageEvent(QuickLaunchMoreToChooseMessage event) {
        if (event.isQuickLaunchChanged && event.toChooseEntity != null) {
            if (!mChooseQuickLaunchItems.contains(event.toChooseEntity)) {
                mChooseQuickLaunchItems.add(event.toChooseEntity);

                notifyItemChanged(mChooseQuickLaunchItems.size());
            }
        }

    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_CHOOSE_QUICK_LAUNCH_HEADER;
        } else if (position > 0 && position < mChooseQuickLaunchItems.size() + COUNT_PRE_CHOOSE_QUICK_LAUNCH_HEADER) {
            return CHOOSE_QUICK_LAUNCH;
        }
        return INVALID_VALUE;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View view;
        switch (viewType) {
            case INVALID_VALUE:
                view = mInflater.inflate(R.layout.item_quick_launch_management, parent, false);
                final EmptyViewHolder emptyHolder = new EmptyViewHolder(view);
                return emptyHolder;

            case TYPE_CHOOSE_QUICK_LAUNCH_HEADER:
                view = mInflater.inflate(R.layout.item_quick_launch_management_header, parent, false);
                TextView headerTitle = (TextView) view.findViewById(R.id.item_quick_launch_management_header_title);
                if (headerTitle != null) {
                    headerTitle.setText(mActivity.getResources().getString(R.string.mop_str_title_chosen));
                }
                return new ViewHolder(view) {};

            case CHOOSE_QUICK_LAUNCH:
                view = mInflater.inflate(R.layout.item_quick_launch_management, parent, false);
                final ChooseViewHolder chooseViewHolder = new ChooseViewHolder(view);

                chooseViewHolder.imageViewRight.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int action = event.getAction();
                        if (MotionEvent.ACTION_DOWN == action) {
                            startTimeImage = System.currentTimeMillis();
                        } else if (MotionEvent.ACTION_UP == action) {
                            moveChooseToOther(chooseViewHolder);
                        } else if (MotionEvent.ACTION_MOVE == action) {
                            if (System.currentTimeMillis() - startTimeImage > SPACE_TIME) {
                                mItemTouchHelper.startDrag(chooseViewHolder);
                            }
                        }
                        return true;
                    }
                });

                chooseViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(final View v) {
                        mItemTouchHelper.startDrag(chooseViewHolder);
                        return true;
                    }
                });

                chooseViewHolder.itemView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (MotionEventCompat.getActionMasked(event)) {
                            case MotionEvent.ACTION_DOWN:
                                startTime = System.currentTimeMillis();
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (System.currentTimeMillis() - startTime > SPACE_TIME) {
                                    mItemTouchHelper.startDrag(chooseViewHolder);
                                }
                                break;
                            case MotionEvent.ACTION_CANCEL:
                            case MotionEvent.ACTION_UP:
                                startTime = 0;
                                break;
                        }
                        return false;
                    }
                });
                return chooseViewHolder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof ChooseViewHolder) {
            ChooseViewHolder chooseViewHolder = (ChooseViewHolder) holder;
            int startPosition = position - COUNT_PRE_CHOOSE_QUICK_LAUNCH_HEADER;
            if (mChooseQuickLaunchItems.isEmpty() || startPosition >= mChooseQuickLaunchItems.size())
                return;
            QuickLaunchEntity entity = mChooseQuickLaunchItems.get(startPosition);
            chooseViewHolder.textView.setText(entity.getDisplayName());
            entity.choosenStatus = true;
            chooseViewHolder.imageView.setImageDrawable(entity.getDisplayIcon());
            chooseViewHolder.imageViewRight.setImageResource(R.drawable.ic_smartcard_reduce);
        } else if (holder instanceof EmptyViewHolder) {
            EmptyViewHolder emptyViewHolder = (EmptyViewHolder) holder;
            emptyViewHolder.imageView.setImageResource(R.drawable.ic_smartcard_occupied);
        }
    }

    @Override
    public int getItemCount() {
        return mChooseListMaxSize + COUNT_PRE_CHOOSE_QUICK_LAUNCH_HEADER;
    }
    //点击item右上角删除图标的动作.
    private void moveChooseToOther(ChooseViewHolder myHolder) {
        int position = myHolder.getAdapterPosition();

        int startPosition = position - COUNT_PRE_CHOOSE_QUICK_LAUNCH_HEADER;
        if (startPosition > mChooseQuickLaunchItems.size() - 1 || startPosition < 0) {
            return;
        }
        QuickLaunchEntity item = mChooseQuickLaunchItems.get(startPosition);
        mChooseQuickLaunchItems.remove(startPosition);

        notifyDataSetChanged();

        QuickLaunchChooseToMoreMessage msg = new QuickLaunchChooseToMoreMessage();
        msg.isQuickLaunchChanged = true;
        item.choosenStatus = false;
        msg.toMoreEntity = item;
        if (mActivity != null) {
            mActivity.updateChange(msg);
        }
        //更新负一屏的widget
        WidgetHelper.getInstance().notifyAppWidgetViewDataChanged(mActivity);
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        QuickLaunchEntity item = mChooseQuickLaunchItems.get(fromPosition - COUNT_PRE_CHOOSE_QUICK_LAUNCH_HEADER);
        mChooseQuickLaunchItems.remove(fromPosition - COUNT_PRE_CHOOSE_QUICK_LAUNCH_HEADER);
        mChooseQuickLaunchItems.add(toPosition - COUNT_PRE_CHOOSE_QUICK_LAUNCH_HEADER, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    class ChooseViewHolder extends QuickLuanchViewHolder implements OnDragVHListener {

        public ChooseViewHolder(View itemView) {
            super(itemView);
        }

        /**
         * item 被选中时
         */
        @Override
        public void onItemSelected() {
//            textView.setBackgroundResource(R.drawable.bg_channel_p);
        }

        /**
         * item 取消选中时
         */
        @Override
        public void onItemFinish() {
//            textView.setBackgroundResource(R.drawable.bg_channel);
            if (mActivity != null) {
                mActivity.updateChange(null);
            }
        }
    }

    class EmptyViewHolder extends QuickLuanchViewHolder {
        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }

}
