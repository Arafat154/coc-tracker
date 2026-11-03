package org.clashledger.app;

import android.content.Context;
public final class DisplaySettings {
    public static boolean oled(Context c) { return c.getSharedPreferences("display",0).getBoolean("oled",false); }
    public static boolean compactWidget(Context c) { return c.getSharedPreferences("display",0).getBoolean("compactWidget",false); }
    private DisplaySettings() { }
}
