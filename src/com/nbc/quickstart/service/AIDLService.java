package com.nbc.quickstart.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.nbc.quickstart.IQuickStartServiceManager;
import com.nbc.quickstart.core.Config;
import com.nbc.quickstart.utils.Utilities;

public class AIDLService extends Service {
    private static final String TAG = "AIDLService";

    IQuickStartServiceManager.Stub stub = new IQuickStartServiceManager.Stub() {
        @Override
        public void setEnable(boolean enable) throws RemoteException {
            Log.d(TAG,"call remote success");
            Utilities.getPrefs(AIDLService.this).edit().putBoolean(Config.EXTRA_CARD_VIEW_ENABLED,enable).apply();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }
}
