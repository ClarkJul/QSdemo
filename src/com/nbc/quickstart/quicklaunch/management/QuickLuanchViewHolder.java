package com.nbc.quickstart.quicklaunch.management;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.nbc.quickstart.R;

public class QuickLuanchViewHolder extends RecyclerView.ViewHolder {
    public TextView textView;
    public ImageView imageView;
    public ImageView imageViewRight;

    public QuickLuanchViewHolder(View itemView) {
        super(itemView);
        textView = (TextView) itemView.findViewById(R.id.quick_launch_name);
        imageView = (ImageView) itemView.findViewById(R.id.quick_launch_icon);
        imageViewRight = (ImageView) itemView.findViewById(R.id.quick_launch_right_icon);
    }
}
