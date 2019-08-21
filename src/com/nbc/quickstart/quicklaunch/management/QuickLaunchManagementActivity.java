package com.nbc.quickstart.quicklaunch.management;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.nbc.quickstart.R;
import com.nbc.quickstart.cardmanagement.ItemDragHelperCallback;
import com.nbc.quickstart.core.Config;
import com.nbc.quickstart.utils.AppInfoConst;
import com.nbc.quickstart.utils.Utilities;
import com.nbc.quickstart.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class QuickLaunchManagementActivity extends Activity implements View.OnClickListener {

    private static final String TAG = Config.TAG + QuickLaunchManagementActivity.class.getSimpleName();


    private RecyclerView mRecy;
    private RecyclerView mRecyChoose;
    private TextView cardName;
    private QuickLaunchManager mQuickLaunchManager;
    private QuickLaunchChooseAdapter mChooseAdapter;
    private QuickLaunchManagementAdapter mMoreAdapter;
    private Switch mSwitchButton;
    private boolean mPreviousEnableValue;
    public boolean mDataUpdated;
    private RelativeLayout mHeaderActionBar;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.status_bg_color));
        setContentView(R.layout.quick_launche_manage_main_layout);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        mQuickLaunchManager = QuickLaunchManager.getInstance(this);
        mQuickLaunchManager.setQuickLaunchManagementActivity(this);

        initView();

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mHeaderActionBar.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = (int) Utils.getActionBarHeight(QuickLaunchManagementActivity.this);
        mHeaderActionBar.setLayoutParams(params);


        setQuickLaunchCardNameAndSwitch();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void initView() {
        mRecy = findViewById(R.id.recy);
        mRecyChoose = findViewById(R.id.recy_choose);
        mHeaderActionBar = findViewById(R.id.header_action_bar);
        ivBack = findViewById(R.id.ic_header_back);
        cardName = findViewById(R.id.tv);
        mSwitchButton = findViewById(R.id.card_switch_button);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Keep
    public void onMessageEvent(RefreshManageUIMessage event) {
        if (event != null) {
            Log.i(TAG, "onMessageEvent UI changed!!!");
            if (mChooseAdapter != null && event.chooseBool) {
                mChooseAdapter.notifyDataSetChanged();
            }
            if (mMoreAdapter != null && event.moreBool) {
                mMoreAdapter.notifyDataSetChanged();
            }
        }
    }

    private void setQuickLaunchCardNameAndSwitch() {
        boolean isFirstEntry = Utilities.getPrefs(this).getBoolean(Config.FIRST_ENTRY,true);
        if(isFirstEntry){
            if (getIntent() != null) {
                mPreviousEnableValue = getIntent().getBooleanExtra(Config.EXTRA_CARD_VIEW_ENABLED, false);
                //以下一行代码必须执行，否则会造成从launcher设置里面点击快捷启动进来，增加或删除某个程序时，会造成刷新无效.
                mQuickLaunchManager.setResultWithData(mPreviousEnableValue, Config.CARD_KEY_QUICK_LAUNCH);
                Utilities.getPrefs(this).edit().putBoolean(Config.EXTRA_CARD_VIEW_ENABLED, mPreviousEnableValue).apply();
                Utilities.getPrefs(this).edit().putBoolean(Config.FIRST_ENTRY,false).apply();
            }
        }

        if (mSwitchButton != null) {

            mSwitchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (mSwitchButton.isChecked()) {
                        showCloseContentView(true);
                        if (cardName != null) {
                            cardName.setText(getString(R.string.mop_str_on));
                        }
                        mQuickLaunchManager.sendDataCollection(AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_ON);
                        Utilities.getPrefs(QuickLaunchManagementActivity.this).edit().putBoolean(Config.EXTRA_CARD_VIEW_ENABLED, true).apply();
                    } else {
                        showCloseContentView(false);
                        if (cardName != null) {
                            cardName.setText(getString(R.string.mop_str_off));
                        }
                        mQuickLaunchManager.sendDataCollection(AppInfoConst.FUNC_MINUSONEPAGE_QUICKSTART_OFF);
                        Utilities.getPrefs(QuickLaunchManagementActivity.this).edit().putBoolean(Config.EXTRA_CARD_VIEW_ENABLED, false).apply();
                    }
                    //通知launcher更新负一屏
                    mQuickLaunchManager.setResultWithData(mSwitchButton.isChecked(), Config.CARD_KEY_QUICK_LAUNCH);

                }
            });
        }
    }

    private void init() {
        ivBack.setOnClickListener(this);
        GridLayoutManager chooseManager = new GridLayoutManager(this, mQuickLaunchManager.getQuickLaunchChooseLineCount());
        mRecyChoose.setLayoutManager(chooseManager);
        ItemDragHelperCallback chooseCallback = new ItemDragHelperCallback();
        final ItemTouchHelper chooseHelper = new ItemTouchHelper(chooseCallback);
        chooseHelper.attachToRecyclerView(mRecyChoose);
        mChooseAdapter = new QuickLaunchChooseAdapter(this, chooseHelper, mQuickLaunchManager.getQuickLaunchChooseList());

        chooseManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = mChooseAdapter.getItemViewType(position);
                switch (viewType) {
                    case QuickLaunchChooseAdapter.INVALID_VALUE:
                    case QuickLaunchChooseAdapter.CHOOSE_QUICK_LAUNCH:
                        viewType = 1;
                        break;
                    default:
                        viewType = mQuickLaunchManager.getQuickLaunchChooseLineCount();
                        break;
                }
                return viewType;
            }
        });
        mRecyChoose.setAdapter(mChooseAdapter);


