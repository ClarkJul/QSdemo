package com.nbc.quickstart.widget;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchEntity;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchManager;

//负一屏点击快捷启动某一个icon会触发这里
public class SmartCardActionActivity extends Activity{
    private static final String TAG = "SmartCardActionActivity";
    @Override
    protected void onResume() {
        super.onResume();
        QuickLaunchEntity entity = new QuickLaunchEntity(getApplicationContext());
        if (getIntent() != null) {
            String action = getIntent().getStringExtra(QuickLaunchEntity.EXTRAS_KEY_ACTION);
            if(!TextUtils.isEmpty(action)&& action.equals("quick_start")){
                Log.d(TAG,"点击编辑按钮，跳转到quickstart主页面");
                entity = new QuickLaunchEntity(getApplicationContext());
                entity.packageName = "com.nbc.quickstart";
                entity.startActivity = "com.nbc.quickstart.quicklaunch.management.QuickLaunchManagementActivity";
            }else{
                entity.quickLaunchKey = getIntent().getStringExtra(QuickLaunchEntity.EXTRAS_KEY_QUICKLAUNCHKEY);
                entity.packageName = getIntent().getStringExtra(QuickLaunchEntity.EXTRAS_KEY_PACKAGE);
                entity.startActivity = getIntent().getStringExtra(QuickLaunchEntity.EXTRAS_KEY_ACTIVITY);
                entity.type = getIntent().getIntExtra(QuickLaunchEntity.EXTRAS_KEY_TYPE, QuickLaunchEntity.QUICK_LAUNCH_FUNCTION);
                entity.action = getIntent().getStringExtra(QuickLaunchEntity.EXTRAS_KEY_ACTION);
                entity.userId = getIntent().getIntExtra(QuickLaunchEntity.EXTRAS_KEY_USERID, 0);
            }
            QuickLaunchManager.getInstance(getApplicationContext()).startActivitySafely(entity);
            Log.i("QuickStart", "startActivitySafely");
        }
        finish();
    }
}
