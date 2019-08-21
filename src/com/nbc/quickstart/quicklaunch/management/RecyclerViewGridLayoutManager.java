package com.nbc.quickstart.quicklaunch.management;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * 防止RecyclerView出现java.lang.IndexOutOfBoundsException
 */
public class RecyclerViewGridLayoutManager extends GridLayoutManager {

    public RecyclerViewGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RecyclerViewGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public RecyclerViewGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            //try catch一下
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
}