//        GridLayoutManager manager = new GridLayoutManager(this, mQuickLaunchManager.getQuickLaunchChooseLineCount());
        GridLayoutManager manager = new RecyclerViewGridLayoutManager(this, mQuickLaunchManager.getQuickLaunchChooseLineCount());
        mRecy.setLayoutManager(manager);

//        mMoreAdapter = new QuickLaunchManagementAdapter(this, mQuickLaunchManager.getMoreFunctionList(), mQuickLaunchManager.getMoreAppList());
        mMoreAdapter = new QuickLaunchManagementAdapter(this, mQuickLaunchManager.getQuickStartAppList());
        mQuickLaunchManager.removeMoreFunctionAndAppsListObserver();
        mQuickLaunchManager.addMoreFunctionAndAppsListObserver(mMoreAdapter);

        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = mMoreAdapter.getItemViewType(position);
                switch (viewType) {
                    case QuickLaunchManagementAdapter.MORE_FUNCTIONS:
                    case QuickLaunchManagementAdapter.MORE_APPS:
                        viewType = 1;
                        break;
                    default:
                        viewType = mQuickLaunchManager.getQuickLaunchChooseLineCount();
                        break;
                }
                return viewType;
            }
        });
        mRecy.setAdapter(mMoreAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mQuickLaunchManager.getQuickLaunchChooseList().isEmpty()) {
//                    if (mQuickLaunchManager.getWidgetProviderShowStatus()) {
//                        mQuickLaunchManager.updateWidgetProviderStatus(false);
//                    }
//                } else {
//                    if (!mQuickLaunchManager.getWidgetProviderShowStatus()) {
//                        mQuickLaunchManager.updateWidgetProviderStatus(true);
//                    }
//                }
//            }
//        }, 300);

        boolean enable = Utilities.getPrefs(this).getBoolean(Config.EXTRA_CARD_VIEW_ENABLED, false);
        showCloseContentView(enable);
        if (enable) {
            if (cardName != null) {
                cardName.setText(getString(R.string.mop_str_on));
            }
            mSwitchButton.setChecked(true);
        } else {
            if (cardName != null) {
                cardName.setText(getString(R.string.mop_str_off));
            }
            mSwitchButton.setChecked(false);
        }

//        if (mQuickLaunchManager.getLauncherAppsMap().isEmpty()) {
//
//        }

        mQuickLaunchManager.initAppsFromLauncher();
        mQuickLaunchManager.init();
        mQuickLaunchManager.initMoreAppsInBackground();
        init();
    }

    public void updateChange(Object msg) {
        if (msg instanceof QuickLaunchChooseToMoreMessage) {
            QuickLaunchEntity entity = ((QuickLaunchChooseToMoreMessage) msg).toMoreEntity;
            entity.choosenStatus = false;
            mQuickLaunchManager.updateQuickLaunchStatusToDB(entity);
            mMoreAdapter.onMessageEvent((QuickLaunchChooseToMoreMessage) msg);
        } else if (msg instanceof QuickLaunchMoreToChooseMessage) {
            mChooseAdapter.onMessageEvent((QuickLaunchMoreToChooseMessage) msg);
        }
        mDataUpdated = true;
        mQuickLaunchManager.saveQuickLaunchListToDB();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        mQuickLaunchManager.setQuickLaunchManagementActivity(null);
        mQuickLaunchManager.removeMoreFunctionAndAppsListObserver();

        mRecy = null;
        mRecyChoose = null;
        mChooseAdapter = null;
        mMoreAdapter = null;
        mDataUpdated = false;
    }

    private void showCloseContentView(boolean on) {
        TextView emptyText = (TextView) findViewById(R.id.quick_launch_management_empty_text);
        LinearLayout layout = findViewById(R.id.quick_launch_management_choose_layout);
        if (mRecy != null && mRecyChoose != null) {
            if (on) {
                if (layout != null) {
                    layout.setElevation(getResources().getDimension(R.dimen.quick_launch_manage_choose_elevation));
                }
                mRecyChoose.setVisibility(View.VISIBLE);
                mRecy.setVisibility(View.VISIBLE);
                if (emptyText != null) {
                    emptyText.setVisibility(View.GONE);
                }
            } else {
                if (layout != null) {
                    layout.setElevation(0);
                }
                mRecyChoose.setVisibility(View.GONE);
                mRecy.setVisibility(View.GONE);
                if (emptyText != null) {
                    emptyText.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ic_header_back:
                finish();
                break;
        }
    }
    //按home键返回后，会回调此方法
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finish();
    }
}
