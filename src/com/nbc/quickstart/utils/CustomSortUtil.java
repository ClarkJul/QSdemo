package com.nbc.quickstart.utils;

import android.text.TextUtils;
import android.util.Log;

import com.nbc.quickstart.core.Config;
import com.nbc.quickstart.quicklaunch.management.QuickLaunchEntity;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CustomSortUtil{

    private static final String TAG = Config.TAG + CustomSortUtil.class.getSimpleName();

    private static class CustomSortComparator implements Comparator<QuickLaunchEntity>, Serializable {

        @Override
        public int compare(QuickLaunchEntity entity1, QuickLaunchEntity entity2) {
            if(!TextUtils.isEmpty(entity1.displayName) && !TextUtils.isEmpty(entity2.displayName)){
                return Collator.getInstance().compare(entity1.displayName, entity2.displayName);
            }else{
                return -1;
            }
        }
    }

    public static List<QuickLaunchEntity> sort(List<QuickLaunchEntity> list) {
        if (list.isEmpty()) {
            Log.e(TAG, "The passed list is null !!! ");
            return null;
        }

        Locale locale = Locale.getDefault();
        Log.i(TAG, "The default locale is " + locale);
        CustomSortComparator comparator = new CustomSortComparator();

        Collections.sort(list, comparator);

        List<QuickLaunchEntity> temp = new ArrayList<>();
        if (locale.equals(Locale.CHINA) || locale.equals(Locale.TAIWAN)||locale.equals(Locale.US)) {
            for (QuickLaunchEntity entity : list) {
                char char0 = entity.displayName.charAt(0);
                if ((char0 >= 'a' && char0 <= 'z') || (char0 >= 'A' && char0 <= 'Z') || (char0 >= '0' && char0 <= '9')) {
                    temp.add(entity);
                } else {
                    break;
                }
            }
            list.removeAll(temp);
            list.addAll(temp);
        }

        return list;
    }
}
