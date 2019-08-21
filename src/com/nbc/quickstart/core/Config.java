package com.nbc.quickstart.core;


import com.nbc.quickstart.widget.SmartCardWidgetProvider;

public class Config {
    public static final String TAG = "QuickStart:";

    public static final String EXTRA_CARD_VIEW_ENABLED= "card_view_enabled";
    public static final String EXTRA_FROM_LAUNCHER = "from_launcher";
    public static final String FIRST_ENTRY = "first_entry";

    public static final String CARD_KEY_QUICK_LAUNCH = "quick_launch";
    public static final String CARD_KEY_QUICK_LAUNCH_EDGE = "quick_launch_edge";

    public static final String MINUS_BASE_URI = "content://android.launcher.smartcard";
    public static final String EDGE_PANEL_BASE_URI = "content://com.nbc.minusonepage.provider";

    public static final String CARD_VIEW_SHOWSTATUS_MINUS_URI = MINUS_BASE_URI + "/cardviews/showstatus";
    public static final String CARD_VIEW_SHOWSTATUS_EDGEPANEL_URI = EDGE_PANEL_BASE_URI + "/cardviews/showstatus";
    public static final String CARDNAME_COLUMN = "cardname";
    public static final String ENABLED_COLUMN = "enabled";

    public static final String QUICK_LAUNCH_MINUS_URI = MINUS_BASE_URI + "/quicklaunch";


    public static final String WIDGET_MINUS_URI = MINUS_BASE_URI + "/widgets";
    public static final String WIDGET_EDGEPANEL_URI = EDGE_PANEL_BASE_URI + "/widgets";
    public static final String WIDGET_SHOWSTATUS_MINUS_URI = MINUS_BASE_URI + "/widgets/showstatus";
    public static final String WIDGET_SHOWSTATUS_EDGEPANEL_URI = EDGE_PANEL_BASE_URI + "/widgets/showstatus";
    public static final String QUICKLAUNCHKEY_COLUMN = "quicklaunchkey";
    public static final String PROVIDERNAME_COLUMN = "providername";
    public static final String SHOW_COLUMN = "show";
    public static final String WIDGET_PROVIDER_NAME = SmartCardWidgetProvider.class.getCanonicalName();

    public static final String WECHAT_6_6_MOMENT_START_ACTIVITY = "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI";

}
